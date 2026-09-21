package com.devidea.timeleft.ui.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.devidea.timeleft.ui.theme.TimeRulerTokens
import kotlin.math.roundToInt

private val glowStops = FloatArray(33) { it / 32f }

/** The same bounded light for Compose and RemoteViews, including Android versions before API 31. */
internal fun drawTimeRulerGlow(canvas: Canvas, width: Float, height: Float, elapsed: Float, color: Int, alpha: Float) {
    if (!elapsed.isFinite() || elapsed >= 1f || width <= 0 || height <= 0) return
    val left = width * elapsed.coerceIn(0f, 1f)
    val remainingWidth = width - left
    if (remainingWidth <= 0f) return
    // Fade only vertically: every x in the remaining span must have the same light profile.
    // The smooth falloff reaches transparency at the top and bottom with zero slope.
    // Keep the original hue even at zero alpha to prevent a dark fringe.
    val colors = IntArray(glowStops.size) { index ->
        val distance = glowStops[index] * 2f - 1f
        val fade = 1f - distance * distance
        val opacity = (alpha * fade * fade * fade).coerceIn(0f, 1f)
        Color.argb((opacity * 255).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))
    }
    val light = LinearGradient(
        0f, height * (TimeRulerTokens.GlowCenterY - TimeRulerTokens.GlowRadiusY),
        0f, height * (TimeRulerTokens.GlowCenterY + TimeRulerTokens.GlowRadiusY),
        colors, glowStops, Shader.TileMode.CLAMP,
    )
    // Fill from the remaining-time boundary through the end, without horizontal fading.
    canvas.drawRect(left, 0f, width, height, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = light })
}
