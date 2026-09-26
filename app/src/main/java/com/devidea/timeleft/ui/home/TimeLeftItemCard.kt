package com.devidea.timeleft.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.ui.components.remainingTimeLabel
import com.devidea.timeleft.ui.components.TimeDetailSheet
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.ui.itemAccentColor
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.widget.PinWidgetButton
import com.devidea.timeleft.widget.WidgetSource

private const val DetailExpandDurationMillis = 220
private const val DetailCollapseDurationMillis = 180

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
    progressDisplayMode: String = UserPreferences.PROGRESS_DISPLAY_FULL,
    interactive: Boolean = true,
    onDuplicate: ((Int) -> Unit)? = null,
    onSaveTemplate: ((Int) -> Unit)? = null,
    onPin: ((AdapterItem) -> Unit)? = null,
    onMove: ((Int, Int) -> Unit)? = null,
) {
    val board = com.devidea.timeleft.ui.theme.LocalTimeLayout.current == com.devidea.timeleft.ui.theme.TimeLayout.TimeBoard
    var moreActions by remember { mutableStateOf(false) }
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(item.id) { mutableStateOf(false) }
    var showTimeDetails by rememberSaveable(item.id) { mutableStateOf(false) }
    val countdown = if (upcoming) remainingTimeLabel(item.secondsUntilStart, null) else when {
        item.isCalendarOccurrence && item.isExpired -> item.countdownText
        item.isFocusSession -> if (item.isExpired) item.dueText else remainingTimeLabel(item.remainingSeconds, null)
        item.timePhase == TimeRangePhase.Active -> remainingTimeLabel(item.remainingSeconds, null)
        item.type == ItemType.Time -> remainingTimeLabel(item.secondsUntilStart, null)
        item.type == ItemType.Date -> dateScheduleCountdown(item)
        else -> item.countdownText.ifBlank { item.leftString }
    }
    val relation = if ((item.isCalendarOccurrence || item.isFocusSession) && item.isExpired) "" else if (item.isFocusSession) item.dueText else stringResource(if (upcoming || (item.type == ItemType.Time && item.secondsUntilStart != null)) R.string.time_until_start else R.string.time_remaining)
    val expandedLabel = stringResource(if (expanded) R.string.card_details_expanded else R.string.card_details_collapsed)
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            Modifier.fillMaxWidth().then(if (interactive) Modifier.clickable(role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = expandedLabel } else Modifier)
                .padding(vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            if (board) {
                Text(countdown, style = MaterialTheme.typography.displaySmall)
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                if (item.type == ItemType.Time && relation.isNotBlank()) Text(relation,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (grid || LocalDensity.current.fontScale > 1.2f) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(countdown, style = MaterialTheme.typography.titleLarge)
                if (item.type == ItemType.Time && relation.isNotBlank()) Text(relation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.Top) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text(countdown, style = MaterialTheme.typography.titleMedium)
                        if (item.type == ItemType.Time && relation.isNotBlank()) Text(relation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (item.type == ItemType.Time) "${item.startLabel}–${item.endLabel}"
                    else if (item.type == ItemType.Date) dateScheduleCaption(item) else item.dueText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (interactive) Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            if (item.category.isNotBlank()) Text(item.category, style = MaterialTheme.typography.bodySmall, color = itemAccentColor(item.colorKey, MaterialTheme.colorScheme.primary))
        }
        AnimatedVisibility(
            visible = interactive && expanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = DetailExpandDurationMillis),
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = tween(durationMillis = DetailExpandDurationMillis)),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = DetailCollapseDurationMillis),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = tween(durationMillis = DetailCollapseDurationMillis)),
        ) {
            Column(
                // Closing content stays composed until the exit finishes; hide it from accessibility immediately.
                modifier = if (interactive && expanded) Modifier else Modifier.clearAndSetSemantics {},
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                listOf(item.startString, item.endString, item.updateInfo, item.recurrenceText, item.reminderText).filter { it.isNotBlank() }.distinct().forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    TextButton(onClick = { showTimeDetails = true }, enabled = interactive && expanded && !item.dataError,
                        modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.card_action_time_details)) }
                    TextButton(onClick = { onEditItem(item.id) }, enabled = interactive && expanded, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(if (item.isFocusSession) R.string.focus_open else R.string.card_action_edit)) }
                    PinWidgetButton(item, WidgetSource.Custom, enabled = interactive && expanded && !item.dataError)
                    if (onDuplicate != null || onSaveTemplate != null || onPin != null || onMove != null) Box {
                        TextButton(onClick = { moreActions = true }, enabled = interactive && expanded,
                            modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.schedule_more_actions)) }
                        DropdownMenu(moreActions, onDismissRequest = { moreActions = false }) {
                            if (onDuplicate != null && !item.isFocusSession && !item.dataError) DropdownMenuItem(
                                text = { Text(stringResource(R.string.schedule_duplicate)) }, onClick = { moreActions = false; onDuplicate(item.id) })
                            if (onSaveTemplate != null && !item.isFocusSession && !item.isCalendarOccurrence && !item.dataError) DropdownMenuItem(
                                text = { Text(stringResource(R.string.schedule_save_template)) }, onClick = { moreActions = false; onSaveTemplate(item.id) })
                            if (onPin != null && !item.dataError && !item.isExpired && (item.type == ItemType.Date || item.endsAtMillis != null)) DropdownMenuItem(
                                text = { Text(stringResource(if (item.isPinned) R.string.schedule_unpin else R.string.schedule_pin)) },
                                onClick = { moreActions = false; onPin(item) })
                            if (onMove != null) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.schedule_move_up)) }, onClick = { moreActions = false; onMove(item.id, -1) })
                                DropdownMenuItem(text = { Text(stringResource(R.string.schedule_move_down)) }, onClick = { moreActions = false; onMove(item.id, 1) })
                            }
                        }
                    }
                    TextButton(onClick = { showDeleteDialog = true }, enabled = interactive && expanded, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget)) { Text(stringResource(R.string.card_action_delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    if (interactive && showTimeDetails) TimeDetailSheet(item, item.title, progressDisplayMode, WidgetSource.Custom,
        onDismiss = { showTimeDetails = false })
    if (interactive && showDeleteDialog) AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text(stringResource(R.string.card_delete_title)) },
        text = { Text(stringResource(R.string.card_delete_message, item.title)) },
        confirmButton = { TextButton(onClick = { showDeleteDialog = false; onDeleteItem(item.id) }) { Text(stringResource(R.string.card_action_delete), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
    )
}
