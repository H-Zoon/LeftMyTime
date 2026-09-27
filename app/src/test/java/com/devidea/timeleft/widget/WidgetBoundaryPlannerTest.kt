package com.devidea.timeleft.widget

import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class WidgetBoundaryPlannerTest {
    private val now = ZonedDateTime.parse("2026-09-26T10:00:00+09:00[Asia/Seoul]")
    private fun time(id: Int, start: String, end: String) = ItemEntity(id, ItemType.Time, "Test", start, end, RecurrenceMode.TimeRange, 0)

    @Test fun `all candidates participate and a delayed refresh advances past old boundaries`() {
        val items = listOf(time(1, "9:00", "12:00"), time(2, "10:05", "10:06"))
        assertEquals(now.plusMinutes(5).toInstant().toEpochMilli(), nextWidgetSelectionBoundary(items, now))
        assertEquals(now.plusMinutes(6).toInstant().toEpochMilli(), nextWidgetSelectionBoundary(items, now.plusMinutes(5)))
        assertEquals(now.plusHours(2).toInstant().toEpochMilli(), nextWidgetSelectionBoundary(items, now.plusMinutes(7)))
    }

    @Test fun `editing and deleting recomputes boundaries without a stale item payload`() {
        val original = time(1, "10:05", "11:00")
        assertEquals(now.plusMinutes(5).toInstant().toEpochMilli(), nextWidgetSelectionBoundary(listOf(original), now))
        assertEquals(now.plusMinutes(20).toInstant().toEpochMilli(), nextWidgetSelectionBoundary(listOf(original.copy(startValue = "10:20")), now))
        assertNull(nextWidgetSelectionBoundary(emptyList(), now))
    }

    @Test fun `focus templates and malformed times do not introduce bogus civil time alarms`() {
        val items = listOf(time(1, "bad", "11:00"), time(2, "10:01", "11:00").copy(isTemplate = true),
            time(3, "10:02", "11:00").copy(focusDurationMillis = 60_000))
        assertNull(nextWidgetSelectionBoundary(items, now))
    }

    @Test fun `date candidate asks for next local midnight across DST`() {
        val dst = ZonedDateTime.parse("2026-03-08T00:00:00-05:00[America/New_York]")
        val date = ItemEntity(1, ItemType.Date, "Date", "2026-03-01", "2026-03-09", RecurrenceMode.None, 0)
        assertEquals(dst.toInstant().plusSeconds(23 * 3600L).toEpochMilli(), nextWidgetSelectionBoundary(listOf(date), dst))
    }
}
