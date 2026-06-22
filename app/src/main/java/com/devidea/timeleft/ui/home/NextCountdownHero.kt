package com.devidea.timeleft.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.formatPercent
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.itemIconVector
import com.devidea.timeleft.ui.theme.Motion
import com.devidea.timeleft.ui.theme.Spacing
import kotlin.math.min

private val RING_SIZE = 116.dp
private val RING_STROKE = 10.dp

@Composable
internal fun NextCountdownHero(
    item: AdapterItem,
    progressDisplayMode: String,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val targetProgress = (item.percent / 100f).coerceIn(0f, 1f)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(item.id) { entered = true }
    val progress by animateFloatAsState(
        targetValue = if (entered) targetProgress else 0f,
        animationSpec = tween(durationMillis = Motion.EmphasizedMs, delayMillis = 60, easing = FastOutSlowInEasing),
        label = "heroRingProgress"
    )
    val showRing = progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN
    val countdownText = item.countdownText.ifBlank { "${formatPercent(item.percent)}%" }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(Spacing.l),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showRing) {
                CountdownRingBlock(
                    progress = progress,
                    accent = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    centerText = countdownText
                )
                Spacer(modifier = Modifier.width(Spacing.l))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                HeroLabelRow(item = item, accent = accent)
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!showRing) {
                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val footer = item.dueText.ifBlank { item.leftString }
                if (footer.isNotBlank()) {
                    Text(
                        text = footer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroLabelRow(item: AdapterItem, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = accent.copy(alpha = 0.12f),
            contentColor = accent
        ) {
            Icon(
                imageVector = itemIconVector(item.iconKey),
                contentDescription = null,
                modifier = Modifier
                    .padding(5.dp)
                    .size(14.dp)
            )
        }
        Text(
            text = stringResource(R.string.home_next_countdown),
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CountdownRingBlock(
    progress: Float,
    accent: Color,
    trackColor: Color,
    centerText: String,
) {
    Box(
        modifier = Modifier.size(RING_SIZE),
        contentAlignment = Alignment.Center
    ) {
        CountdownRing(
            progress = progress,
            accent = accent,
            trackColor = trackColor,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = centerText,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.m)
        )
    }
}

@Composable
private fun CountdownRing(
    progress: Float,
    accent: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokePx = RING_STROKE.toPx()
        val inset = strokePx / 2f + 2.dp.toPx()
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

@Composable
internal fun InfoChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
