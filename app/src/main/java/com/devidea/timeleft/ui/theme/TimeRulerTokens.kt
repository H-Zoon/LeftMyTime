package com.devidea.timeleft.ui.theme

/** dp geometry shared by the Compose canvas and RemoteViews bitmap. */
object TimeRulerTokens {
    const val Height = 28f
    const val MajorHeight = 24f
    const val MinorHeight = 12f
    const val StrokeWidth = 2f
    const val TickSpacing = 5f
    // App-only direct manipulation; bitmap widgets keep their static geometry.
    const val TouchLift = 4f
    const val TouchRadius = 28f
    const val CurrentSnapRadius = 6f
    const val CurrentSnapMaxFraction = .02f
    const val HapticIntervalMs = 120L
    const val GlowLightAlpha = .10f
    const val GlowDarkAlpha = .20f
    // Finish fading inside the drawing bounds so the top and bottom do not cut through the light.
    const val GlowCenterY = .5f
    const val GlowRadiusY = .5f
    fun tickCount(widthDp: Float): Int = (widthDp / TickSpacing).toInt().coerceIn(20, 60)
}
