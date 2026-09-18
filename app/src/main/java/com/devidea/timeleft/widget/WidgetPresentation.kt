package com.devidea.timeleft.widget

import android.content.Context
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.formatRemainingTime

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
