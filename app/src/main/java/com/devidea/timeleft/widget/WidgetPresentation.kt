package com.devidea.timeleft.widget

import android.content.Context
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.calc.TimeProgressCalculator
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.formatRemainingTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Match the next rendered recurrence without persisting anything when configuration is canceled. */
internal fun ItemEntity.forWidgetPreview(today: LocalDate = LocalDate.now()): ItemEntity {
    if (type == ItemType.Time) return this
    val format = DateTimeFormatter.ofPattern("yyyy-M-d")
    val shift = TimeProgressCalculator.catchUpRecurrence(
        currentStart = LocalDate.parse(startValue, format),
        currentEnd = LocalDate.parse(endValue, format),
        today = today,
        updateFlag = updateFlag,
        updateRate = updateRate,
    ) ?: return this
    return copy(startValue = shift.newStart.toString(), endValue = shift.newEnd.toString())
}

internal fun AdapterItem.toWidgetData(
    context: Context,
    showRemaining: Boolean,
    useWidgetString: Boolean,
): WidgetDisplayData {
    val waiting = type == ItemType.Time && timePhase != TimeRangePhase.Active
    val remainingText = formatRemainingTime(
        context, if (waiting) secondsUntilStart else remainingSeconds,
        remainingDays?.takeIf { it >= 0 }, countdownText.ifBlank { leftString }
    )
    val value = when {
        !showRemaining && (type == ItemType.Date || (useWidgetString && !waiting)) -> countdownText.ifBlank { remainingText }
        else -> remainingText
    }
    val meta = when {
        waiting -> context.getString(R.string.time_until_start)
        dueText.isNotBlank() -> dueText
        else -> context.getString(R.string.time_remaining)
    }
    return WidgetDisplayData(
        title = title, value = value, meta = meta,
        progress = percent.toInt().coerceIn(0, 100),
        accessibilityText = "$title, $value, $meta"
    )
}


internal data class WidgetDisplayData(
    val title: String,
    val value: String,
    val meta: String,
    val progress: Int,
    val accessibilityText: String,
)
