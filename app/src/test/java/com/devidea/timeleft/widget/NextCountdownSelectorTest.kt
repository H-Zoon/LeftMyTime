package com.devidea.timeleft.widget

import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NextCountdownSelectorTest {
    private val today = LocalDate.of(2026, 9, 18)
    private fun time(id: Int, start: String, end: String) = ItemEntity(id, ItemType.Time, "Work", start, end, RecurrenceMode.TimeRange, 0)
    private fun date(id: Int, end: String) = ItemEntity(id, ItemType.Date, "Date", "2026-09-01", end, RecurrenceMode.None, 0)

    @Test fun `malformed or reversed range cannot hide the next valid schedule`() {
        val malformed = date(1, "2026-09-18").copy(startValue = "broken")
        val reversed = date(2, "2026-09-18").copy(startValue = "2026-10-01")
        assertEquals(3, NextCountdownSelector.select(listOf(malformed, reversed, date(3, "2026-09-20")), LocalTime.NOON, today)?.id)
    }

    @Test fun `auto widget uses the same active priority and stable tie break as home`() {
        val items = listOf(date(1, "2026-09-18"), time(9, "14:0", "15:0"), time(2, "14:0", "15:0"))
        assertEquals(2, NextCountdownSelector.select(items, LocalTime.of(14, 18), today)?.id)
    }

    @Test fun `when work finishes a nearby future start ranks before a later date`() {
        val items = listOf(time(1, "14:0", "15:0"), date(2, "2026-09-20"), time(3, "15:5", "16:0"))
        assertEquals(3, NextCountdownSelector.select(items, LocalTime.of(15, 0), today)?.id)
        assertNull(NextCountdownSelector.select(emptyList(), LocalTime.NOON, today))
    }

    @Test fun `expired dates alone leave the automatic widget empty`() {
        assertNull(NextCountdownSelector.select(listOf(date(1, "2026-09-16"), date(2, "2026-09-17")), LocalTime.NOON, today))
    }

    @Test fun `expired date never replaces an upcoming date`() {
        assertEquals(2, NextCountdownSelector.select(listOf(date(1, "2026-09-17"), date(2, "2026-09-20")), LocalTime.NOON, today)?.id)
    }
}
