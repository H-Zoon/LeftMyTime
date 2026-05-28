package com.devidea.timeleft.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import kotlin.math.min

@Composable
internal fun HeaderSection(
    themeMode: String,
    todayItem: AdapterItem?,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    collapseFraction: Float,
    animateEntry: Boolean,
) {
    val topPadding = dynamicDp(16.dp, 8.dp, collapseFraction)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = topPadding)
    ) {
        if (todayItem != null) {
            HeaderTodayFlow(
                themeMode = themeMode,
                todayItem = todayItem,
                onToggleTheme = onToggleTheme,
                onOpenSettings = onOpenSettings,
                collapseFraction = collapseFraction,
                animateEntry = animateEntry
            )
        } else {
            HeaderFallback(
                themeMode = themeMode,
                onToggleTheme = onToggleTheme,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@Composable
private fun HeaderTodayFlow(
    themeMode: String,
    todayItem: AdapterItem,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    collapseFraction: Float,
    animateEntry: Boolean,
) {
    val accent = MaterialTheme.colorScheme.primary
    val targetProgress = (todayItem.percent / 100f).coerceIn(0f, 1f)
    var entryStarted by remember { mutableStateOf(!animateEntry) }
    LaunchedEffect(Unit) {
        entryStarted = true
    }
    val progress by animateFloatAsState(
        targetValue = if (entryStarted) targetProgress else 0f,
        animationSpec = tween(
            durationMillis = if (animateEntry) 760 else 650,
            delayMillis = if (animateEntry) 80 else 0
        ),
        label = "headerTodayProgress"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderTodayText(
            title = todayItem.title.trim(),
            leftString = todayItem.leftString,
            accent = accent,
            collapseFraction = collapseFraction,
            modifier = Modifier.weight(1f)
        )
        HeaderProgressRing(
            progress = progress,
            accent = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            sizeDp = dynamicDp(40.dp, 32.dp, collapseFraction)
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onToggleTheme) {
            Icon(
                imageVector = themeIcon(themeMode),
                contentDescription = stringResource(R.string.home_change_theme),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SettingsButton(onClick = onOpenSettings)
    }
}

@Composable
private fun HeaderProgressRing(
    progress: Float,
    accent: Color,
    trackColor: Color,
    sizeDp: Dp,
) {
    Box(
        modifier = Modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 3.dp.toPx()
            val inset = strokePx / 2f
            val diameter = min(size.width, size.height) - inset * 2f
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            val clamped = progress.coerceIn(0f, 1f)
            if (clamped > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun HeaderTodayText(
    title: String,
    leftString: String,
    accent: Color,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
) {
    val fraction = collapseFraction.coerceIn(0f, 1f)
    val expandedAlpha = ((0.72f - fraction) / 0.72f).coerceIn(0f, 1f)
    val collapsedAlpha = ((fraction - 0.28f) / 0.72f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dynamicDp(44.dp, 28.dp, fraction))
            .clipToBounds(),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = expandedAlpha
                    translationY = -6f * fraction
                }
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = leftString,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = collapsedAlpha
                    translationY = 4f * (1f - fraction)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = leftString,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun HeaderFallback(
    themeMode: String,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_today_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onToggleTheme) {
            Icon(
                imageVector = themeIcon(themeMode),
                contentDescription = stringResource(R.string.home_change_theme),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SettingsButton(onClick = onOpenSettings)
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.home_open_settings),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun themeIcon(themeMode: String) =
    when (themeMode) {
        "light" -> Icons.Filled.LightMode
        "dark" -> Icons.Filled.DarkMode
        else -> Icons.Filled.BrightnessAuto
    }
