package com.devidea.timeleft.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.R
import com.devidea.timeleft.database.ItemType
import com.devidea.timeleft.config.UserPreferences
import com.devidea.timeleft.ui.components.RemainingTimeText
import com.devidea.timeleft.ui.components.TimeRuler
import com.devidea.timeleft.ui.components.TimeDetailSheet
import com.devidea.timeleft.ui.components.TimeHeadline
import com.devidea.timeleft.ui.theme.LocalTimeLayout
import com.devidea.timeleft.ui.theme.TimeLayout
import com.devidea.timeleft.ui.theme.LayoutTokens
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.widget.WidgetSource

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun NextCountdownHero(
    item: TimeDisplayItem,
    progressDisplayMode: String,
    onEditItem: (Int) -> Unit,
    interactive: Boolean = true,
    supportingAction: (@Composable () -> Unit)? = null,
) {
    val waiting = item.type == ItemType.Time && !item.isFocusSession && item.secondsUntilStart != null
    val board = LocalTimeLayout.current == TimeLayout.TimeBoard
    var showTimeDetails by rememberSaveable(item.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = Spacing.m, bottom = Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (item.isPinned) Text(stringResource(R.string.schedule_pinned), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        Text(
            when {
                item.isFocusSession -> item.dueText
                waiting -> stringResource(R.string.time_until_start)
                item.type == ItemType.Date -> dateScheduleCaption(item)
                else -> stringResource(R.string.home_active_until, item.endLabel)
            },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TimeHeadline(label = { Text(
            item.title, style = MaterialTheme.typography.headlineSmall,
            textAlign = if (board) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth().heightIn(min = LayoutTokens.MinTouchTarget)
                .then(if (interactive) Modifier.clickable(role = Role.Button,
                    onClickLabel = stringResource(R.string.card_action_edit)) { onEditItem(item.id) } else Modifier)
        ) }, value = {
        if (item.type == ItemType.Date && item.remainingDays == 0) {
            Text(stringResource(R.string.home_date_ends_today), style = MaterialTheme.typography.displaySmall)
        } else {
            RemainingTimeText(item, hero = true, showSeconds = true, untilStart = waiting)
        }
        })
        if (progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN) {
            TimeRuler(item.detailFacts?.percentElapsed ?: item.percent, item.startLabel, item.endLabel,
                currentLabel = item.currentLabel, glowEnabled = item.detailFacts?.glowActive == true)
        }
        if (interactive) FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            TextButton(onClick = { showTimeDetails = true }, modifier = Modifier.heightIn(min = LayoutTokens.MinTouchTarget),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                contentPadding = PaddingValues(vertical = Spacing.s)) { Text(stringResource(R.string.card_action_time_details)) }
            supportingAction?.invoke()
        }
    }
    if (interactive && showTimeDetails) TimeDetailSheet(item, item.title, progressDisplayMode, WidgetSource.Custom,
        onDismiss = { showTimeDetails = false })
}
