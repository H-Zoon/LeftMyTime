package com.devidea.timeleft.focus

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneOffset

class FocusCompletionNoticeTest {
    private val completed = FocusSession.settle(FocusSession.create("Focus", 1, 0, ZoneOffset.UTC), 60_000)
        .copy(id = 7)

    @Test fun onlyTheSameUndeletedCompletionCanBeDelivered() {
        val notice = FocusCompletionNotice(7, 60_000)
        assertTrue(notice.matches(completed))
        assertFalse(notice.matches(null))
        assertFalse(notice.matches(completed.copy(id = 8)))
        assertFalse(notice.matches(completed.copy(deletedAt = 70_000)))
        assertFalse(notice.matches(completed.copy(focusState = FocusSession.ABORTED)))
        assertFalse(notice.matches(completed.copy(focusStoppedAt = 120_000)))
    }

    @Test fun platformFailuresBackOffAndStopGrowingAfterOneHour() {
        val first = FocusCompletionNotice(7, 60_000).attempted(100_000)
        assertEquals(160_000L, first.nextAttemptAt)
        assertEquals(1, first.attempts)
        assertEquals(320_000L, first.attempted(200_000).nextAttemptAt)
        val capped = first.copy(attempts = 30).attempted(300_000)
        assertEquals(3_900_000L, capped.nextAttemptAt)
        assertEquals(30, capped.attempts)
    }
}
