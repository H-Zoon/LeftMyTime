package com.devidea.timeleft.ui.home

import androidx.compose.ui.unit.Dp
import java.util.Locale

internal fun formatPercent(value: Float): String {
    val safeValue = value.coerceIn(0f, 100f)
    return if (safeValue % 1f == 0f) {
        safeValue.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", safeValue)
    }
}

internal fun dynamicDp(expanded: Dp, collapsed: Dp, fraction: Float): Dp =
    expanded + (collapsed - expanded) * fraction.coerceIn(0f, 1f)
