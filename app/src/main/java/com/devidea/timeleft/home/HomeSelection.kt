package com.devidea.timeleft.home

import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.ItemType

internal class HomeHeroCandidates(items: List<TimeDisplayItem>) {
    val activeItems = items.filter { it.type == ItemType.Time && it.timePhase == TimeRangePhase.Active }
        .sortedWith(compareBy<TimeDisplayItem> { it.remainingSeconds ?: Long.MAX_VALUE }.thenBy { it.id })
    val dateItems = items.filter { it.isHomeDate() }
        .sortedWith(compareBy<TimeDisplayItem> { it.remainingDays }.thenBy { it.id })

    private val pinned = items.firstOrNull {
        it.isPinned && !it.isExpired && !it.dataError && (it.isHomeDate() ||
            it.type == ItemType.Time && (it.timePhase == TimeRangePhase.Active || it.secondsUntilStart != null))
    }

    fun hero(selectedActiveId: Int?): TimeDisplayItem? =
        pinned ?: activeItems.firstOrNull { it.id == selectedActiveId } ?: activeItems.firstOrNull() ?: dateItems.firstOrNull()
}

internal fun upcomingTimeItems(items: List<TimeDisplayItem>): List<TimeDisplayItem> = items
    .filter { it.type == ItemType.Time && it.timePhase != TimeRangePhase.Active && it.secondsUntilStart != null }
    .sortedWith(compareBy<TimeDisplayItem> { it.secondsUntilStart }.thenBy { it.id })

/** A date ending today is still available; expired or invalid periods stay in schedule management. */
private fun TimeDisplayItem.isHomeDate(): Boolean =
    type == ItemType.Date && !isExpired && (remainingDays ?: -1) >= 0 &&
        detailFacts?.validRange == true &&
        (detailFacts.phase == TimeRangePhase.Active || detailFacts.phase == TimeRangePhase.Upcoming)
