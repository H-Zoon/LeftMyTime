package com.devidea.timeleft

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val percentFormatters = ThreadLocal.withInitial { mutableMapOf<Locale, NumberFormat>() }

internal fun roundPercent(raw: Float): Float =
    if (raw.isFinite()) {
        BigDecimal(raw.toString()).setScale(1, RoundingMode.HALF_UP).toFloat()
    } else {
        raw
    }

internal fun formatPercent(value: Float, locale: Locale = Locale.getDefault()): String {
    val roundedValue = roundPercent(value.coerceIn(0f, 100f))
    val formatterCache = percentFormatters.get()
        ?: mutableMapOf<Locale, NumberFormat>().also(percentFormatters::set)
    val formatter = formatterCache.getOrPut(locale) {
        NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            maximumFractionDigits = 1
        }
    }
    formatter.minimumFractionDigits = if (roundedValue % 1f == 0f) 0 else 1
    return formatter.format(roundedValue)
}
