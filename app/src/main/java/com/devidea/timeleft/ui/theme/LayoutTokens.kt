package com.devidea.timeleft.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Semantic layout roles shared by home, editor and settings. */
object LayoutTokens {
    val ScreenHorizontal
        @Composable get() = if (LocalConfiguration.current.screenWidthDp < 360) Spacing.l else Spacing.page
    val ScreenVertical = Spacing.l
    val SectionGap = Spacing.xxl
    val CardPadding = Spacing.l
    val MinTouchTarget = 48.dp
    val TopAppBarMinHeight = 64.dp
    const val DetailSheetMaxHeightFraction = .8f
}
