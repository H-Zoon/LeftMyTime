package com.devidea.timeleft.ui.home

import androidx.compose.ui.unit.Dp

internal fun dynamicDp(expanded: Dp, collapsed: Dp, fraction: Float): Dp =
    expanded + (collapsed - expanded) * fraction.coerceIn(0f, 1f)
