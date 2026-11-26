package com.devidea.timeleft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.ui.TimeDetailRange
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.ItemType
import com.devidea.timeleft.ui.formatPercent
import com.devidea.timeleft.ui.formatRemainingTime
import com.devidea.timeleft.config.UserPreferences
import com.devidea.timeleft.ui.roundPercent
import com.devidea.timeleft.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Used inside both the period sheet and the activity opened from a widget. No independent ticker. */
@Composable
internal fun TimeDetailContent(
    item: TimeDisplayItem,
    progressDisplayMode: String,
    modifier: Modifier = Modifier,
    calendarPolicy: String? = null,
) {
    // State survives ticks/rotation, and belongs to this item and actual period only.
    val identity = "${item.type}/${item.id}/${item.detailFacts?.range ?: "${item.startLabel}/${item.endLabel}"}"
    key(identity) { TimeDetailBody(item, progressDisplayMode, modifier, calendarPolicy) }
}

@Composable
private fun TimeDetailBody(item: TimeDisplayItem, progressDisplayMode: String, modifier: Modifier, calendarPolicy: String?) {
    val facts = item.detailFacts
    if (facts?.validRange == false) {
        Text(stringResource(R.string.detail_invalid_range), modifier = modifier, style = MaterialTheme.typography.bodyLarge)
        return
    }
    val waiting = !item.isFocusSession && item.type == ItemType.Time && item.secondsUntilStart != null
    val showRuler = progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN
    val expiredDate = item.type == ItemType.Date && item.isExpired
    val context = LocalContext.current
    var infoExpanded by rememberSaveable { mutableStateOf(false) }
    val calendarRange = facts?.range as? TimeDetailRange.Calendar
    val calendarDay = if (facts != null && facts.inDays && facts.includesToday && calendarRange != null) {
        facts.elapsed.coerceIn(1L, facts.total.coerceAtLeast(1L))
    } else null
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        if (item.isCalendarOccurrence) Text(stringResource(R.string.calendar_copy_hint), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (item.isFocusSession) {
            Text(item.dueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.focus_elapsed_basis), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (waiting) {
            Text(stringResource(if (item.timePhase == TimeRangePhase.Finished) R.string.detail_next_start else R.string.time_until_start),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (!item.isFocusSession && facts?.phase == TimeRangePhase.Upcoming) {
            Text(stringResource(R.string.detail_not_started), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (item.isFocusSession && item.isExpired) {
            Text(stringResource(R.string.focus_recorded_time), style = MaterialTheme.typography.bodyMedium)
            val elapsed = facts?.elapsed ?: 0L
            RemainingTimeText(item.copy(remainingSeconds = elapsed, detailFacts = facts?.copy(secondsLeft = elapsed)),
                hero = true, showSeconds = true, showRelation = false)
        } else if (expiredDate || item.isCalendarOccurrence && item.isExpired) {
            Text(item.countdownText.ifBlank { item.leftString }, style = MaterialTheme.typography.displayMedium)
        } else {
            InteractiveRemainingTime(item, waiting)
        }
        if (showRuler && !(item.isFocusSession && item.isExpired)) {
            val range = facts?.range
            if (facts != null && range != null && range.maximum > range.minimum && facts.phase == TimeRangePhase.Active && !waiting && !expiredDate) {
                key(facts.phase) { InteractiveTimeRuler(item, range) }
            } else {
                TimeRuler(facts?.percentElapsed ?: item.percent, item.startLabel, item.endLabel,
                    glowEnabled = facts?.glowActive == true)
            }
        }
        if (facts != null) {
            fun duration(value: Long): String = formatRemainingTime(context,
                seconds = value.takeUnless { facts.inDays }, days = value.toInt().takeIf { facts.inDays }, showSeconds = true)
            if (calendarDay != null && calendarRange != null) {
                val date = calendarRange.start.plusDays(calendarDay - 1).format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(context.resources.configuration.locales[0]))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.detail_calendar_position, date, calendarDay),
                    style = MaterialTheme.typography.bodyMedium)
            }
            val hasTotal = item.type != null || facts.inDays
            TimeDetailDisclosure(
                title = stringResource(R.string.detail_period_info),
                summary = if (hasTotal) stringResource(R.string.detail_total_summary, duration(facts.total))
                    else stringResource(R.string.detail_clock_info_summary),
                expanded = infoExpanded,
                onToggle = { infoExpanded = !infoExpanded },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    if (progressDisplayMode == UserPreferences.PROGRESS_DISPLAY_FULL) {
                        val raw = facts.percentElapsed.takeIf { it.isFinite() }?.coerceIn(0f, 100f) ?: 0f
                        // Avoid visually completing an active range before its real end.
                        val elapsed = roundPercent(raw).let { if (facts.glowActive) it.coerceAtMost(99.9f) else it }
                        val label = when {
                            item.isFocusSession && item.isExpired -> R.string.focus_recorded_progress
                            calendarDay != null -> R.string.detail_calendar_progress
                            else -> R.string.detail_progress
                        }
                        Text(stringResource(label,
                            formatPercent(elapsed, context.resources.configuration.locales[0], alwaysOneDecimal = true),
                            formatPercent(100f - elapsed, context.resources.configuration.locales[0], alwaysOneDecimal = true)),
                            style = MaterialTheme.typography.bodyLarge)
                    }
                    DetailFact(stringResource(R.string.detail_start), item.startLabel)
                    DetailFact(stringResource(R.string.detail_end), item.endLabel)
                    // Today's 23:59:59 endpoint is not presented as an exact 24-hour sum.
                    if (hasTotal) DetailFact(stringResource(R.string.detail_total), duration(facts.total))
                    if (calendarDay != null) {
                        DetailFact(stringResource(R.string.detail_calendar_day),
                            stringResource(R.string.detail_calendar_day_value, calendarDay))
                    } else DetailFact(stringResource(R.string.detail_elapsed), duration(facts.elapsed))
                    if (facts.inDays) {
                        Text(stringResource(if (facts.includesToday) R.string.detail_calendar_basis else R.string.period_days_explanation),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    calendarPolicy?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else if (item.remainingDays != null && !item.isExpired) {
            Text(stringResource(R.string.period_days_explanation), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailFact(label: String, value: String) {
    if (value.isBlank()) return
    BoxWithConstraints(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.2f) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"))
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
                Text(label, modifier = Modifier.weight(1f).alignByBaseline(),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, modifier = Modifier.weight(2f).alignByBaseline(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), textAlign = TextAlign.End)
            }
        }
    }
}
