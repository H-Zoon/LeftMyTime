package com.devidea.timeleft.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.itemAccentColor
import com.devidea.timeleft.ui.itemIconVector

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun NextCountdownHero(
    item: AdapterItem,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    val accent = itemAccentColor(item.colorKey, countdownAccent(item))
    val progress by animateFloatAsState(
        targetValue = (item.percent / 100f).coerceIn(0f, 1f),
        label = "heroProgress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.13f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_next_countdown),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onEditItem(item.id) }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.card_action_edit),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.card_action_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = itemIconVector(item.iconKey),
                    contentDescription = null,
                    tint = accent
                )
                Text(
                    text = item.countdownText.ifBlank { formatPercent(item.percent) + "%" },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.dueText.ifBlank { item.leftString },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                InfoChip(
                    text = stringResource(R.string.card_progress_value, formatPercent(item.percent)),
                    color = accent
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
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
internal fun InfoChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
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
