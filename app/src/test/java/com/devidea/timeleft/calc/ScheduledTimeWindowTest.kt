package com.devidea.timeleft.calc

import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class ScheduledTimeWindowTest {
    private fun range(start: String = "23:50", end: String = "0:15", overnight: Boolean = true, weekdays: Int = 127) =
        ItemEntity(1, ItemType.Time, "test", start, end, RecurrenceMode.TimeRange, 0, weekdays = weekdays, endNextDay = overnight)

    @Test fun midnightBelongsToPreviousStartDay() {
        val now = ZonedDateTime.parse("2026-09-22T00:00:00+09:00[Asia/Seoul]") // Tuesday
        val window = scheduledTimeWindow(range(weekdays = 1), now)!! // Monday only
        assertEquals(TimeRangePhase.Active, window.phase)
        assertEquals("2026-09-21", window.start.toLocalDate().toString())
        assertEquals(1500L, window.totalSeconds)
    }

    @Test fun excludedWeekdayWaitsUntilNextSelectedDay() {
        val now = ZonedDateTime.parse("2026-09-25T12:00:00+09:00[Asia/Seoul]")
        val window = scheduledTimeWindow(range(weekdays = 1), now)!!
        assertEquals("2026-09-28", window.start.toLocalDate().toString())
        assertEquals(TimeRangePhase.Upcoming, window.phase)
    }

    @Test fun legacyReversedWindowDoesNotBecomeOvernight() {
        assertNull(scheduledTimeWindow(range(overnight = false), ZonedDateTime.parse("2026-09-25T12:00:00Z")))
    }

    @Test fun endBoundaryIsExclusiveAndMovesToNextStart() {
        val now = ZonedDateTime.parse("2026-09-26T00:15:00+09:00[Asia/Seoul]")
        val window = scheduledTimeWindow(range(), now)!!
        assertEquals(TimeRangePhase.Finished, window.phase)
        assertEquals("2026-09-26", window.start.toLocalDate().toString())
        assertTrue(window.start.isAfter(now))
    }

    @Test fun dstDurationUsesRealInstants() {
        val now = ZonedDateTime.parse("2026-03-08T01:30:00-05:00[America/New_York]")
        assertEquals(3600L, scheduledTimeWindow(range("1:0", "3:0", false), now)!!.totalSeconds)
    }

    @Test fun noWeekdayProducesNoWindow() {
        assertNull(scheduledTimeWindow(range(weekdays = 0), ZonedDateTime.parse("2026-09-25T12:00:00Z")))
    }
}
