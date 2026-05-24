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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.itemAccentColor
import com.devidea.timeleft.ui.itemIconVector

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TimeLeftItemCard(
    item: AdapterItem,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
    progressDisplayMode: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
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

    val cardModifier = modifier.fillMaxWidth()
    val contentPadding = if (compact) 0.dp else 12.dp

    if (compact) {
        Column(modifier = cardModifier) {
            ItemCardContent(
                item = item,
                accent = accent,
                progress = progress,
                progressDisplayMode = progressDisplayMode,
                expanded = false,
                chevronRotation = chevronRotation,
                showDetails = false,
                onToggleExpanded = {},
                onEditItem = onEditItem,
                onDeleteClick = { showDeleteDialog = true },
                compact = true,
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 2.dp)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                modifier = Modifier.padding(start = 76.dp)
            )
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            ItemCardContent(
                item = item,
                accent = accent,
                progress = progress,
                progressDisplayMode = progressDisplayMode,
                expanded = expanded,
                chevronRotation = chevronRotation,
                showDetails = true,
                onToggleExpanded = { expanded = !expanded },
                onEditItem = onEditItem,
                onDeleteClick = { showDeleteDialog = true },
                compact = false,
                modifier = Modifier.padding(contentPadding)
            )
        }
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
    progressDisplayMode: String,
    expanded: Boolean,
    chevronRotation: Float,
    showDetails: Boolean,
    onToggleExpanded: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CountdownBadge(
                    text = item.countdownText.ifBlank { formatPercent(item.percent) + "%" },
                    iconKey = item.iconKey,
                    color = accent
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.dueText.ifBlank { item.leftString },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showDetails) {
                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.card_show_details),
                            modifier = Modifier.rotate(chevronRotation)
                        )
                    }
                }
            }
            if (progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN) {
                Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    if (progressDisplayMode == UserPreferences.PROGRESS_DISPLAY_FULL) {
                        Text(
                            text = stringResource(R.string.card_progress_value, formatPercent(item.percent)),
                            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            if (showDetails) {
                if (item.recurrenceText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataChips(item = item, accent = accent)
                } else if (item.category.isNotBlank() || item.reminderText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataChips(item = item, accent = accent)
                }
            }
            AnimatedVisibility(visible = expanded && showDetails) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailText(item.startString)
                    DetailText(item.endString)
                    DetailText(item.leftString)
                    if (item.updateInfo.isNotBlank()) {
                        DetailText(item.updateInfo)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.card_action_edit))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
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

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MetadataChips(
    item: AdapterItem,
    accent: androidx.compose.ui.graphics.Color,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
private fun CountdownBadge(
    text: String,
    iconKey: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.10f),
        contentColor = color,
        modifier = Modifier.size(width = 58.dp, height = 48.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = itemIconVector(iconKey),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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
