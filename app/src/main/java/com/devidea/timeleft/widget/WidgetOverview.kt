package com.devidea.timeleft.widget

import android.content.Context
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.widget.RemoteViews
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.formatRemainingTime
import kotlin.math.ceil

/** The measurements mirror the two overview XML layouts. Never drop an individual period to fit. */
internal fun createOverviewViews(
    context: Context,
    dimensions: WidgetDimensions,
    periods: List<AdapterItem>,
    background: Int,
    foreground: Int,
    muted: Int,
): RemoteViews {
    val metrics = context.resources.displayMetrics
    fun dp(value: Int) = value * metrics.density
    val labels = listOf(R.string.period_today, R.string.period_month, R.string.period_year).map(context::getString)
    val values = periods.take(3).map {
        formatRemainingTime(context, it.remainingSeconds, it.remainingDays, it.leftString)
            .replace(Regex("""(\d+)\s+(\p{L}+)""")) { match -> "${match.groupValues[1]}\u00a0${match.groupValues[2]}" }
    }
    val title = context.getString(R.string.period_summary_title)
    fun textPaint(sp: Float) = TextPaint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics)
        typeface = Typeface.create("sans", Typeface.NORMAL)
        fontFeatureSettings = "tnum"
    }
    fun textHeight(text: String, sp: Float, width: Float): Int {
        return StaticLayout.Builder.obtain(text, 0, text.length, textPaint(sp), width.toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(true).build().height
    }
    val innerWidth = dp(dimensions.width - 32).coerceAtLeast(1f)
    fun fits(horizontal: Boolean): Boolean {
        val columnWidth = if (horizontal) innerWidth / 3 - dp(8) else innerWidth
        if (columnWidth < dp(64) || values.size != 3) return false
        // Keep each number and its unit together instead of breaking them across narrow columns.
        val valuePaint = textPaint(22f)
        if (values.any { value -> value.split(' ').any { valuePaint.measureText(it) > columnWidth } }) return false
        val heights = labels.indices.map { index ->
            textHeight(labels[index], 12f, columnWidth) + dp(4) + textHeight(values[index], 22f, columnWidth)
        }
        val rowsHeight = if (horizontal) heights.maxOrNull() ?: 0f else heights.sum() + dp(24)
        val required = dp(32 + 12 + 4) + textHeight(title, 14f, innerWidth) + rowsHeight
        return ceil(required.toDouble()) <= dp(dimensions.height)
    }
    val horizontal = fits(true)
    val layout = when {
        horizontal -> R.layout.app_widget_overview_wide
        fits(false) -> R.layout.app_widget_overview
        else -> R.layout.app_widget_overview_resize
    }
    return RemoteViews(context.packageName, layout).apply {
        setInt(R.id.widgetRoot, "setBackgroundResource", background)
        setTextColor(R.id.summary, muted)
        if (layout == R.layout.app_widget_overview_resize) {
            setTextViewText(R.id.summary, context.getString(R.string.widget_overview_resize))
            setContentDescription(R.id.widgetRoot, context.getString(R.string.widget_overview_size_hint))
        } else {
            setTextViewText(R.id.summary, title)
            val labelIds = listOf(R.id.overviewTodayLabel, R.id.overviewMonthLabel, R.id.overviewYearLabel)
            val valueIds = listOf(R.id.overviewTodayValue, R.id.overviewMonthValue, R.id.overviewYearValue)
            labels.indices.forEach { index ->
                setTextViewText(labelIds[index], labels[index])
                setTextColor(labelIds[index], muted)
                setTextViewText(valueIds[index], values[index])
                setTextColor(valueIds[index], foreground)
                setContentDescription(valueIds[index], context.getString(R.string.period_summary_description, labels[index], values[index]))
            }
        }
    }
}
