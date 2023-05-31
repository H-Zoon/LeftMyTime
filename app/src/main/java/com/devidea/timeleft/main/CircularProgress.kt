package com.devidea.timeleft.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devidea.timeleft.theme.light_blue_200

@Composable
fun CircularProgress(
    progress: Float,
    progressSize: Dp = 150.dp,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 20f,
    color: Color = light_blue_200
) {

    val animateFloat = remember { Animatable(0f) }
    LaunchedEffect(animateFloat) {
        animateFloat.animateTo(
            targetValue = progress*0.01f,
            animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier.size(progressSize)) {

        AnimatedNumberText(progress.toInt(), Modifier.align(Alignment.Center) )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val innerRadius = (size.minDimension - strokeWidth) / 2
            val halfSize = size / 2.0f
            val topLeft = Offset(
                x = halfSize.width - innerRadius,
                y = halfSize.height - innerRadius
            )
            val size = Size(innerRadius * 2, innerRadius * 2)

            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = animateFloat.value * 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = size
            )
        }
    }


}

@Composable
fun AnimatedNumberText(targetNumber: Int, modifier: Modifier) {
    var days by remember { mutableStateOf(0) }
    val daysCounter by animateIntAsState(
        targetValue = days,
        animationSpec = tween(
            durationMillis = 3000,
            easing = FastOutSlowInEasing
        )
    )
    LaunchedEffect(Unit) {
        days = targetNumber
    }
    Text(text = "$daysCounter%",
        modifier = modifier,
        style = TextStyle(
            fontSize = 50.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    )
}



@Preview
@Composable
fun PreviewCircularProgress() {
    CircularProgress(progress = 0.7f)
}