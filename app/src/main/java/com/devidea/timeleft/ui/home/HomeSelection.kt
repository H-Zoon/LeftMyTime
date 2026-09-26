package com.devidea.timeleft.ui.home

import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemType

internal fun activeTimeItems(items: List<AdapterItem>): List<AdapterItem> = items
    .filter { it.type == ItemType.Time && it.timePhase == TimeRangePhase.Active }
    .sortedWith(compareBy<AdapterItem> { it.remainingSeconds ?: Long.MAX_VALUE }.thenBy { it.id })

internal fun selectActiveTimeItem(items: List<AdapterItem>, selectedId: Int?): AdapterItem? {
    val active = activeTimeItems(items)
    return active.firstOrNull { it.id == selectedId } ?: active.firstOrNull()
}

internal fun upcomingTimeItems(items: List<AdapterItem>): List<AdapterItem> = items
    .filter { it.type == ItemType.Time && it.timePhase != TimeRangePhase.Active && it.secondsUntilStart != null }
    .sortedWith(compareBy<AdapterItem> { it.secondsUntilStart }.thenBy { it.id })

/** A date ending today is still available; expired or invalid periods stay in schedule management. */
internal fun homeDateItems(items: List<AdapterItem>): List<AdapterItem> = items
    .filter {
        it.type == ItemType.Date && !it.isExpired && (it.remainingDays ?: -1) >= 0 &&
            it.detailFacts?.validRange == true &&
            it.detailFacts.phase in listOf(TimeRangePhase.Active, TimeRangePhase.Upcoming)
    }
    .sortedWith(compareBy<AdapterItem> { it.remainingDays }.thenBy { it.id })

internal fun selectHomeHero(items: List<AdapterItem>, selectedActiveId: Int?): AdapterItem? =
    items.firstOrNull { it.isPinned && !it.isExpired && !it.dataError &&
        (it in homeDateItems(items) || it.type == ItemType.Time && (it.timePhase == TimeRangePhase.Active || it.secondsUntilStart != null)) }
        ?: selectActiveTimeItem(items, selectedActiveId) ?: homeDateItems(items).firstOrNull()
