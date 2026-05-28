package com.devidea.timeleft.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.devidea.timeleft.ui.itemAccentColor
import com.devidea.timeleft.ui.itemIconVector
import kotlin.math.min

private val RING_SIZE = 220.dp
private val RING_STROKE = 16.dp

@Composable
internal fun NextCountdownHero(
    item: AdapterItem,
    progressDisplayMode: String,
    modifier: Modifier = Modifier,
) {
    val accent = itemAccentColor(item.colorKey, countdownAccent(item))
    val targetProgress = (item.percent / 100f).coerceIn(0f, 1f)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(item.id) { entered = true }
    val progress by animateFloatAsState(
        targetValue = if (entered) targetProgress else 0f,
        animationSpec = tween(durationMillis = 720, delayMillis = 60, easing = FastOutSlowInEasing),
        label = "heroRingProgress"
    )
    val showRing = progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN
    val showPercentLabel = progressDisplayMode == UserPreferences.PROGRESS_DISPLAY_FULL

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeroLabelRow(item = item, accent = accent)

            if (showRing) {
                CountdownRingBlock(
                    item = item,
                    accent = accent,
                    progress = progress,
                    showPercentLabel = showPercentLabel,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                CompactCenterBlock(item = item)
            }

            val footer = item.dueText.ifBlank { item.leftString }
            if (footer.isNotBlank()) {
                Text(
                    text = footer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HeroLabelRow(item: AdapterItem, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    .padding(7.dp)
                    .size(18.dp)
            )
        }
        Text(
            text = stringResource(R.string.home_next_countdown),
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CountdownRingBlock(
    item: AdapterItem,
    accent: Color,
    progress: Float,
    showPercentLabel: Boolean,
    trackColor: Color,
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
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.countdownText.ifBlank { "${formatPercent(item.percent)}%" },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showPercentLabel) {
                Text(
                    text = stringResource(R.string.card_progress_value, formatPercent(item.percent)),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactCenterBlock(item: AdapterItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.countdownText.ifBlank { "${formatPercent(item.percent)}%" },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun countdownAccent(item: AdapterItem): Color =
    when {
        item.isExpired -> MaterialTheme.colorScheme.onSurfaceVariant
        item.remainingSortKey <= SECONDS_PER_DAY -> MaterialTheme.colorScheme.error
        item.remainingSortKey <= SECONDS_PER_WEEK -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

private const val SECONDS_PER_DAY = 86_400L
private const val SECONDS_PER_WEEK = SECONDS_PER_DAY * 7
