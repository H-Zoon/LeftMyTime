package com.devidea.timeleft

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

internal fun roundPercent(raw: Float): Float =
    if (raw.isFinite()) {
        BigDecimal(raw.toString()).setScale(1, RoundingMode.HALF_UP).toFloat()
    } else {
        raw
    }

internal fun formatPercent(value: Float, locale: Locale = Locale.getDefault()): String {
    val roundedValue = roundPercent(value.coerceIn(0f, 100f))
    return NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        minimumFractionDigits = if (roundedValue % 1f == 0f) 0 else 1
        maximumFractionDigits = 1
    }.format(roundedValue)
}
