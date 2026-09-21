package com.devidea.timeleft

import android.content.Context

data class RemainingTimeGroup(val number: String, val unit: String)

/** Numeric groups shared by Compose and RemoteViews, independent of translated sentences. */
fun remainingTimeGroups(context: Context, seconds: Long?, days: Int?): List<RemainingTimeGroup> = when {
    days != null -> listOf(RemainingTimeGroup(days.coerceAtLeast(0).toString(),
        context.resources.getQuantityString(R.plurals.time_unit_days, days.coerceAtLeast(0))))
    seconds == null || seconds < 60 -> emptyList()
    seconds < 3600 -> listOf(RemainingTimeGroup((seconds / 60).toString(), context.getString(R.string.time_unit_minutes)))
    else -> listOf(
        RemainingTimeGroup((seconds / 3600).toString(), context.getString(R.string.time_unit_hours)),
        RemainingTimeGroup((seconds % 3600 / 60).toString(), context.getString(R.string.time_unit_minutes)),
    )
}

/** Shared wording for Compose and RemoteViews; no parsing of localized display strings. */
fun formatRemainingTime(context: Context, seconds: Long?, days: Int?, fallback: String = ""): String = when {
    days != null -> context.resources.getQuantityString(R.plurals.time_days, days.coerceAtLeast(0), days.coerceAtLeast(0))
    seconds == null -> fallback
    seconds < 60 -> context.getString(R.string.time_under_minute)
    seconds < 3600 -> context.getString(R.string.time_minutes, seconds / 60)
    else -> context.getString(R.string.time_hours_minutes, seconds / 3600, seconds % 3600 / 60)
}
