package com.devidea.timeleft.calc

import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Display projection only: reading a home, widget or preview never mutates the saved schedule. */
fun ItemEntity.currentOccurrence(today: LocalDate = LocalDate.now()): ItemEntity {
    if (type == ItemType.Time) return this
    val format = DateTimeFormatter.ofPattern("yyyy-M-d")
    val shift = runCatching {
        TimeProgressCalculator.catchUpRecurrence(LocalDate.parse(startValue, format), LocalDate.parse(endValue, format),
            today, updateFlag, updateRate)
    }.getOrNull() ?: return this
    return copy(startValue = shift.newStart.toString(), endValue = shift.newEnd.toString())
}
