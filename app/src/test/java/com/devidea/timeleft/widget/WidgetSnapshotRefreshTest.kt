package com.devidea.timeleft.widget

import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.database.itemdata.ItemType
import org.junit.Assert.*
import org.junit.Test

class WidgetSnapshotRefreshTest {
    private val now = 1_790_000_000_000L

    @Test fun `today and overview retain a refresh request without seconds support`() {
        for (source in listOf(WidgetSource.Today, WidgetSource.Overview)) {
            assertEquals(now + 900_000L,
                nextSnapshotRefreshMillis(WidgetConfiguration(source, showSeconds = false), null, now))
        }
    }

    @Test fun `a time boundary earlier than the periodic refresh wins`() {
        val configuration = WidgetConfiguration(WidgetSource.Custom, 1)
        val item = AdapterItem(type = ItemType.Time, startsAtMillis = now + 120_000, endsAtMillis = now + 600_000)
        assertEquals(now + 120_000, nextSnapshotRefreshMillis(configuration, item, now))
        assertEquals(now + 600_000, nextSnapshotRefreshMillis(configuration, item, now + 120_000))
    }

    @Test fun `date-only widgets do not acquire a frequent refresh unless they show legacy time`() {
        assertNull(nextSnapshotRefreshMillis(WidgetConfiguration(WidgetSource.Month), null, now))
        assertEquals(now + 900_000L,
            nextSnapshotRefreshMillis(WidgetConfiguration(WidgetSource.Month, legacySummary = true), null, now))
    }

    @Test fun `updating another widget cannot discard an overdue refresh`() {
        assertEquals(now + 60_000L, nextWidgetRefreshAlarmMillis(listOf(now - 10_800_000L, now + 900_000L), now))
        assertEquals(now + 900_000L, nextWidgetRefreshAlarmMillis(listOf(now + 900_000L), now))
        assertEquals(now + 2_000L, nextWidgetRefreshAlarmMillis(listOf(now + 2_000L), now))
        assertNull(nextWidgetRefreshAlarmMillis(emptyList(), now))
    }

    @Test fun `overdue retry cannot hide a nearer future boundary in a different widget`() {
        assertEquals(now + 2_000L,
            nextWidgetRefreshAlarmMillis(listOf(now - 10_800_000L, now + 2_000L), now))
    }

    @Test fun `renders keep a pending alarm even after its requested time`() {
        assertTrue(keepWidgetRefreshAlarm(now + 900_000L, now + 960_000L))
        assertTrue(keepWidgetRefreshAlarm(now - 1_000L, now + 60_000L))
        assertTrue(keepWidgetRefreshAlarm(now + 2_000L, now + 2_000L))
        assertFalse(keepWidgetRefreshAlarm(now + 900_000L, now + 2_000L))
        assertFalse(keepWidgetRefreshAlarm(null, now + 60_000L))
    }
}
