package com.devidea.timeleft.widget

import android.appwidget.AppWidgetManager
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import androidx.core.content.edit
import com.devidea.timeleft.R

internal enum class WidgetSource(val prefValue: String, val labelRes: Int) {
    Today("embedTime", R.string.widget_configure_today),
    Month("embedMonth", R.string.widget_configure_month),
    Year("embedYear", R.string.widget_configure_year),
    Week("embedWeek", R.string.period_week),
    Quarter("embedQuarter", R.string.period_quarter),
    Overview("calendarOverview", R.string.widget_configure_overview),
    Next("nextCustom", R.string.widget_configure_next),
    Custom("custom", R.string.widget_configure_custom);

    companion object {
        fun fromPrefValue(value: String): WidgetSource =
            values().firstOrNull { it.prefValue == value } ?: Custom

        // Keep the four installed provider identities. Their names no longer determine content or size.
        fun defaultForProvider(className: String?): WidgetSource = when (className) {
            MediumAppWidget::class.java.name -> Month
            WideAppWidget::class.java.name -> Year
            LargeAppWidget::class.java.name -> Overview
            ScheduleAppWidget::class.java.name -> Next
            PeriodAppWidget::class.java.name -> Week
            else -> Today
        }
    }
}

/** Render exact sizes reported by modern launchers; keep paired orientation sizes as fallback. */
@Suppress("DEPRECATION")
internal fun widgetViewsForSizes(options: Bundle, source: WidgetSource, render: (WidgetDimensions) -> RemoteViews): RemoteViews {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            .orEmpty().filter { it.width > 0 && it.height > 0 }.distinct().take(16)
        if (sizes.isNotEmpty()) return RemoteViews(sizes.associateWith { render(WidgetDimensions(it.width.toInt(), it.height.toInt())) })
    }
    val portrait = WidgetDimensions.fromOptions(options, source)
    val landscape = WidgetDimensions(
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).takeIf { it > 0 } ?: portrait.width,
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 } ?: portrait.height,
    )
    return if (portrait == landscape) render(portrait) else RemoteViews(render(landscape), render(portrait))
}

internal data class WidgetPreview(
    val dimensions: WidgetDimensions,
    val views: RemoteViews,
    val fits: Boolean = views.layoutId != R.layout.app_widget_resize,
)

/** Preview-only growth bounds; installed widgets always use the size supplied by their host. */
private const val PREVIEW_GROWTH_DP = 64
private const val PREVIEW_MAX_WIDTH_DP = 640
private const val PREVIEW_MAX_HEIGHT_DP = 960

internal fun widgetContentPreview(
    requested: WidgetDimensions,
    source: WidgetSource,
    render: (WidgetDimensions) -> WidgetPreview,
): WidgetPreview {
    val representative = WidgetDimensions.previewFor(source)
    var size = if (requested.meetsMinimumFor(source)) requested else representative
    val maxWidth = maxOf(size.width, PREVIEW_MAX_WIDTH_DP)
    val maxHeight = maxOf(size.height, PREVIEW_MAX_HEIGHT_DP)
    while (true) {
        val preview = render(size)
        if (preview.fits) return preview
        val next = WidgetDimensions(
            maxOf(representative.width, minOf(size.width + PREVIEW_GROWTH_DP, maxWidth)),
            maxOf(representative.height, minOf(size.height + PREVIEW_GROWTH_DP, maxHeight)),
        )
        if (next == size) return preview
        size = next
    }
}

/** Static pin/picker previews contain only variants that actually show their content. */
internal fun widgetPreviewForSizes(source: WidgetSource, render: (WidgetDimensions) -> RemoteViews): RemoteViews {
    val minimum = WidgetDimensions.minimumFor(source)
    val preview = WidgetDimensions.previewFor(source)
    if (Build.VERSION.SDK_INT < 31) return widgetContentPreview(preview, source) {
        WidgetPreview(it, render(it))
    }.views
    val sizes = listOf(minimum, WidgetDimensions(preview.width, minimum.height),
        WidgetDimensions(minimum.width, preview.height), preview,
        WidgetDimensions(340, 260), WidgetDimensions(360, 320), WidgetDimensions(400, 240)).distinct()
    val rendered = sizes.map { WidgetPreview(it, render(it)) }.filter { it.fits }
    val content = rendered.ifEmpty {
        listOf(widgetContentPreview(preview, source) { WidgetPreview(it, render(it)) })
    }
    return RemoteViews(content.associate {
        SizeF(it.dimensions.width.toFloat(), it.dimensions.height.toFloat()) to it.views
    })
}

internal data class WidgetConfiguration(
    val source: WidgetSource = WidgetSource.Today,
    val itemId: Int? = null,
    val showRemaining: Boolean = true,
    val legacySummary: Boolean = false,
    val showSeconds: Boolean = false,
) {
    fun write(prefs: SharedPreferences, id: Int) {
        val value = if (source == WidgetSource.Custom) requireNotNull(itemId).toString() else source.prefValue
        prefs.edit {
            putString(id.toString(), value)
            putBoolean("${id}option", showRemaining)
            putString(displayKey(id), if (legacySummary) "legacy" else "focused")
            putBoolean(secondsKey(id), showSeconds)
        }
    }

    companion object {
        fun displayKey(id: Int) = "${id}displayMode"
        fun secondsKey(id: Int) = "${id}seconds"

        fun hasSavedSettings(prefs: SharedPreferences, id: Int): Boolean =
            prefs.contains(id.toString()) || prefs.contains("${id}option") || prefs.contains(displayKey(id))

        fun read(
            prefs: SharedPreferences,
            id: Int,
            forConfiguration: Boolean = false,
            defaultSource: WidgetSource = WidgetSource.Today,
        ): WidgetConfiguration {
            if (forConfiguration && !hasSavedSettings(prefs, id)) return WidgetConfiguration(source = defaultSource)
            val value = prefs.getString(id.toString(), "").orEmpty()
            return WidgetConfiguration(
                // Missing legacy selections already render the month as their fallback.
                source = if (value.isEmpty()) WidgetSource.Month else WidgetSource.fromPrefValue(value),
                itemId = value.toIntOrNull(),
                showRemaining = prefs.getBoolean("${id}option", false),
                legacySummary = prefs.getString(displayKey(id), "legacy") == "legacy",
                showSeconds = prefs.getBoolean(secondsKey(id), false),
            )
        }
    }
}

internal data class WidgetDimensions(val width: Int, val height: Int) {
    val meetsMinimum: Boolean get() = width >= MIN_WIDTH && height >= MIN_HEIGHT
    fun meetsMinimumFor(source: WidgetSource): Boolean {
        val minimum = minimumFor(source)
        return width >= minimum.width && height >= minimum.height
    }
    val sizeClass: WidgetSizeClass
        get() = when {
            width >= 245 && height >= 185 -> WidgetSizeClass.Large
            width >= 245 && height < 115 -> WidgetSizeClass.Wide
            height >= 115 -> WidgetSizeClass.Medium
            else -> WidgetSizeClass.Compact
        }

    companion object {
        // Actual content bounds in widget_tokens.xml, NOT the provider's legacy cell hints.
        // The host declares 110dp / 40dp for 2 / 1 rows but reports the real allocated size here.
        const val MIN_WIDTH = 250
        const val MIN_HEIGHT = 160
        val Minimum = WidgetDimensions(MIN_WIDTH, MIN_HEIGHT)
        val OverviewMinimum = WidgetDimensions(MIN_WIDTH, 64)
        fun minimumFor(source: WidgetSource) = if (source == WidgetSource.Overview) OverviewMinimum else Minimum
        // Match widget_preview_* resources. These are representative sizes, not fixed cell dimensions.
        fun previewFor(source: WidgetSource) = WidgetDimensions(320, if (source == WidgetSource.Overview) 96 else 200)
        fun fromOptions(options: Bundle, source: WidgetSource = WidgetSource.Today) = WidgetDimensions(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: previewFor(source).width,
            // The range's minimum width and maximum height describe portrait together.
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 }
                ?: options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 } ?: previewFor(source).height,
        )
    }
}

internal enum class WidgetSizeClass { Compact, Medium, Wide, Large }
