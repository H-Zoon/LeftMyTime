package com.devidea.timeleft.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.ui.components.remainingTimeLabel
import com.devidea.timeleft.ui.itemAccentColor
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing

/** A quiet list/grid cell. The historic name is kept to avoid duplicating public call sites. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TimeLeftItemCard(
    item: AdapterItem,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    grid: Boolean = false,
    upcoming: Boolean = false,
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    val countdown = if (upcoming) remainingTimeLabel(item.secondsUntilStart, null) else when {
        item.timePhase == TimeRangePhase.Active -> remainingTimeLabel(item.remainingSeconds, null)
        item.type == ItemType.Time -> remainingTimeLabel(item.secondsUntilStart, null)
        else -> item.countdownText.ifBlank { item.leftString }
    }
    val relation = stringResource(if (upcoming || (item.type == ItemType.Time && item.timePhase != TimeRangePhase.Active)) R.string.time_until_start else R.string.time_remaining)
    val expandedLabel = stringResource(if (expanded) R.string.card_details_expanded else R.string.card_details_collapsed)
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = expandedLabel }
                .padding(vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            if (grid || LocalDensity.current.fontScale > 1.2f) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(countdown, style = MaterialTheme.typography.titleLarge)
                if (item.type == ItemType.Time) Text(relation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.Top) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text(countdown, style = MaterialTheme.typography.titleMedium)
                        if (item.type == ItemType.Time) Text(relation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (item.type == ItemType.Time) "${item.startLabel}–${item.endLabel}" else item.dueText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            if (item.category.isNotBlank()) Text(item.category, style = MaterialTheme.typography.bodySmall, color = itemAccentColor(item.colorKey, MaterialTheme.colorScheme.primary))
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                listOf(item.startString, item.endString, item.updateInfo, item.recurrenceText, item.reminderText).filter { it.isNotBlank() }.distinct().forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    TextButton(onClick = { onEditItem(item.id) }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.card_action_edit)) }
                    TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.card_action_delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    if (showDeleteDialog) AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text(stringResource(R.string.card_delete_title)) },
        text = { Text(stringResource(R.string.card_delete_message, item.title)) },
        confirmButton = { TextButton(onClick = { showDeleteDialog = false; onDeleteItem(item.id) }) { Text(stringResource(R.string.card_action_delete), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
    )
}
