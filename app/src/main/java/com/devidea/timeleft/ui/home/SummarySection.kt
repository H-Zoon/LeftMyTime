package com.devidea.timeleft.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R

@Composable
internal fun SummarySection(
    topItems: List<AdapterItem>,
    modifier: Modifier = Modifier,
) {
    val items = topItems.take(3)
    if (items.isEmpty()) return

    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val activeIndex = selectedIndex.coerceIn(0, items.lastIndex)

    val accents = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    val flowItems = items.mapIndexed { index, item ->
        FlowOverviewItem(
            item = item,
            accent = accents[index % accents.size],
            startLabel = startLabelFor(index),
            endLabel = endLabelFor(index)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.home_time_overview),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (maxWidth >= 520.dp && flowItems.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExpandedFlowCard(
                        flowItem = flowItems[activeIndex],
                        modifier = Modifier.weight(1.45f)
                    )
                    FlowSelector(
                        flowItems = flowItems,
                        selectedIndex = activeIndex,
                        onSelect = { selectedIndex = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExpandedFlowCard(flowItem = flowItems[activeIndex])
                    FlowSelector(
                        flowItems = flowItems,
                        selectedIndex = activeIndex,
                        onSelect = { selectedIndex = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedFlowCard(
    flowItem: FlowOverviewItem,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = flowItem.progress,
        animationSpec = tween(durationMillis = 650),
        label = "expandedFlowProgress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        color = flowItem.accent.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, flowItem.accent.copy(alpha = 0.32f)),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flowItem.item.title.trim(),
                        style = MaterialTheme.typography.labelLarge,
                        color = flowItem.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = flowItem.item.leftString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatPercent(flowItem.item.percent) + "%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            FlowTrack(
                progress = progress,
                accent = flowItem.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = flowItem.startLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = flowItem.endLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FlowSelector(
    flowItems: List<FlowOverviewItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        flowItems.forEachIndexed { index, flowItem ->
            CompactFlowRow(
                flowItem = flowItem,
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun CompactFlowRow(
    flowItem: FlowOverviewItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = flowItem.progress,
        animationSpec = tween(durationMillis = 500),
        label = "compactFlowProgress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) flowItem.accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) flowItem.accent.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
        ),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = flowItem.item.title.trim(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) flowItem.accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatPercent(flowItem.item.percent) + "%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = flowItem.accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun FlowTrack(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "flowMarkerPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flowMarkerPulseScale"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val trackHeight = 10.dp.toPx()
        val markerRadius = 7.dp.toPx()
        val pulseRadius = 17.dp.toPx() * pulse
        val startX = trackHeight / 2f
        val endX = size.width - trackHeight / 2f
        val centerY = size.height / 2f
        val trackWidth = endX - startX
        val markerX = startX + trackWidth * clampedProgress

        drawLine(
            color = trackColor,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = Offset(startX, centerY),
            end = Offset(markerX, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )

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

        drawCircle(
            color = accent.copy(alpha = 0.18f),
            radius = pulseRadius,
            center = Offset(markerX, centerY)
        )
        drawCircle(
            color = surfaceColor,
            radius = markerRadius + 3.dp.toPx(),
            center = Offset(markerX, centerY)
        )
        drawCircle(
            color = accent,
            radius = markerRadius,
            center = Offset(markerX, centerY)
        )
    }
}

@Composable
private fun startLabelFor(index: Int): String =
    when (index) {
        0 -> stringResource(R.string.flow_start_today)
        1 -> stringResource(R.string.flow_start_month)
        else -> stringResource(R.string.flow_start_year)
    }

@Composable
private fun endLabelFor(index: Int): String =
    when (index) {
        0 -> stringResource(R.string.flow_end_today)
        1 -> stringResource(R.string.flow_end_month)
        else -> stringResource(R.string.flow_end_year)
    }

private data class FlowOverviewItem(
    val item: AdapterItem,
    val accent: Color,
    val startLabel: String,
    val endLabel: String,
) {
    val progress: Float = (item.percent / 100f).coerceIn(0f, 1f)
}
