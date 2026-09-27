package com.devidea.timeleft.widget

import com.devidea.timeleft.calc.currentOccurrence
import com.devidea.timeleft.calc.scheduledTimeWindow
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.focus.isFocusSession
import java.time.ZonedDateTime

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
