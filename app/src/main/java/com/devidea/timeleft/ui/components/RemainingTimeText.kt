package com.devidea.timeleft.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.RemainingTimeGroup
import com.devidea.timeleft.formatRemainingTime
import com.devidea.timeleft.remainingTimeGroups
import com.devidea.timeleft.ui.theme.Spacing
import java.text.NumberFormat

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
    presentationValue: Long? = null,
    reserveValue: Long? = null,
    unitReferenceSeconds: Long? = null,
    totalSeconds: Boolean = false,
) {
    val seconds = if (untilStart) {
        if (showSeconds) item.detailFacts?.secondsUntilStart ?: item.secondsUntilStart else item.secondsUntilStart
    } else if (showSeconds) item.detailFacts?.secondsLeft ?: item.remainingSeconds else item.remainingSeconds
    val days = item.remainingDays
    val context = LocalContext.current
    val numberFormat = NumberFormat.getIntegerInstance(context.resources.configuration.locales[0])
    val label = if (totalSeconds && seconds != null && days == null) {
        stringResource(R.string.detail_total_seconds_description, numberFormat.format(seconds.coerceAtLeast(0)))
    } else remainingTimeLabel(seconds, days, item.countdownText.ifBlank { item.leftString }, showSeconds)
    val relationRes = if (untilStart) R.string.time_until_start_description else R.string.time_remaining_description
    val description = stringResource(relationRes, label)
    fun displayGroups(value: Long?): List<RemainingTimeGroup> {
        val displayed = (value ?: days?.toLong() ?: seconds)?.coerceAtLeast(0)
            ?: return remainingTimeGroups(context, seconds, days, showSeconds)
        if (days != null) return remainingTimeGroups(context, null, displayed.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        if (totalSeconds) return listOf(RemainingTimeGroup(numberFormat.format(displayed), context.getString(R.string.time_unit_seconds)))
        if (!showSeconds || seconds == null || (value == null && unitReferenceSeconds == null))
            return remainingTimeGroups(context, seconds, days, showSeconds)
        // A detail keeps its full range's units while open, including leading zeroes.
        // This avoids changing rows at 1 h / 1 min and enormous intermediate seconds.
        val reference = unitReferenceSeconds ?: seconds
        return buildList {
            var rest = displayed
            if (reference >= 3_600) {
                add(RemainingTimeGroup((rest / 3_600).toString(), context.getString(R.string.time_unit_hours)))
                rest %= 3_600
            }
            if (reference >= 60) {
                val minutes = (rest / 60).toString().let { if (reference >= 3_600) it.padStart(2, '0') else it }
                add(RemainingTimeGroup(minutes, context.getString(R.string.time_unit_minutes)))
                rest %= 60
            }
            add(RemainingTimeGroup(rest.toString().padStart(2, '0'), context.getString(R.string.time_unit_seconds), isSeconds = true))
        }
    }
    val groups = displayGroups(presentationValue)
    val reserved = reserveValue?.let(::displayGroups)
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
                val widestValue = reserved?.getOrNull(index)?.number?.takeIf { it.length >= value.length } ?: value
                val secondarySeconds = group.isSeconds && groups.size > 1
                val numberStyle = when {
                    secondarySeconds && compact -> MaterialTheme.typography.bodyMedium
                    secondarySeconds -> MaterialTheme.typography.headlineSmall
                    compact -> MaterialTheme.typography.titleLarge
                    !hero -> MaterialTheme.typography.displaySmall
                    primaryCount > 1 || widestValue.length > 2 -> MaterialTheme.typography.displayMedium
                    else -> MaterialTheme.typography.displayLarge
                }.copy(fontFeatureSettings = "tnum")
                val relation = stringResource(if (untilStart) R.string.time_until_start else R.string.time_remaining)
                val unitLabel = if (!compact && showRelation && index == groups.lastIndex) "$unit $relation" else unit
                val unitStyle = if (compact) MaterialTheme.typography.bodySmall else if (hero) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
                @Composable fun Number(modifier: Modifier = Modifier) {
                    Box(modifier, contentAlignment = Alignment.CenterEnd) {
                        if (reserved != null) Text(widestValue, style = numberStyle,
                            modifier = Modifier.graphicsLayer { alpha = 0f })
                        Text(value, style = numberStyle)
                    }
                }
                if (!compact && LocalDensity.current.fontScale > 1.5f) {
                    // Share the number's first baseline across groups, including smaller seconds.
                    Column(Modifier.alignByBaseline()) {
                        Number()
                        Text(unitLabel, style = unitStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Row(Modifier.alignByBaseline()) {
                        Number(Modifier.alignByBaseline())
                        Text(unitLabel, style = unitStyle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alignByBaseline().padding(start = Spacing.xs))
                    }
                }
            }
        }
    }
}
