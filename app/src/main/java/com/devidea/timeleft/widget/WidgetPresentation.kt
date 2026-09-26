package com.devidea.timeleft.widget

import android.content.Context
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.calc.currentOccurrence
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.formatRemainingTime
import com.devidea.timeleft.remainingTimeGroups
import com.devidea.timeleft.RemainingTimeGroup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Compatibility entry point shared by previews and installed widget details. */
internal fun ItemEntity.forWidgetPreview(today: LocalDate = LocalDate.now()): ItemEntity = currentOccurrence(today)

internal fun AdapterItem.toWidgetData(
    context: Context,
    showRemaining: Boolean,
    useWidgetString: Boolean,
): WidgetDisplayData {
    val waiting = !isFocusSession && type == ItemType.Time && secondsUntilStart != null
    val remainingText = formatRemainingTime(
        context, if (waiting) secondsUntilStart else remainingSeconds,
        remainingDays?.takeIf { it >= 0 }, countdownText.ifBlank { leftString }
    )
    val useCountdown = !showRemaining && (type == ItemType.Date || (useWidgetString && !waiting)) && countdownText.isNotBlank()
    val endedFocus = isFocusSession && isExpired
    val value = when {
        isCalendarOccurrence && isExpired -> countdownText
        endedFocus -> dueText
        useCountdown -> countdownText
        else -> remainingText
    }
    val valueTemplateRes = when {
        endedFocus -> null
        waiting -> R.string.widget_value_until_start
        isExpired || (remainingDays ?: 0) < 0 -> null
        useCountdown && type == ItemType.Date -> null // D-day already expresses its own state.
        else -> R.string.widget_value_remaining
    }
    // The renderer hides the repeated deadline when the range is visible, retaining it otherwise.
    val meta = dueText
    val spokenValue = valueTemplateRes?.let { context.getString(it).replace("^1", value) } ?: value
    val groups = if (useCountdown || endedFocus || isCalendarOccurrence && isExpired) emptyList() else remainingTimeGroups(context,
        if (waiting) secondsUntilStart else remainingSeconds, remainingDays?.takeIf { it >= 0 })
    return WidgetDisplayData(
        title = title, value = value, meta = meta,
        progress = (detailFacts?.percentElapsed ?: percent).takeIf { it.isFinite() }?.coerceIn(0f, 100f) ?: 0f,
        glowEnabled = detailFacts?.glowActive == true,
        accessibilityText = listOf(title, spokenValue, meta).filter { it.isNotBlank() }.joinToString(", "),
        valueTemplateRes = valueTemplateRes,
        groups = groups, isTextValue = !useCountdown && groups.isEmpty(),
        startLabel = startLabel, endLabel = endLabel,
    )
}


internal data class WidgetDisplayData(
    val title: String,
    val value: String,
    val meta: String,
    val progress: Float,
    val accessibilityText: String,
    val groups: List<RemainingTimeGroup> = emptyList(),
    val startLabel: String = "",
    val endLabel: String = "",
    val isTextValue: Boolean = false,
    val valueTemplateRes: Int? = null,
    val glowEnabled: Boolean = false,
)
