package com.devidea.timeleft.home

import com.devidea.timeleft.database.ItemEntity
import com.devidea.timeleft.database.ItemType
import com.devidea.timeleft.database.RecurrenceMode
import com.devidea.timeleft.ui.TimeDisplayItem
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HomeDateCacheTest {
    private val day = LocalDate.of(2026, 11, 2)

    private fun date(id: Int = 1) = ItemEntity(
        id = id, type = ItemType.Date, title = "Schedule",
        startValue = "2026-11-1", endValue = "2026-11-2",
        updateFlag = RecurrenceMode.Day, updateRate = 3,
    )

    @Test fun `same date and equal source reuse the display across second ticks`() {
        var builds = 0
        val cache = HomeDateCache {
            builds++
            TimeDisplayItem(title = it.title)
        }
        val raw = date()
        val first = cache.item(raw, day, 0L)

        for (tick in 1..60) {
            assertSame(first, cache.item(raw.copy(), day, tick * 1_000L))
        }
        assertEquals(1, builds)
    }

    @Test fun `date changes project the new recurrence and support a date moving backwards`() {
        val cache = HomeDateCache { TimeDisplayItem(startLabel = it.startValue, endLabel = it.endValue) }
        val raw = date()
        val beforeMidnight = cache.item(raw, day, 0L)
        val afterMidnight = cache.item(raw, day.plusDays(1), 1_000L)

        assertEquals("2026-11-2", beforeMidnight.endLabel)
        assertEquals("2026-11-05", afterMidnight.startLabel)
        assertEquals("2026-11-06", afterMidnight.endLabel)
        assertEquals(beforeMidnight, cache.item(raw, day, 2_000L))
        assertEquals("2026-11-2", raw.endValue)
    }

    @Test fun `source edits refresh text and recurrence even when the id stays the same`() {
        val cache = HomeDateCache { TimeDisplayItem(title = it.title, endLabel = it.endValue) }
        val raw = date()
        val first = cache.item(raw, day.plusDays(1), 0L)
        val edited = cache.item(raw.copy(title = "Updated", updateRate = 5), day.plusDays(1), 1_000L)

        assertEquals("2026-11-06", first.endLabel)
        assertEquals("Updated", edited.title)
        assertEquals("2026-11-08", edited.endLabel)
    }

    @Test fun `pin expires within the day and is restored if the clock moves backwards`() {
        var now = 999L
        var builds = 0
        val cache = HomeDateCache {
            builds++
            TimeDisplayItem(isPinned = it.isPinned && (it.pinnedUntilMillis ?: Long.MAX_VALUE) > now)
        }
        val raw = date().copy(isPinned = true, pinnedUntilMillis = 1_000L)
        assertTrue(cache.item(raw, day, now).isPinned)

        now = 1_000L
        val expired = cache.item(raw, day, now)
        assertFalse(expired.isPinned)
        now = 2_000L
        assertSame(expired, cache.item(raw, day, now))
        now = 999L
        assertTrue(cache.item(raw, day, now).isPinned)
        assertEquals(3, builds)
    }

    @Test fun `unlimited pin does not expire as seconds advance`() {
        var builds = 0
        val cache = HomeDateCache {
            builds++
            TimeDisplayItem(isPinned = it.isPinned)
        }
        val raw = date().copy(isPinned = true)
        val first = cache.item(raw, day, 0L)

        assertTrue(first.isPinned)
        assertSame(first, cache.item(raw, day, 60_000L))
        assertEquals(1, builds)
    }

    @Test fun `deleted or retyped ids cannot reuse an old date display when they return`() {
        var builds = 0
        val cache = HomeDateCache {
            builds++
            TimeDisplayItem(id = it.id)
        }
        val raw = date()
        val first = cache.item(raw, day, 0L)
        val other = cache.item(date(2), day, 0L)
        cache.retainDatesIn(listOf(date(2)))

        assertSame(other, cache.item(date(2), day, 1_000L))
        val restored = cache.item(raw, day, 1_000L)
        assertNotSame(first, restored)
        cache.retainDatesIn(listOf(raw.copy(type = ItemType.Time), date(2)))
        assertNotSame(restored, cache.item(raw, day, 2_000L))
        assertEquals(4, builds)
    }

    @Test fun `a cached invalid row can be corrected without invalidating another row`() {
        val cache = HomeDateCache {
            TimeDisplayItem(id = it.id, dataError = it.startValue == "invalid")
        }
        val invalid = date().copy(startValue = "invalid")
        val valid = cache.item(date(2), day, 0L)
        assertTrue(cache.item(invalid, day, 0L).dataError)

        assertFalse(cache.item(date(), day, 1_000L).dataError)
        assertSame(valid, cache.item(date(2), day, 1_000L))
    }
}
