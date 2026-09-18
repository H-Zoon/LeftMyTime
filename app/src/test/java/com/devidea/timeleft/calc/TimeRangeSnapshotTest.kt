package com.devidea.timeleft.calc

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class TimeRangeSnapshotTest {
    private val start = LocalTime.of(14, 0)
    private val end = LocalTime.of(15, 0)

    @Test fun `upcoming is not mistaken for active zero percent`() {
        val waiting = timeRangeSnapshot(start, end, LocalTime.of(13, 30))
        val begun = timeRangeSnapshot(start, end, start)
        assertEquals(TimeRangePhase.Upcoming, waiting.phase)
        assertEquals(1800L, waiting.secondsUntilStart)
        assertEquals(TimeRangePhase.Active, begun.phase)
        assertEquals(3600L, begun.secondsLeft)
        assertEquals(waiting.percentElapsed, begun.percentElapsed)
    }

    @Test fun `end is finished and next start is tomorrow`() {
        val result = timeRangeSnapshot(start, end, end)
        assertEquals(TimeRangePhase.Finished, result.phase)
        assertEquals(0L, result.secondsLeft)
        assertEquals(23 * 3600L, result.secondsUntilStart)
    }

    @Test fun `last second remains active and midnight resets next occurrence`() {
        assertEquals(TimeRangePhase.Active, timeRangeSnapshot(start, end, LocalTime.of(14, 59, 59)).phase)
        val result = timeRangeSnapshot(start, end, LocalTime.MIDNIGHT)
        assertEquals(TimeRangePhase.Upcoming, result.phase)
        assertEquals(14 * 3600L, result.secondsUntilStart)
    }
    @Test fun `legacy invalid ranges remain inactive without crashing`() {
        assertEquals(TimeRangePhase.Finished, timeRangeSnapshot(end, start, start).phase)
        assertEquals(TimeRangePhase.Finished, timeRangeSnapshot(start, start, start).phase)
    }
}
