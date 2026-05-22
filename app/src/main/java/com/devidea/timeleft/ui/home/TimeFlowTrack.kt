package com.devidea.timeleft.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun TimeFlowTrack(
    progress: Float,
    accent: Color,
    trackColor: Color,
    tickColor: Color,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 10.dp,
    markerRadius: Dp = 7.dp,
    showTicks: Boolean = true,
    showPulse: Boolean = true,
) {
    val pulse = if (showPulse) {
        val transition = rememberInfiniteTransition(label = "timeFlowMarkerPulse")
        val animatedPulse by transition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "timeFlowMarkerPulseScale"
        )
        animatedPulse
    } else {
        1f
    }

    Canvas(modifier = modifier) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val trackHeightPx = trackHeight.toPx()
        val markerRadiusPx = markerRadius.toPx()
        val startX = trackHeightPx / 2f
        val endX = size.width - trackHeightPx / 2f
        val centerY = size.height / 2f
        val trackWidth = endX - startX
        val markerX = startX + trackWidth * clampedProgress

        drawLine(
            color = trackColor,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = trackHeightPx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(startX, centerY),
            end = Offset(markerX, centerY),
            strokeWidth = trackHeightPx,
            cap = StrokeCap.Round
        )

        if (showTicks) {
            for (index in 0..4) {
                val tickX = startX + trackWidth * index / 4f
                drawLine(
                    color = tickColor,
                    start = Offset(tickX, centerY - 18.dp.toPx()),
                    end = Offset(tickX, centerY - 11.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tickColor,
                    start = Offset(tickX, centerY + 11.dp.toPx()),
                    end = Offset(tickX, centerY + 18.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        if (showPulse) {
            drawCircle(
                color = accent.copy(alpha = 0.18f),
                radius = 17.dp.toPx() * pulse,
                center = Offset(markerX, centerY)
            )
        }
        drawCircle(
            color = surfaceColor,
            radius = markerRadiusPx + 3.dp.toPx(),
            center = Offset(markerX, centerY)
        )
        drawCircle(
            color = accent,
            radius = markerRadiusPx,
            center = Offset(markerX, centerY)
        )
    }
}
