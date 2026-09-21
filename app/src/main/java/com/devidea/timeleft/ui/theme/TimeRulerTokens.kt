package com.devidea.timeleft.ui.theme

/** dp geometry shared by the Compose canvas and RemoteViews bitmap. */
object TimeRulerTokens {
    const val Height = 28f
    const val MajorHeight = 24f
    const val MinorHeight = 12f
    const val StrokeWidth = 2f
    const val TickSpacing = 5f
    fun tickCount(widthDp: Float): Int = (widthDp / TickSpacing).toInt().coerceIn(20, 60)
}
