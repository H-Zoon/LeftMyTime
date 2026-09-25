package com.devidea.timeleft.ui.home

import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.TimeDetailFacts
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemType
import org.junit.Assert.*
import org.junit.Test

class HomeSelectionTest {
    private fun active(id: Int, seconds: Long) = AdapterItem(id = id, type = ItemType.Time, timePhase = TimeRangePhase.Active, remainingSeconds = seconds)
    private fun date(id: Int, days: Int, phase: TimeRangePhase = TimeRangePhase.Active, valid: Boolean = true) = AdapterItem(
        id = id, type = ItemType.Date, remainingDays = days, isExpired = days < 0,
        detailFacts = TimeDetailFacts(0f, 0, 30, phase, inDays = true, validRange = valid),
    )

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

    @Test fun `home keeps selected current work before falling back to a date`() {
        val dates = listOf(date(10, 0), date(11, 12))
        val items = dates + listOf(active(1, 300), active(2, 900))
        assertEquals(1, selectHomeHero(items, null)?.id)
        assertEquals(2, selectHomeHero(items, 2)?.id)
        assertEquals(1, selectHomeHero(items.filterNot { it.id == 2 }, 2)?.id)
        assertEquals(10, selectHomeHero(dates, 2)?.id)
    }

    @Test fun `future dates remain visible beside many recurring time ranges`() {
        val waiting = (1..10).map {
            AdapterItem(id = it, type = ItemType.Time, timePhase = TimeRangePhase.Finished, secondsUntilStart = it * 60L)
        }
        val items = waiting + listOf(date(12, 8, TimeRangePhase.Upcoming), date(11, 8), date(13, 20))
        assertEquals(listOf(11, 12, 13), homeDateItems(items).map { it.id })
        assertEquals(11, selectHomeHero(items, null)?.id)
    }

    @Test fun `a valid same day period survives while expired and invalid dates stay out of home`() {
        val today = date(5, 0).let { it.copy(detailFacts = it.detailFacts?.copy(total = 0)) }
        val items = listOf(
            date(1, -3, TimeRangePhase.Finished),
            date(2, 1, valid = false),
            date(3, 0, TimeRangePhase.Finished),
            date(4, 1).copy(isExpired = true),
            date(6, 1).copy(remainingDays = null),
            date(7, 1).copy(detailFacts = null),
            today,
        )
        assertEquals(listOf(5), homeDateItems(items).map { it.id })
        assertEquals(5, selectHomeHero(items, null)?.id)
        assertNull(selectHomeHero(items.filterNot { it.id == 5 }, null))
        assertNull(selectHomeHero(emptyList(), null))
    }
}
