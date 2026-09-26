package com.devidea.timeleft.widget

import android.content.Context
import android.widget.RemoteViews
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.formatRemainingTime
import com.devidea.timeleft.remainingTimeGroups
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** All three periods keep equal typography; measure actual spans before selecting a layout. */
internal fun createOverviewViews(
    context: Context,
    dimensions: WidgetDimensions,
    periods: List<AdapterItem>,
    palette: WidgetPalette,
): RemoteViews {
    if (!dimensions.meetsMinimumFor(WidgetSource.Overview) || periods.size < 3) return resizeWidgetViews(context, palette)
    val measure = WidgetTextMeasure(context)
    val labels = listOf(R.string.period_today, R.string.period_month, R.string.period_year).map(context::getString)
    val plainValues = periods.take(3).map { formatRemainingTime(context, it.remainingSeconds, it.remainingDays, it.leftString) }
    val values = periods.take(3).mapIndexed { index, item ->
        widgetValueText(context, remainingTimeGroups(context, item.remainingSeconds, item.remainingDays),
            plainValues[index], R.dimen.widget_label_size, palette.muted)
    }
    val padding = measure.px(R.dimen.widget_padding)
    val verticalPadding = measure.px(R.dimen.widget_compact_vertical_padding)
    val gap = measure.px(R.dimen.widget_gap)
    val smallGap = measure.px(R.dimen.widget_small_gap)
    val touch = measure.px(R.dimen.widget_touch_target)
    val width = measure.dp(dimensions.width.toFloat()) - padding * 2
    // A short widget cannot reserve an extra title row. The period labels and accessibility
    // descriptions retain the meaning; column widths follow the actual number/unit groups.
    val availableColumnsWidth = (width - gap * 2).toInt()
    val minimumWidths = labels.indices.map { index ->
        ceil(maxOf(touch, measure.unbrokenWidth(labels[index], R.dimen.widget_label_size),
            measure.unbrokenWidth(values[index], R.dimen.widget_summary_value_size)).toDouble()).toInt()
    }
    val preferredWidths = labels.indices.map { index ->
        ceil(maxOf(minimumWidths[index].toFloat(), measure.width(labels[index], R.dimen.widget_label_size),
            measure.width(values[index], R.dimen.widget_summary_value_size)).toDouble()).toInt()
    }
    val columnWidths = if (minimumWidths.sum() <= availableColumnsWidth) {
        val extra = availableColumnsWidth - minimumWidths.sum()
        val wanted = preferredWidths.sum() - minimumWidths.sum()
        val widths = if (extra >= wanted) {
            // Preserve today's breathing room once every period fits on one line.
            preferredWidths.mapIndexed { index, value -> value + ((extra - wanted) * (if (index == 0) 1.5f else 1f) / 3.5f).toInt() }
        } else {
            minimumWidths.mapIndexed { index, value -> value + (extra.toFloat() * (preferredWidths[index] - value) / wanted).toInt() }
        }.toMutableList()
        widths[0] += availableColumnsWidth - widths.sum()
        widths
    } else null
    fun fits(horizontal: Boolean): Boolean {
        val heights = labels.indices.map { index ->
            val valueWidth = if (horizontal) {
                columnWidths?.get(index)?.toFloat() ?: return false
            } else (width - gap) * 2 / 3
            val labelWidth = if (horizontal) valueWidth else (width - gap) / 3
            if (!measure.fitsUnbroken(values[index], R.dimen.widget_summary_value_size, valueWidth)) return false
            val labelHeight = measure.height(labels[index], R.dimen.widget_label_size, labelWidth)
            val valueHeight = measure.height(values[index], R.dimen.widget_summary_value_size, valueWidth)
            max(touch, if (horizontal) labelHeight + smallGap + valueHeight else max(labelHeight, valueHeight).toFloat())
        }
        val required = verticalPadding * 2 +
            (if (horizontal) heights.maxOrNull() ?: 0f else heights.sum() + gap * 2)
        return ceil(required.toDouble()) <= measure.dp(dimensions.height.toFloat())
    }
    val layout = when {
        palette.layout == com.devidea.timeleft.ui.theme.TimeLayout.TimeBoard && fits(false) -> R.layout.app_widget_overview
        palette.layout == com.devidea.timeleft.ui.theme.TimeLayout.TimeBoard && fits(true) -> R.layout.app_widget_overview_board
        fits(true) -> R.layout.app_widget_overview_wide
        fits(false) -> R.layout.app_widget_overview
        else -> return resizeWidgetViews(context, palette)
    }
    return RemoteViews(context.packageName, layout).apply {
        setInt(R.id.widgetRoot, "setBackgroundResource", palette.backgroundDrawableRes)
        setViewPadding(R.id.widgetRoot, padding.roundToInt(), verticalPadding.roundToInt(), padding.roundToInt(), verticalPadding.roundToInt())
        val rows = listOf(R.id.overviewToday, R.id.overviewMonth, R.id.overviewYear)
        val labelIds = listOf(R.id.overviewTodayLabel, R.id.overviewMonthLabel, R.id.overviewYearLabel)
        val valueIds = listOf(R.id.overviewTodayValue, R.id.overviewMonthValue, R.id.overviewYearValue)
        labels.indices.forEach { index ->
            if (layout == R.layout.app_widget_overview_wide || layout == R.layout.app_widget_overview_board) {
                // TextView.setWidth works on pre-Android-12 RemoteViews as well.
                val columnWidth = requireNotNull(columnWidths)[index]
                setInt(labelIds[index], "setWidth", columnWidth)
                setInt(valueIds[index], "setWidth", columnWidth)
            }
            setTextViewText(labelIds[index], labels[index])
            setTextColor(labelIds[index], palette.muted)
            setTextViewText(valueIds[index], values[index])
            setTextColor(valueIds[index], palette.onSurface)
            setContentDescription(rows[index], context.getString(R.string.period_summary_description, labels[index], plainValues[index]))
        }
    }
}
