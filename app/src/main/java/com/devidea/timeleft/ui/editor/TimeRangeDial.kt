package com.devidea.timeleft.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.Spacing
import java.time.Duration
import java.time.LocalTime
import com.devidea.timeleft.formatClockTime
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

/** Optional direct manipulation; primary input uses the start/end picker fields. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TimeRangeDial(
    startTime: LocalTime,
    endTime: LocalTime,
    onRangeChange: (LocalTime, LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val description = stringResource(R.string.editor_time_range_accessibility, formatClockTime(context, startTime), formatClockTime(context, endTime), rangeDurationText(startTime, endTime))
    val start = startTime.toSecondOfDay() / 60f
    val end = endTime.toSecondOfDay() / 60f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(rangeDurationText(startTime, endTime), style = MaterialTheme.typography.bodyMedium)
        RangeSlider(
            value = start.coerceAtMost(end)..end,
            onValueChange = { range ->
                val from = ((range.start / 5).roundToInt() * 5).coerceIn(0, 1438)
                val to = ((range.endInclusive / 5).roundToInt() * 5).coerceIn(1, 1439)
                if (to > from) onRangeChange(LocalTime.ofSecondOfDay(from * 60L), LocalTime.ofSecondOfDay(to * 60L))
            },
            valueRange = 0f..1439f,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = description }
        )
        Text(stringResource(R.string.editor_time_range_drag_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun rangeDurationText(startTime: LocalTime, endTime: LocalTime): String {
    val minutes = Duration.between(startTime, endTime).toMinutes().coerceAtLeast(0)
    return when {
        minutes >= 60 && minutes % 60 > 0 -> stringResource(R.string.editor_time_range_hours_minutes, minutes / 60, minutes % 60)
        minutes >= 60 -> stringResource(R.string.editor_time_range_hours, minutes / 60)
        else -> stringResource(R.string.editor_time_range_minutes, minutes)
    }
}
