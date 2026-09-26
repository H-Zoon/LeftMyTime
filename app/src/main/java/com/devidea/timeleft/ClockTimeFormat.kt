package com.devidea.timeleft

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Wall clock labels only. Durations and persisted H:m values must not use this formatter. */
fun formatClockTime(context: Context, time: LocalTime, seconds: Boolean = false): String {
    val locale = context.resources.configuration.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(context)) {
        if (seconds) "Hms" else "Hm"
    } else if (seconds) "hms" else "hm"
    return time.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
}
