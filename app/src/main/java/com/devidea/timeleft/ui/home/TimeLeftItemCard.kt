package com.devidea.timeleft.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.formatPercent
import com.devidea.timeleft.ui.itemAccentColor
import com.devidea.timeleft.ui.itemIconVector
import com.devidea.timeleft.ui.theme.Spacing
import kotlin.math.min

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TimeLeftItemCard(
    item: AdapterItem,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    grid: Boolean = false,
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    val accent = itemAccentColor(item.colorKey, countdownAccent(item))
    val progress by animateFloatAsState(
        targetValue = (item.percent / 100f).coerceIn(0f, 1f),
        label = "itemProgress"
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "itemChevron"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        ItemCardContent(
            item = item,
            accent = accent,
            progress = progress,
            expanded = expanded,
            chevronRotation = chevronRotation,
            onToggleExpanded = { expanded = !expanded },
            onEditItem = onEditItem,
            onDeleteClick = { showDeleteDialog = true },
            grid = grid,
            modifier = Modifier.padding(Spacing.m)
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.card_delete_title)) },
            text = { Text(stringResource(R.string.card_delete_message, item.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteItem(item.id)
                    }
                ) {
                    Text(stringResource(R.string.card_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ItemCardContent(
    item: AdapterItem,
    accent: androidx.compose.ui.graphics.Color,
    progress: Float,
    expanded: Boolean,
    chevronRotation: Float,
    onToggleExpanded: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    grid: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (grid) {
            GridItemSummary(
                item = item,
                accent = accent,
                progress = progress,
                chevronRotation = chevronRotation,
                onToggleExpanded = onToggleExpanded
            )
        } else {
            ListItemSummary(
                item = item,
                accent = accent,
                progress = progress,
                chevronRotation = chevronRotation,
                onToggleExpanded = onToggleExpanded
            )
        }
        if (!grid && (
                item.recurrenceText.isNotBlank() ||
                    item.category.isNotBlank() ||
                    item.reminderText.isNotBlank()
                )
        ) {
            Spacer(modifier = Modifier.height(Spacing.s))
            MetadataChips(item = item, accent = accent)
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(modifier = Modifier.height(Spacing.m))
                DetailText(item.startString)
                DetailText(item.endString)
                DetailText(item.leftString)
                if (item.updateInfo.isNotBlank()) {
                    DetailText(item.updateInfo)
                }
                Spacer(modifier = Modifier.height(Spacing.s))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onEditItem(item.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.s))
                        Text(stringResource(R.string.card_action_edit))
                    }
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    TextButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        if (!grid) {
                            Spacer(modifier = Modifier.width(Spacing.s))
                            Text(
                                text = stringResource(R.string.card_action_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListItemSummary(
    item: AdapterItem,
    accent: Color,
    progress: Float,
    chevronRotation: Float,
    onToggleExpanded: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CountdownRingBadge(
            progress = progress,
            iconKey = item.iconKey,
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(Spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Spacing.s))
                Text(
                    text = item.countdownLabel(),
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    maxLines = 1
                )
            }
            Text(
                text = item.dueText.ifBlank { item.leftString },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        ExpandButton(
            rotation = chevronRotation,
            onClick = onToggleExpanded
        )
    }
}

@Composable
private fun GridItemSummary(
    item: AdapterItem,
    accent: Color,
    progress: Float,
    chevronRotation: Float,
    onToggleExpanded: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CountdownRingBadge(
            progress = progress,
            iconKey = item.iconKey,
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        ExpandButton(
            rotation = chevronRotation,
            onClick = onToggleExpanded
        )
    }
    Spacer(modifier = Modifier.height(Spacing.m))
    Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    Text(
        text = item.countdownLabel(),
        style = MaterialTheme.typography.titleSmall,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Text(
        text = item.dueText.ifBlank { item.leftString },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ExpandButton(
    rotation: Float,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.card_show_details),
            modifier = Modifier.rotate(rotation)
        )
    }
}

private fun AdapterItem.countdownLabel(): String =
    countdownText.ifBlank { formatPercent(percent) + "%" }

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MetadataChips(
    item: AdapterItem,
    accent: androidx.compose.ui.graphics.Color,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        if (item.recurrenceText.isNotBlank()) {
            InfoChip(text = item.recurrenceText, color = MaterialTheme.colorScheme.secondary)
        }
        if (item.category.isNotBlank()) {
            InfoChip(text = item.category, color = accent)
        }
        if (item.reminderText.isNotBlank()) {
            InfoChip(text = item.reminderText, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun CountdownRingBadge(
    progress: Float,
    iconKey: String,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(BADGE_SIZE),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(BADGE_SIZE)) {
            val strokePx = BADGE_STROKE.toPx()
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
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Icon(
            imageVector = itemIconVector(iconKey),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}

private val BADGE_SIZE = 48.dp
private val BADGE_STROKE = 3.dp

@Composable
private fun DetailText(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 3.dp)
    )
}
