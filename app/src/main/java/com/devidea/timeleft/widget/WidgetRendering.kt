package com.devidea.timeleft.widget

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.theme.ThemePalette
import com.devidea.timeleft.ui.theme.TimeRulerTokens
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

internal data class WidgetPalette(
    val backgroundDrawableRes: Int,
    val primary: Int,
    val onSurface: Int,
    val muted: Int,
    val track: Int,
) {
    companion object {
        fun fromPreferences(context: Context, prefs: SharedPreferences): WidgetPalette {
            val dark = when (prefs.getString(UserPreferences.KEY_THEME, UserPreferences.THEME_AUTO)) {
                UserPreferences.THEME_DARK -> true
                UserPreferences.THEME_LIGHT -> false
                else -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            }
            return create(prefs.getString(UserPreferences.KEY_COLOR_THEME, UserPreferences.COLOR_THEME_CLAY)
                ?: UserPreferences.COLOR_THEME_CLAY, dark)
        }
        fun create(key: String, dark: Boolean): WidgetPalette {
            val palette = ThemePalette.fromKey(key)
            val colors = if (dark) palette.darkColors else palette.lightColors
            return WidgetPalette(
                if (dark) R.drawable.widget_background_dark else R.drawable.widget_background_light,
                colors.primary.toArgb(), colors.onBackground.toArgb(), colors.onSurfaceVariant.toArgb(), colors.outlineVariant.toArgb(),
            )
        }
    }
}

internal fun resizeWidgetViews(context: Context, palette: WidgetPalette): RemoteViews =
    RemoteViews(context.packageName, R.layout.app_widget_resize).apply {
        setInt(R.id.widgetRoot, "setBackgroundResource", palette.backgroundDrawableRes)
        setTextColor(R.id.summary, palette.onSurface)
        setContentDescription(R.id.widgetRoot, context.getString(R.string.widget_size_hint))
    }

internal fun createSingleWidgetViews(
    context: Context,
    dimensions: WidgetDimensions,
    configuration: WidgetConfiguration,
    item: AdapterItem?,
    periods: List<AdapterItem>,
    palette: WidgetPalette,
    emptyMessage: Int,
    showProgress: Boolean,
): RemoteViews {
    if (!dimensions.meetsMinimum) return resizeWidgetViews(context, palette)
    val data = item?.toWidgetData(context, configuration.showRemaining,
        configuration.source == WidgetSource.Today || item.type == ItemType.Time)
        ?: WidgetDisplayData(context.getString(configuration.source.labelRes), context.getString(emptyMessage), "", 0,
            context.getString(configuration.source.labelRes) + ", " + context.getString(emptyMessage))
    val measure = WidgetTextMeasure(context)
    val padding = measure.px(R.dimen.widget_padding)
    val verticalPadding = if (dimensions.height < 200) measure.px(R.dimen.widget_compact_vertical_padding) else padding
    val gap = measure.px(R.dimen.widget_gap)
    val section = measure.px(R.dimen.widget_section_gap)
    val touch = measure.px(R.dimen.widget_touch_target)
    val width = measure.dp(dimensions.width.toFloat()) - padding * 2
    val titleWidth = width - touch - gap
    val valueSize = when {
        item == null -> R.dimen.widget_title_size
        data.isTextValue -> R.dimen.widget_message_value_size
        dimensions.width >= 340 && dimensions.height >= 260 -> R.dimen.widget_large_value_size
        else -> R.dimen.widget_value_size
    }
    val value = widgetValueWithStatus(context,
        widgetValueText(context, data.groups, data.value, R.dimen.widget_unit_size, palette.muted),
        data.valueTemplateRes, palette.muted)
    val rulerVisible = item != null && showProgress
    val rangeVisible = rulerVisible && data.startLabel.isNotBlank() && data.endLabel.isNotBlank()
    val meta = data.meta.takeUnless { rangeVisible }.orEmpty()
    val rangeHeight = if (rangeVisible) measure.px(R.dimen.widget_small_gap) +
        max(measure.height(data.startLabel, R.dimen.widget_label_size, width / 2),
            measure.height(data.endLabel, R.dimen.widget_label_size, width / 2)) else 0f
    val legacy = configuration.legacySummary && dimensions.sizeClass == WidgetSizeClass.Large
    val legacyValues = if (legacy) periods.map { it.title + " · " + it.toWidgetData(context, true, true).value } else emptyList()
    val baseHeight = verticalPadding * 2 + max(touch, measure.height(data.title, R.dimen.widget_title_size, titleWidth).toFloat()) +
        gap + measure.height(value, valueSize, width) +
        (if (meta.isBlank()) 0f else gap + measure.height(meta, R.dimen.widget_label_size, width)) +
        (if (rulerVisible) gap + measure.px(R.dimen.widget_ruler_height) else 0f) + rangeHeight +
        (if (legacyValues.isEmpty()) 0f else section + legacyValues.sumOf { measure.height(it, R.dimen.widget_label_size, width) } + gap * (legacyValues.size - 1))
    if (!measure.fitsUnbroken(value, valueSize, width) ||
        ceil(baseHeight.toDouble()) > measure.dp(dimensions.height.toFloat())) return resizeWidgetViews(context, palette)

    return RemoteViews(context.packageName, R.layout.app_widget_single).apply {
        setInt(R.id.widgetRoot, "setBackgroundResource", palette.backgroundDrawableRes)
        setViewPadding(R.id.widgetRoot, padding.roundToInt(), verticalPadding.roundToInt(), padding.roundToInt(), verticalPadding.roundToInt())
        setTextColor(R.id.summary, palette.onSurface)
        setTextViewText(R.id.summary, data.title)
        setTextColor(R.id.percent, palette.onSurface)
        setTextViewTextSize(R.id.percent, TypedValue.COMPLEX_UNIT_PX, measure.px(valueSize))
        setTextViewText(R.id.percent, value)
        setContentDescription(R.id.percent, data.accessibilityText)
        setInt(R.id.refresh, "setColorFilter", palette.muted)
        setTextColor(R.id.widgetMeta, palette.muted)
        setTextViewText(R.id.widgetMeta, meta)
        setViewVisibility(R.id.widgetMeta, if (meta.isBlank()) View.GONE else View.VISIBLE)
        setViewVisibility(R.id.widgetRuler, if (rulerVisible) View.VISIBLE else View.GONE)
        if (rulerVisible) setImageViewBitmap(R.id.widgetRuler, widgetRulerBitmap(context, width, data.progress, palette))
        setViewVisibility(R.id.widgetRange, if (rangeVisible) View.VISIBLE else View.GONE)
        setTextViewText(R.id.widgetStart, data.startLabel)
        setTextViewText(R.id.widgetEnd, data.endLabel)
        setContentDescription(R.id.widgetStart, context.getString(R.string.widget_range_start, data.startLabel))
        setContentDescription(R.id.widgetEnd, context.getString(R.string.widget_range_end, data.endLabel))
        setTextColor(R.id.widgetStart, palette.muted)
        setTextColor(R.id.widgetEnd, palette.muted)
        setViewVisibility(R.id.widgetLegacyFlow, if (legacy) View.VISIBLE else View.GONE)
        listOf(R.id.widgetFlowToday, R.id.widgetFlowMonth, R.id.widgetFlowYear).forEachIndexed { index, id ->
            setTextColor(id, palette.muted)
            setTextViewText(id, legacyValues.getOrElse(index) { "" })
        }
    }
}

private fun widgetRulerBitmap(context: Context, widthPx: Float, elapsed: Int, palette: WidgetPalette): Bitmap {
    val density = context.resources.displayMetrics.density
    val bitmap = createBitmap(widthPx.roundToInt().coerceAtLeast(1),
        (TimeRulerTokens.Height * density).roundToInt().coerceAtLeast(1))
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = TimeRulerTokens.StrokeWidth * density }
    val count = TimeRulerTokens.tickCount(widthPx / density)
    repeat(count) { index ->
        val fraction = (index + .5f) / count
        paint.color = if (fraction < elapsed / 100f) palette.track else palette.primary
        val height = if (index % 5 == 0) TimeRulerTokens.MajorHeight else TimeRulerTokens.MinorHeight
        val x = bitmap.width * fraction
        canvas.drawLine(x, bitmap.height.toFloat(), x, bitmap.height - height * density, paint)
    }
    return bitmap
}
