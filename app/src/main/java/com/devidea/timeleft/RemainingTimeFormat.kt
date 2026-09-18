package com.devidea.timeleft

import android.content.Context

/** Shared wording for Compose and RemoteViews; no parsing of localized display strings. */
fun formatRemainingTime(context: Context, seconds: Long?, days: Int?, fallback: String = ""): String = when {
    days != null -> context.resources.getQuantityString(R.plurals.time_days, days.coerceAtLeast(0), days.coerceAtLeast(0))
    seconds == null -> fallback
    seconds < 60 -> context.getString(R.string.time_under_minute)
    seconds < 3600 -> context.getString(R.string.time_minutes, seconds / 60)
    else -> context.getString(R.string.time_hours_minutes, seconds / 3600, seconds % 3600 / 60)
}
