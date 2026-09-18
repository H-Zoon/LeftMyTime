package com.devidea.timeleft.widget

import android.appwidget.AppWidgetManager
import android.content.SharedPreferences
import android.os.Bundle
import com.devidea.timeleft.R

internal enum class WidgetSource(val prefValue: String, val labelRes: Int) {
    Today("embedTime", R.string.widget_configure_today),
    Month("embedMonth", R.string.widget_configure_month),
    Year("embedYear", R.string.widget_configure_year),
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
            else -> Today
        }
    }
}

internal data class WidgetConfiguration(
    val source: WidgetSource = WidgetSource.Today,
    val itemId: Int? = null,
    val showRemaining: Boolean = true,
    val legacySummary: Boolean = false,
) {
    companion object {
        fun displayKey(id: Int) = "${id}displayMode"

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
            )
        }
    }
}

internal data class WidgetDimensions(val width: Int, val height: Int) {
    val sizeClass: WidgetSizeClass
        get() = when {
            width >= 245 && height >= 185 -> WidgetSizeClass.Large
            width >= 245 && height < 115 -> WidgetSizeClass.Wide
            height >= 115 -> WidgetSizeClass.Medium
            else -> WidgetSizeClass.Compact
        }

    companion object {
        fun fromOptions(options: Bundle) = WidgetDimensions(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: 245,
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 } ?: 185,
        )
    }
}

internal enum class WidgetSizeClass(val layoutRes: Int) {
    Compact(R.layout.app_widget),
    Medium(R.layout.app_widget_medium),
    Wide(R.layout.app_widget_wide),
    Large(R.layout.app_widget_large),
}
