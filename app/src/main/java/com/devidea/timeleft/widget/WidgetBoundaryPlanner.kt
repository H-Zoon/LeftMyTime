package com.devidea.timeleft.widget

import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.calc.currentOccurrence
import com.devidea.timeleft.calc.scheduledTimeWindow
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.focus.isFocusSession
import java.time.ZonedDateTime

/**
 * Backup for snapshots whose displayed time changes between system provider updates.
 * This is an inexact, non-waking refresh request, not a freshness guarantee during idle.
 */
internal fun nextSnapshotRefreshMillis(
    configuration: WidgetConfiguration,
    item: AdapterItem?,
    nowMillis: Long,
): Long? {
    val showsTime = configuration.source == WidgetSource.Today ||
        configuration.source == WidgetSource.Overview || configuration.legacySummary ||
        item?.type == ItemType.Time
    if (!showsTime) return null
    return listOfNotNull(
        nowMillis + 15 * 60_000L,
        item?.startsAtMillis?.takeIf { it > nowMillis },
        item?.endsAtMillis?.takeIf { it > nowMillis },
    ).minOrNull()
}

/** Keep overdue requests until their widget has actually been rendered or deleted. */
internal fun nextWidgetRefreshAlarmMillis(boundaries: Collection<Long>, nowMillis: Long): Long? =
    boundaries.minOfOrNull { if (it <= nowMillis) nowMillis + 60_000L else it }

/** An overdue inexact alarm may still be queued by Android. Do not keep postponing it. */
internal fun keepWidgetRefreshAlarm(scheduledMillis: Long?, nextMillis: Long): Boolean =
    scheduledMillis != null && scheduledMillis <= nextMillis

/** Recomputed from current data, never from an old alarm's item/occurrence payload. */
internal fun nextWidgetSelectionBoundary(items: List<ItemEntity>, now: ZonedDateTime): Long? {
    val candidates = items.filterNot { it.isTemplate || it.isFocusSession }
    val nowMillis = now.toInstant().toEpochMilli()
    val timed = candidates.asSequence().filter { it.type == ItemType.Time }
        .mapNotNull { entity -> runCatching { scheduledTimeWindow(entity.currentOccurrence(now.toLocalDate()), now) }.getOrNull() }
        .flatMap { sequenceOf(it.start.toInstant().toEpochMilli(), it.end.toInstant().toEpochMilli()) }
        .filter { it > nowMillis }.minOrNull()
    val midnight = if (candidates.any { it.type == ItemType.Date })
        now.toLocalDate().plusDays(1).atStartOfDay(now.zone).toInstant().toEpochMilli() else null
    return listOfNotNull(timed, midnight).minOrNull()
}
