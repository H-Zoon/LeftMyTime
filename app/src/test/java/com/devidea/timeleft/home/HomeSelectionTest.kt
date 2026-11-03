package com.devidea.timeleft.home

import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.ui.TimeDetailFacts
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.ItemType
import org.junit.Assert.*
import org.junit.Test

class HomeSelectionTest {
    private fun active(id: Int, seconds: Long) = TimeDisplayItem(id = id, type = ItemType.Time, timePhase = TimeRangePhase.Active, remainingSeconds = seconds)
    private fun date(id: Int, days: Int, phase: TimeRangePhase = TimeRangePhase.Active, valid: Boolean = true) = TimeDisplayItem(
        id = id, type = ItemType.Date, remainingDays = days, isExpired = days < 0,
        detailFacts = TimeDetailFacts(0f, 0, 30, phase, inDays = true, validRange = valid),
    )

    @Test fun `a date due today and a waiting range cannot displace current work`() {
        val items = listOf(
            TimeDisplayItem(id = 1, type = ItemType.Date, remainingSortKey = 0),
            TimeDisplayItem(id = 2, type = ItemType.Time, timePhase = TimeRangePhase.Upcoming, percent = 0f),
            active(3, 600)
        )
        assertEquals(3, HomeHeroCandidates(items).hero(null)?.id)
    }

    @Test fun `manual selection survives closer overlapping ranges then falls back at completion`() {
        val items = listOf(active(9, 900), active(2, 300), active(1, 300))
        assertEquals(1, HomeHeroCandidates(items).hero(null)?.id)
        assertEquals(9, HomeHeroCandidates(items).hero(9)?.id)
        val ended = items.map { if (it.id == 9) it.copy(timePhase = TimeRangePhase.Finished) else it }
        assertEquals(1, HomeHeroCandidates(ended).hero(9)?.id)
        assertNull(HomeHeroCandidates(emptyList()).hero(9))
    }

    @Test fun `next ranges include tomorrow and exclude the active hero`() {
        val items = listOf(
            active(1, 300),
            TimeDisplayItem(id = 2, type = ItemType.Time, timePhase = TimeRangePhase.Finished, secondsUntilStart = 70_000),
            TimeDisplayItem(id = 3, type = ItemType.Time, timePhase = TimeRangePhase.Upcoming, secondsUntilStart = 600)
        )
        assertEquals(listOf(3, 2), upcomingTimeItems(items).map { it.id })
    }

    @Test fun `home keeps selected current work before falling back to a date`() {
        val dates = listOf(date(10, 0), date(11, 12))
        val items = dates + listOf(active(1, 300), active(2, 900))
        assertEquals(1, HomeHeroCandidates(items).hero(null)?.id)
        assertEquals(2, HomeHeroCandidates(items).hero(2)?.id)
        assertEquals(1, HomeHeroCandidates(items.filterNot { it.id == 2 }).hero(2)?.id)
        assertEquals(10, HomeHeroCandidates(dates).hero(2)?.id)
    }

    @Test fun `future dates remain visible beside many recurring time ranges`() {
        val waiting = (1..10).map {
            TimeDisplayItem(id = it, type = ItemType.Time, timePhase = TimeRangePhase.Finished, secondsUntilStart = it * 60L)
        }
        val items = waiting + listOf(date(12, 8, TimeRangePhase.Upcoming), date(11, 8), date(13, 20))
        assertEquals(listOf(11, 12, 13), HomeHeroCandidates(items).dateItems.map { it.id })
        assertEquals(11, HomeHeroCandidates(items).hero(null)?.id)
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
        assertEquals(listOf(5), HomeHeroCandidates(items).dateItems.map { it.id })
        assertEquals(5, HomeHeroCandidates(items).hero(null)?.id)
        assertNull(HomeHeroCandidates(items.filterNot { it.id == 5 }).hero(null))
        assertNull(HomeHeroCandidates(emptyList()).hero(null))
    }

    @Test fun `first valid pinned item keeps input priority over a selected active item`() {
        val pinnedDate = date(20, 10).copy(isPinned = true)
        val pinnedTime = active(2, 30).copy(isPinned = true)
        val selected = active(1, 10)

        assertEquals(20, HomeHeroCandidates(listOf(pinnedDate, selected, pinnedTime)).hero(1)?.id)
        assertEquals(2, HomeHeroCandidates(listOf(pinnedTime, pinnedDate, selected)).hero(1)?.id)
    }

    @Test fun `invalid expired and ineligible pinned rows do not displace valid work`() {
        val rows = listOf(
            date(10, 1, valid = false).copy(isPinned = true),
            date(11, -1).copy(isPinned = true),
            date(12, 1).copy(isPinned = true, dataError = true),
            TimeDisplayItem(id = 13, type = ItemType.Time, isPinned = true, timePhase = TimeRangePhase.Finished),
            active(1, 300),
        )

        assertEquals(1, HomeHeroCandidates(rows).hero(null)?.id)
    }

    @Test fun `a pinned waiting range can be the hero before its start`() {
        val waiting = TimeDisplayItem(
            id = 9, type = ItemType.Time, isPinned = true,
            timePhase = TimeRangePhase.Upcoming, secondsUntilStart = 600,
        )

        assertEquals(9, HomeHeroCandidates(listOf(date(5, 0), active(1, 30), waiting)).hero(1)?.id)
    }
}
