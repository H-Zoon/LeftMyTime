package com.devidea.timeleft.ui.home

import java.util.Locale

internal fun formatPercent(value: Float): String {
    val safeValue = value.coerceIn(0f, 100f)
    return if (safeValue % 1f == 0f) {
        safeValue.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", safeValue)
    }
}
