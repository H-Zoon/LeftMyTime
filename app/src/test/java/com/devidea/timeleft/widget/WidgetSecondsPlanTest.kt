package com.devidea.timeleft.widget

import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WidgetSecondsPlanTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val start = Instant.parse("2026-09-26T01:00:00Z")
    private val config = WidgetConfiguration(WidgetSource.Custom, itemId = 1, showSeconds = true)
    private fun item(startMillis: Long = start.toEpochMilli(), endMillis: Long = startMillis + 3_600_000) =
        AdapterItem(id = 1, type = ItemType.Time, startsAtMillis = startMillis, endsAtMillis = endMillis)

    @Test fun `boundaries switch upcoming active and finished without an app update`() {
        val plan = WidgetSecondsPlan.create(config, item(), start.toEpochMilli())!!
        assertEquals(WidgetSecondsPlan.Frame(TimeRangePhase.Upcoming, 1, 0f), plan.frame(start.minusSeconds(1), zone))
        assertEquals(WidgetSecondsPlan.Frame(TimeRangePhase.Active, 3600, 0f), plan.frame(start, zone))
        assertEquals(3599L, plan.frame(start.plusSeconds(1), zone).seconds)
        assertEquals(0.5f, plan.frame(start.plusSeconds(1800), zone).elapsedFraction, 0f)
        assertEquals(WidgetSecondsPlan.Frame(TimeRangePhase.Finished, 0, 1f), plan.frame(start.plusSeconds(3600), zone))
        assertEquals(0L, plan.frame(start.plusSeconds(86_400), zone).seconds)
        assertEquals(start.toEpochMilli(), plan.nextBoundaryMillis(start.toEpochMilli() - 1))
        assertEquals(start.plusSeconds(3600).toEpochMilli(), plan.nextBoundaryMillis(start.toEpochMilli()))
        assertNull(plan.nextBoundaryMillis(start.plusSeconds(3600).toEpochMilli()))
    }

    @Test fun `subsecond endpoints never show finished before the actual end`() {
        val plan = WidgetSecondsPlan.create(config, item(start.toEpochMilli() + 1, start.toEpochMilli() + 59_001), start.toEpochMilli())!!
        assertEquals(TimeRangePhase.Upcoming, plan.frame(start, zone).phase)
        assertEquals(1L, plan.frame(start.plusMillis(59_999), zone).seconds)
        assertEquals(TimeRangePhase.Finished, plan.frame(start.plusSeconds(60), zone).phase)
    }

    @Test fun `state comes from endpoints even when saved display strings and percentage disagree`() {
        val misleading = item().copy(percent = 100f, timePhase = TimeRangePhase.Finished, isExpired = true, leftString = "ended")
        val plan = WidgetSecondsPlan.create(config, misleading, start.toEpochMilli())!!
        assertEquals(TimeRangePhase.Active, plan.frame(start, zone).phase)
    }

    @Test fun `today rolls at local midnight and uses the app civil day convention`() {
        val plan = WidgetSecondsPlan.create(WidgetConfiguration(WidgetSource.Today), AdapterItem(), start.toEpochMilli())!!
        val midnight = Instant.parse("2026-09-26T15:00:00Z")
        assertEquals(WidgetSecondsPlan.Frame(TimeRangePhase.Finished, 0, 86_399 / 86_400f), plan.frame(midnight.minusSeconds(1), zone))
        assertEquals(WidgetSecondsPlan.Frame(TimeRangePhase.Active, 86_399, 0f), plan.frame(midnight, zone))
        assertNull(plan.nextBoundaryMillis(midnight.toEpochMilli()))
    }

    @Test fun `today follows timezone and DST wall clock while fixed schedule follows its instant`() {
        val ny = ZoneId.of("America/New_York")
        val before = Instant.parse("2026-03-08T06:59:59Z")
        val after = before.plusSeconds(1)
        val today = WidgetSecondsPlan(today = true)
        assertEquals(3601L, today.frame(before, ny).seconds - today.frame(after, ny).seconds)
        assertNotEquals(today.frame(after, zone).seconds, today.frame(after, ny).seconds)
        val fixed = WidgetSecondsPlan(false, before.epochSecond - 60, after.epochSecond + 60)
        assertEquals(1L, fixed.frame(before, ny).seconds - fixed.frame(after, ny).seconds)
        assertEquals(fixed.frame(after, zone), fixed.frame(after, ny))
    }

    @Test fun `unsupported content clocks and malformed or far endpoints fall back`() {
        val now = start.toEpochMilli()
        assertNull(WidgetSecondsPlan.create(config, null, now))
        assertNull(WidgetSecondsPlan.create(config, item().copy(dataError = true), now))
        assertNull(WidgetSecondsPlan.create(config, item().copy(isFocusSession = true), now))
        assertNull(WidgetSecondsPlan.create(config, item().copy(type = ItemType.Date), now))
        assertNull(WidgetSecondsPlan.create(config, item().copy(startsAtMillis = null), now))
        assertNull(WidgetSecondsPlan.create(config, item(now, now), now))
        assertNull(WidgetSecondsPlan.create(config, item(now, now - 1000), now))
        assertNull(WidgetSecondsPlan.create(config, item(now, now + 604_801_000), now))
        assertNull(WidgetSecondsPlan.create(config, item(now + 604_800_000), now))
        assertNull(WidgetSecondsPlan.create(config, item(-1000, 1000), 0))
        assertNull(WidgetSecondsPlan.create(config, item(Int.MAX_VALUE * 1000L, Int.MAX_VALUE * 1000L + 1000), now))
        assertNull(WidgetSecondsPlan.create(config.copy(legacySummary = true), item(), now))
        for (source in listOf(WidgetSource.Month, WidgetSource.Year, WidgetSource.Week, WidgetSource.Quarter, WidgetSource.Overview)) {
            assertNull(WidgetSecondsPlan.create(config.copy(source = source), item(), now))
        }
        assertNotNull(WidgetSecondsPlan.create(config.copy(source = WidgetSource.Next), item(), now))
    }
}
