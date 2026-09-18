package com.devidea.timeleft.ui.home

import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemType
import org.junit.Assert.*
import org.junit.Test

class HomeSelectionTest {
    private fun active(id: Int, seconds: Long) = AdapterItem(id = id, type = ItemType.Time, timePhase = TimeRangePhase.Active, remainingSeconds = seconds)

    @Test fun `a date due today and a waiting range cannot displace current work`() {
        val items = listOf(
            AdapterItem(id = 1, type = ItemType.Date, remainingSortKey = 0),
            AdapterItem(id = 2, type = ItemType.Time, timePhase = TimeRangePhase.Upcoming, percent = 0f),
            active(3, 600)
        )
        assertEquals(3, selectActiveTimeItem(items, null)?.id)
    }

    @Test fun `manual selection survives closer overlapping ranges then falls back at completion`() {
        val items = listOf(active(9, 900), active(2, 300), active(1, 300))
        assertEquals(1, selectActiveTimeItem(items, null)?.id)
        assertEquals(9, selectActiveTimeItem(items, 9)?.id)
        val ended = items.map { if (it.id == 9) it.copy(timePhase = TimeRangePhase.Finished) else it }
        assertEquals(1, selectActiveTimeItem(ended, 9)?.id)
        assertNull(selectActiveTimeItem(emptyList(), 9))
    }

    @Test fun `next ranges include tomorrow and exclude the active hero`() {
        val items = listOf(
            active(1, 300),
            AdapterItem(id = 2, type = ItemType.Time, timePhase = TimeRangePhase.Finished, secondsUntilStart = 70_000),
            AdapterItem(id = 3, type = ItemType.Time, timePhase = TimeRangePhase.Upcoming, secondsUntilStart = 600)
        )
        assertEquals(listOf(3, 2), upcomingTimeItems(items).map { it.id })
    }
}
