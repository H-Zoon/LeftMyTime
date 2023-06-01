package com.devidea.timeleft.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
