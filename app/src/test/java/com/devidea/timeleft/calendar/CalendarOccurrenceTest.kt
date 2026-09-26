package com.devidea.timeleft.calendar

import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.calc.scheduledTimeWindow
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import com.devidea.timeleft.notification.ReminderPlanner
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class CalendarOccurrenceTest {
    private fun occurrence(start: String, end: String, allDay: Boolean = false) = CalendarOccurrence(
        "source", "calendar", Instant.parse(start).toEpochMilli(), Instant.parse(end).toEpochMilli(), allDay)

    @Test fun allDayUsesUtcDatesAndExclusiveEndInEveryDeviceZone() {
        val source = occurrence("2026-09-26T00:00:00Z", "2026-09-28T00:00:00Z", true)
        for (zone in listOf("Asia/Seoul", "America/Los_Angeles")) {
            val item = source.toItem(ZoneId.of(zone))
            assertEquals("2026-09-26", item.startValue)
            assertEquals("2026-09-27", item.endValue)
            assertFalse(item.isTimedOccurrence)
            assertEquals(RecurrenceMode.None, item.updateFlag)
        }
    }

    @Test fun timedOccurrenceNeverBecomesTomorrowOrDaily() {
        val item = occurrence("2026-09-26T14:00:00Z", "2026-09-26T16:00:00Z").toItem(ZoneId.of("Asia/Seoul"))
        val window = scheduledTimeWindow(item, ZonedDateTime.parse("2026-09-27T15:00:00Z"))!!
        assertEquals(TimeRangePhase.Finished, window.phase)
        assertEquals(Instant.parse("2026-09-26T16:00:00Z"), window.end.toInstant())
        assertNull(ReminderPlanner.next(item.copy(reminderOffsetDays = 0), Instant.parse("2026-09-27T00:00:00Z"), ZoneOffset.UTC, LocalTime.NOON))
    }

    @Test fun timezoneChangePreservesInstantsAndElapsedDurationAcrossDst() {
        val item = occurrence("2026-03-08T06:00:00Z", "2026-03-08T07:00:00Z").toItem(ZoneId.of("America/New_York"))
        for (zone in listOf("Asia/Seoul", "America/New_York")) {
            val at = Instant.parse("2026-03-08T06:30:00Z").atZone(ZoneId.of(zone))
            val window = scheduledTimeWindow(item, at)!!
            assertEquals(TimeRangePhase.Active, window.phase)
            assertEquals(3600L, window.totalSeconds)
            assertEquals(1800L, Duration.between(at, window.end).seconds)
        }
    }

}
