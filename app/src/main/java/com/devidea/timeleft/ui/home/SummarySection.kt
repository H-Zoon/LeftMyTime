package com.devidea.timeleft.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.formatPercent

@Composable
internal fun SummarySection(
    topItems: List<AdapterItem>,
    modifier: Modifier = Modifier,
    collapseFraction: Float,
) {
    val contextItems = topItems.drop(1).take(2)
    if (contextItems.isEmpty()) return
    val visibleFraction = (1f - collapseFraction).coerceIn(0f, 1f)

    val accents = listOf(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    val flowItems = contextItems.mapIndexed { index, item ->
        FlowOverviewItem(
            item = item,
            accent = accents[index % accents.size]
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(dynamicDp(64.dp, 0.dp, collapseFraction))
            .clipToBounds()
            .graphicsLayer { alpha = visibleFraction },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            flowItems.forEach { flowItem ->
                CompactFlowCard(
                    flowItem = flowItem,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactFlowCard(
    flowItem: FlowOverviewItem,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = flowItem.progress,
        animationSpec = tween(durationMillis = 500),
        label = "compactContextFlowProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatPercent(flowItem.item.percent) + "%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        TimeFlowTrack(
            progress = progress,
            accent = flowItem.accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            surfaceColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            trackHeight = 4.dp,
            markerRadius = 2.dp,
            showTicks = false,
            showPulse = false
        )
    }
}

private data class FlowOverviewItem(
    val item: AdapterItem,
    val accent: Color,
) {
    val progress: Float = (item.percent / 100f).coerceIn(0f, 1f)
}
