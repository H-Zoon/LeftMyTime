package com.devidea.timeleft.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import com.devidea.timeleft.theme.light_blue_200

@Composable
fun CircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 20f,
    color: Color = light_blue_200
) {

    val animateFloat = remember { Animatable(0f) }
    LaunchedEffect(animateFloat) {
        animateFloat.animateTo(
            targetValue = progress * 0.01f,
            animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing))
    }

        Canvas(modifier = modifier) {
            val innerRadius = (size.minDimension - strokeWidth) / 2
            val halfSize = size / 2.0f
            val topLeft = Offset(
                x = halfSize.width - innerRadius,
                y = halfSize.height - innerRadius
            )
            val size = Size(innerRadius * 2, innerRadius * 2)

            val startAngle = -90f
            val sweepAngle = animateFloat.value * 360f

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = size
            )
        }
    }

@Preview
@Composable
fun PreviewCircularProgress() {
    CircularProgress(progress = 60f)
}