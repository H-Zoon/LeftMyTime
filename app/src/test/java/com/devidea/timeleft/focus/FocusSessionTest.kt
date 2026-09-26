package com.devidea.timeleft.focus

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class FocusSessionTest {
    private val start = Instant.parse("2026-09-25T14:50:00Z").toEpochMilli()
    private fun session() = FocusSession.create("Focus", 25, start, ZoneId.of("Asia/Seoul"))

    @Test fun oneOffSessionCrossesMidnightWithoutRepeating() {
        val item = session()
        assertEquals("23:50", item.startValue)
        assertEquals("0:15", item.endValue)
        val complete = FocusSession.settle(item, start + 25 * 60_000L)
        assertEquals(FocusSession.COMPLETED, complete.focusState)
        assertEquals(0L, FocusSession.remaining(complete, start + 86_400_000))
        assertEquals(25 * 60_000L, complete.focusElapsedMillis)
    }

    @Test fun pauseAndResumeExcludePausedTime() {
        val paused = FocusSession.pause(session(), start + 5 * 60_000)
        assertEquals(5 * 60_000L, paused.focusElapsedMillis)
        assertEquals(20 * 60_000L, FocusSession.remaining(paused, start + 60 * 60_000))
        val resumed = FocusSession.resume(paused, start + 60 * 60_000)
        assertEquals(start + 80 * 60_000L, resumed.focusEndsAt)
        assertEquals(6 * 60_000L, FocusSession.elapsed(resumed, start + 61 * 60_000))
    }

    @Test fun stoppingPausedSessionDoesNotCountThePauseAsWork() {
        val paused = FocusSession.pause(session(), start + 2 * 60_000)
        val stopped = FocusSession.stop(paused, start + 50 * 60_000)
        assertEquals(FocusSession.ABORTED, stopped.focusState)
        assertEquals(2 * 60_000L, stopped.focusElapsedMillis)
    }

    @Test fun lateReceiverRecordsPlannedCompletionInstantOnlyOnce() {
        val completed = FocusSession.settle(session(), start + 90 * 60_000)
        assertEquals(start + 25 * 60_000L, completed.focusStoppedAt)
        assertEquals(completed, FocusSession.settle(completed, start + 180 * 60_000))
    }

    @Test fun pauseAtEndCompletesInsteadOfMakingZeroLengthPausedSession() {
        assertEquals(FocusSession.COMPLETED, FocusSession.pause(session(), start + 25 * 60_000).focusState)
    }

    @Test fun forwardAndBackwardClockChangesPreserveRunningLength() {
        val initial = FocusSession.create("Focus", 25, start, ZoneId.of("UTC"), FocusClockReading(100_000, 7))
        for (wallShift in listOf(-3_600_000L, 3_600_000L)) {
            val now = start + 60_000 + wallShift
            val adjusted = FocusSession.withClock(initial, now, FocusClockReading(160_000, 7))
            assertEquals(24 * 60_000L, FocusSession.remaining(adjusted, now))
            assertEquals(60_000L, FocusSession.elapsed(adjusted, now))
            assertEquals(FocusSession.RUNNING, FocusSession.settle(adjusted, now).focusState)
        }
    }

    @Test fun rebootUsesSavedDeadlineThenAnchorsToTheNewBoot() {
        val initial = FocusSession.create("Focus", 25, start, ZoneId.of("UTC"), FocusClockReading(100_000, 7))
        val now = start + 5 * 60_000
        val rebooted = FocusSession.withClock(initial, now, FocusClockReading(10_000, 8))
        assertEquals(20 * 60_000L, FocusSession.remaining(rebooted, now))
        val shifted = FocusSession.withClock(rebooted, now + 3_660_000, FocusClockReading(70_000, 8))
        assertEquals(19 * 60_000L, FocusSession.remaining(shifted, now + 3_660_000))
    }

    @Test fun resumeCreatesANewMonotonicSegmentWithoutCountingPause() {
        val paused = FocusSession.pause(session(), start + 60_000)
        val resumedAt = start + 600_000
        val resumed = FocusSession.resume(paused, resumedAt, FocusClockReading(900_000, 4))
        val shifted = FocusSession.withClock(resumed, resumedAt - 3_540_000, FocusClockReading(960_000, 4))
        assertEquals(120_000L, FocusSession.elapsed(shifted, resumedAt - 3_540_000))
        assertEquals(23 * 60_000L, FocusSession.remaining(shifted, resumedAt - 3_540_000))
    }

    @Test fun reopeningAfterAnOverdueRebootKeepsTheOriginalCompletionDay() {
        val initial = FocusSession.create("Focus", 25, start, ZoneId.of("UTC"), FocusClockReading(100_000, 7))
        val twoDaysLater = start + 2 * 86_400_000L
        val recovered = FocusSession.withClock(initial, twoDaysLater, FocusClockReading(10_000, 8))
        val completed = FocusSession.settle(recovered, twoDaysLater)
        assertEquals(start + 25 * 60_000L, completed.focusStoppedAt)
        assertEquals(25 * 60_000L, completed.focusElapsedMillis)
        assertEquals(FocusSession.COMPLETED, completed.focusState)
    }

    @Test fun resetRealtimeDoesNotRestartTheFullSegmentWhileBootCountIsUnchanged() {
        val initial = FocusSession.create("Focus", 25, start, ZoneId.of("UTC"), FocusClockReading(100_000, 7))
        val now = start + 5 * 60_000
        val recovered = FocusSession.withClock(initial, now, FocusClockReading(10_000, 7))
        assertEquals(20 * 60_000L, FocusSession.remaining(recovered, now))
        assertEquals(start + 25 * 60_000L, recovered.focusEndsAt)
    }
}
