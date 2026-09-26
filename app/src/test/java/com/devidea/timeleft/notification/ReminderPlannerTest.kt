package com.devidea.timeleft.notification

import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class ReminderPlannerTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private fun date(start: String, end: String, mode: RecurrenceMode = RecurrenceMode.None, rate: Int = 0, offset: Int = 0) =
        ItemEntity(1, ItemType.Date, "test", start, end, mode, rate, reminderOffsetDays = offset)
    private fun next(item: ItemEntity, now: String, zone: ZoneId = seoul, time: LocalTime = LocalTime.of(9, 0)) =
        ReminderPlanner.next(item, Instant.parse(now), zone, time)?.triggerMillis?.let(Instant::ofEpochMilli)

    @Test fun recurringDateContinuesWithoutUpdatingStoredWindow() {
        val item = date("2026-09-25", "2026-09-25", RecurrenceMode.Day, 1)
        assertEquals(Instant.parse("2026-09-26T00:00:00Z"), next(item, "2026-09-25T00:00:00Z"))
        assertEquals("2026-09-25", item.endValue)
    }

    @Test fun earlyReminderCanBelongToLaterThanVisibleOccurrence() {
        val item = date("2026-09-25", "2026-09-25", RecurrenceMode.Day, 1, offset = 7)
        val plan = ReminderPlanner.next(item, Instant.parse("2026-09-25T01:00:00Z"), seoul, LocalTime.of(9, 0))!!
        assertEquals("2026-10-03", plan.occurrenceEnd)
        assertEquals(Instant.parse("2026-09-26T00:00:00Z"), Instant.ofEpochMilli(plan.triggerMillis))
    }

    @Test fun monthlyDay31SkipsShortMonthsAndPreservesCycleLength() {
        val item = date("2026-01-31", "2026-02-02", RecurrenceMode.Month, 31)
        val plan = ReminderPlanner.next(item, Instant.parse("2026-02-03T00:00:00Z"), seoul, LocalTime.of(9, 0))!!
        assertEquals("2026-04-02", plan.occurrenceEnd)
    }

    @Test fun passedOneOffDateDoesNotRepeat() {
        assertNull(next(date("2026-09-20", "2026-09-25"), "2026-09-25T00:00:00Z"))
    }

    @Test fun reminderBeforeMidnightTargetsTomorrowEnd() {
        val item = ItemEntity(2, ItemType.Time, "test", "0:0", "0:10", RecurrenceMode.TimeRange, 0, reminderOffsetDays = 30)
        assertEquals(Instant.parse("2026-09-25T14:40:00Z"), next(item, "2026-09-25T13:00:00Z"))
    }

    @Test fun sameWallClockUsesNewTimezone() {
        val item = date("2026-09-25", "2026-09-26")
        assertEquals(Instant.parse("2026-09-26T00:00:00Z"), next(item, "2026-09-25T00:00:00Z"))
        assertEquals(Instant.parse("2026-09-26T09:00:00Z"), next(item, "2026-09-25T00:00:00Z", ZoneId.of("UTC")))
    }

    @Test fun springDstGapUsesFirstValidLocalTime() {
        val item = date("2026-03-08", "2026-03-08")
        assertEquals(Instant.parse("2026-03-08T07:30:00Z"), next(item, "2026-03-07T00:00:00Z", ZoneId.of("America/New_York"), LocalTime.of(2, 30)))
    }

    @Test fun malformedAndDisabledReminderReturnNoAlarm() {
        assertNull(next(date("bad", "2026-09-25"), "2026-09-20T00:00:00Z"))
        assertNull(next(date("2026-09-26", "2026-09-25"), "2026-09-20T00:00:00Z"))
        assertNull(next(date("2026-09-20", "2026-09-25", offset = -1), "2026-09-20T00:00:00Z"))
    }
}
