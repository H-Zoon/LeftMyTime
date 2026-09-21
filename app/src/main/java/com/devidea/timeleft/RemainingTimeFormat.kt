package com.devidea.timeleft

import android.content.Context

data class RemainingTimeGroup(val number: String, val unit: String, val isSeconds: Boolean = false)

/** Numeric groups shared by Compose and RemoteViews, independent of translated sentences. */
fun remainingTimeGroups(context: Context, seconds: Long?, days: Int?, showSeconds: Boolean = false): List<RemainingTimeGroup> = when {
    days != null -> listOf(RemainingTimeGroup(days.coerceAtLeast(0).toString(),
        context.resources.getQuantityString(R.plurals.time_unit_days, days.coerceAtLeast(0))))
    seconds != null && showSeconds -> buildList {
        val value = seconds.coerceAtLeast(0)
        if (value >= 3600) add(RemainingTimeGroup((value / 3600).toString(), context.getString(R.string.time_unit_hours)))
        if (value >= 60) add(RemainingTimeGroup((value % 3600 / 60).toString(), context.getString(R.string.time_unit_minutes)))
        add(RemainingTimeGroup((value % 60).toString().padStart(2, '0'), context.getString(R.string.time_unit_seconds), isSeconds = true))
    }
    seconds == null || seconds < 60 -> emptyList()
    seconds < 3600 -> listOf(RemainingTimeGroup((seconds / 60).toString(), context.getString(R.string.time_unit_minutes)))
    else -> listOf(
        RemainingTimeGroup((seconds / 3600).toString(), context.getString(R.string.time_unit_hours)),
        RemainingTimeGroup((seconds % 3600 / 60).toString(), context.getString(R.string.time_unit_minutes)),
    )
}

/** Shared wording for Compose and RemoteViews; no parsing of localized display strings. */
fun formatRemainingTime(context: Context, seconds: Long?, days: Int?, fallback: String = "", showSeconds: Boolean = false): String = when {
    days != null -> context.resources.getQuantityString(R.plurals.time_days, days.coerceAtLeast(0), days.coerceAtLeast(0))
    seconds == null -> fallback
    showSeconds && seconds >= 3600 -> context.getString(R.string.time_hours_minutes_seconds, seconds / 3600, seconds % 3600 / 60, seconds % 60)
    showSeconds && seconds >= 60 -> context.getString(R.string.time_minutes_seconds, seconds / 60, seconds % 60)
    showSeconds -> context.getString(R.string.time_seconds, seconds.coerceAtLeast(0))
    seconds < 60 -> context.getString(R.string.time_under_minute)
    seconds < 3600 -> context.getString(R.string.time_minutes, seconds / 60)
    else -> context.getString(R.string.time_hours_minutes, seconds / 3600, seconds % 3600 / 60)
}
