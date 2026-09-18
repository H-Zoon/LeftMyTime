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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalContext
import com.devidea.timeleft.formatRemainingTime
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.Spacing

@Composable
internal fun remainingTimeLabel(seconds: Long?, days: Int?, fallback: String = ""): String =
    formatRemainingTime(LocalContext.current, seconds, days, fallback)

/** Units stay attached to their values; groups wrap instead of shrinking accessible text. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun RemainingTimeText(item: AdapterItem, hero: Boolean, modifier: Modifier = Modifier) {
    val seconds = item.remainingSeconds
    val days = item.remainingDays
    val label = remainingTimeLabel(seconds, days, item.countdownText.ifBlank { item.leftString })
    val description = stringResource(R.string.time_remaining_description, label)
    val groups = when {
        days != null -> listOf(days.coerceAtLeast(0).toString() to pluralStringResource(R.plurals.time_unit_days, days.coerceAtLeast(0)))
        seconds == null || seconds < 60 -> emptyList()
        seconds < 3600 -> listOf((seconds / 60).toString() to stringResource(R.string.time_unit_minutes))
        else -> listOf(
            (seconds / 3600).toString() to stringResource(R.string.time_unit_hours),
            (seconds % 3600 / 60).toString() to stringResource(R.string.time_unit_minutes)
        )
    }
    FlowRow(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (groups.isEmpty()) {
            Text(description, style = if (hero) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge)
        } else {
            groups.forEachIndexed { index, (value, unit) ->
                val numberStyle = when {
                    !hero -> MaterialTheme.typography.displaySmall
                    groups.size > 1 || value.length > 2 -> MaterialTheme.typography.displayMedium
                    else -> MaterialTheme.typography.displayLarge
                }
                val unitLabel = if (index == groups.lastIndex) unit + " " + stringResource(R.string.time_remaining) else unit
                val unitStyle = if (hero) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
                if (LocalDensity.current.fontScale > 1.5f) {
                    Column {
                        Text(value, style = numberStyle, maxLines = 1)
                        Text(unitLabel, style = unitStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Row {
                        Text(value, style = numberStyle, maxLines = 1, modifier = Modifier.alignByBaseline())
                        Text(unitLabel, style = unitStyle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alignByBaseline().padding(start = Spacing.xs))
                    }
                }
            }
        }
    }
}
