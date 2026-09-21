package com.devidea.timeleft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.formatPercent
import com.devidea.timeleft.formatRemainingTime
import com.devidea.timeleft.preferences.UserPreferences
import com.devidea.timeleft.roundPercent
import com.devidea.timeleft.ui.theme.Spacing

/** Used inside both the period sheet and the activity opened from a widget. No independent ticker. */
@Composable
internal fun TimeDetailContent(item: AdapterItem, progressDisplayMode: String, modifier: Modifier = Modifier) {
    val facts = item.detailFacts
    if (facts?.validRange == false) {
        Text(stringResource(R.string.detail_invalid_range), modifier = modifier, style = MaterialTheme.typography.bodyLarge)
        return
    }
    val waiting = item.type == ItemType.Time && item.timePhase != TimeRangePhase.Active
    val showRuler = progressDisplayMode != UserPreferences.PROGRESS_DISPLAY_HIDDEN
    val expiredDate = item.type == ItemType.Date && item.isExpired
    val context = LocalContext.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        if (waiting) {
            Text(stringResource(if (item.timePhase == TimeRangePhase.Finished) R.string.detail_next_start else R.string.time_until_start),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (facts?.phase == TimeRangePhase.Upcoming) {
            Text(stringResource(R.string.detail_not_started), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (expiredDate) {
            Text(item.countdownText.ifBlank { item.leftString }, style = MaterialTheme.typography.displayMedium)
        } else {
            RemainingTimeText(item, hero = true, showSeconds = true, untilStart = waiting, showRelation = !waiting)
        }
        if (showRuler) {
            TimeRuler(facts?.percentElapsed ?: item.percent, item.startLabel, item.endLabel,
                glowEnabled = facts?.glowActive == true)
        }
        if (facts != null) {
            if (progressDisplayMode == UserPreferences.PROGRESS_DISPLAY_FULL) {
                val raw = facts.percentElapsed.takeIf { it.isFinite() }?.coerceIn(0f, 100f) ?: 0f
                // Avoid visually completing an active range before its real end.
                val elapsed = roundPercent(raw).let { if (facts.glowActive) it.coerceAtMost(99.9f) else it }
                Text(stringResource(R.string.detail_progress,
                    formatPercent(elapsed, context.resources.configuration.locales[0], alwaysOneDecimal = true),
                    formatPercent(100f - elapsed, context.resources.configuration.locales[0], alwaysOneDecimal = true)),
                    style = MaterialTheme.typography.bodyLarge)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (!showRuler) {
                    DetailFact(stringResource(R.string.detail_start), item.startLabel)
                    DetailFact(stringResource(R.string.detail_end), item.endLabel)
                }
                fun duration(value: Long): String = formatRemainingTime(context,
                    seconds = value.takeUnless { facts.inDays }, days = value.toInt().takeIf { facts.inDays }, showSeconds = true)
                // Today already communicates its full range on the ruler. Keep the existing
                // 23:59:59 endpoint without presenting it as an exact 24-hour duration sum.
                if (item.type != null || facts.inDays) DetailFact(stringResource(R.string.detail_total), duration(facts.total))
                DetailFact(stringResource(R.string.detail_elapsed), duration(facts.elapsed))
                if (facts.inDays) {
                    Text(stringResource(if (facts.includesToday) R.string.detail_calendar_basis else R.string.period_days_explanation),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (item.remainingDays != null && !item.isExpired) {
            Text(stringResource(R.string.period_days_explanation), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val inDays = item.remainingDays != null
        val remaining = when {
            expiredDate -> 0L
            waiting -> facts?.secondsUntilStart ?: item.secondsUntilStart ?: 0L
            facts?.phase != null && facts.phase != TimeRangePhase.Active -> 0L
            inDays -> item.remainingDays?.toLong() ?: 0L
            else -> facts?.secondsLeft ?: item.remainingSeconds ?: 0L
        }
        val identity = "${item.id}/${item.startLabel}/${item.endLabel}/${facts?.phase}/$inDays"
        TimeStoriesSection(identity, remaining, inDays)
    }
}

@Composable
private fun DetailFact(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Text(label, modifier = Modifier.weight(1f).alignByBaseline(),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(2f).alignByBaseline(),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), textAlign = TextAlign.End)
    }
}
