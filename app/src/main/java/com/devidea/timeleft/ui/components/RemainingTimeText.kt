package com.devidea.timeleft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.devidea.timeleft.formatRemainingTime
import com.devidea.timeleft.remainingTimeGroups
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.Spacing

@Composable
internal fun remainingTimeLabel(seconds: Long?, days: Int?, fallback: String = "", showSeconds: Boolean = false): String =
    formatRemainingTime(LocalContext.current, seconds, days, fallback, showSeconds)

/** Units stay attached to their values; groups wrap instead of shrinking accessible text. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun RemainingTimeText(
    item: AdapterItem,
    hero: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showSeconds: Boolean = false,
    untilStart: Boolean = false,
    showRelation: Boolean = true,
) {
    val seconds = if (untilStart) {
        if (showSeconds) item.detailFacts?.secondsUntilStart ?: item.secondsUntilStart else item.secondsUntilStart
    } else if (showSeconds) item.detailFacts?.secondsLeft ?: item.remainingSeconds else item.remainingSeconds
    val days = item.remainingDays
    val label = remainingTimeLabel(seconds, days, item.countdownText.ifBlank { item.leftString }, showSeconds)
    val relationRes = if (untilStart) R.string.time_until_start_description else R.string.time_remaining_description
    val description = stringResource(relationRes, label)
    val groups = remainingTimeGroups(LocalContext.current, seconds, days, showSeconds)
    val primaryCount = groups.count { !it.isSeconds }.coerceAtLeast(1)
    FlowRow(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(if (compact) Spacing.xs else Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (groups.isEmpty()) {
            Text(description, style = if (hero) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge)
        } else {
            groups.forEachIndexed { index, group ->
                val (value, unit) = group
                val secondarySeconds = group.isSeconds && groups.size > 1
                val numberStyle = when {
                    secondarySeconds && compact -> MaterialTheme.typography.bodyMedium
                    secondarySeconds -> MaterialTheme.typography.headlineSmall
                    compact -> MaterialTheme.typography.titleLarge
                    !hero -> MaterialTheme.typography.displaySmall
                    primaryCount > 1 || value.length > 2 -> MaterialTheme.typography.displayMedium
                    else -> MaterialTheme.typography.displayLarge
                }.copy(fontFeatureSettings = "tnum")
                val relation = stringResource(if (untilStart) R.string.time_until_start else R.string.time_remaining)
                val unitLabel = if (!compact && showRelation && index == groups.lastIndex) "$unit $relation" else unit
                val unitStyle = if (compact) MaterialTheme.typography.bodySmall else if (hero) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
                if (!compact && LocalDensity.current.fontScale > 1.5f) {
                    // Share the number's first baseline across groups, including smaller seconds.
                    Column(Modifier.alignByBaseline()) {
                        Text(value, style = numberStyle, maxLines = 1)
                        Text(unitLabel, style = unitStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Row(Modifier.alignByBaseline()) {
                        Text(value, style = numberStyle, maxLines = 1, modifier = Modifier.alignByBaseline())
                        Text(unitLabel, style = unitStyle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alignByBaseline().padding(start = Spacing.xs))
                    }
                }
            }
        }
    }
}
