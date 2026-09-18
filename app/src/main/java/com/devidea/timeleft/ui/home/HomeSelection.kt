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
