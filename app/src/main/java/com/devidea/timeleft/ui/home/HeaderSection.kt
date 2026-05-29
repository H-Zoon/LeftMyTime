package com.devidea.timeleft.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R

@Composable
internal fun HeaderSection(
    topItems: List<AdapterItem>,
    onOpenSettings: () -> Unit,
    collapseFraction: Float,
) {
    val topPadding = dynamicDp(16.dp, 8.dp, collapseFraction)
    var cycleIndex by rememberSaveable { mutableIntStateOf(0) }
    val total = topItems.size
    val safeIndex = if (total > 0) cycleIndex.coerceAtMost(total - 1) else 0
    val displayedItem = if (total > 0) topItems[safeIndex] else null

    val onCycle = {
        if (total > 0) cycleIndex = (cycleIndex + 1) % total
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = topPadding)
    ) {
        if (displayedItem != null) {
            HeaderTodayFlow(
                item = displayedItem,
                onCycle = onCycle,
                onOpenSettings = onOpenSettings,
                collapseFraction = collapseFraction,
                currentIndex = safeIndex,
                total = total
            )
        } else {
            HeaderFallback(onOpenSettings = onOpenSettings)
        }
    }
}

@Composable
private fun HeaderTodayFlow(
    item: AdapterItem,
    onCycle: () -> Unit,
    onOpenSettings: () -> Unit,
    collapseFraction: Float,
    currentIndex: Int,
    total: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onCycle)
        ) {
            HeaderTodayText(
                title = item.title.trim(),
                leftString = item.leftString,
                accent = MaterialTheme.colorScheme.primary,
                collapseFraction = collapseFraction
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        HeaderPagerDots(currentIndex = currentIndex, total = total)
        SettingsButton(onClick = onOpenSettings)
    }
}

@Composable
private fun HeaderPagerDots(currentIndex: Int, total: Int) {
    if (total <= 1) return
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
    Row(
        modifier = Modifier.padding(end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isActive = index == currentIndex
            val targetSize by animateDpAsState(
                targetValue = if (isActive) 7.dp else 5.dp,
                animationSpec = tween(durationMillis = 220),
                label = "headerDotSize"
            )
            Box(
                modifier = Modifier
                    .size(targetSize)
                    .clip(CircleShape)
                    .background(if (isActive) activeColor else inactiveColor)
            )
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
private fun HeaderFallback(onOpenSettings: () -> Unit) {
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

