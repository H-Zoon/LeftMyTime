package com.devidea.timeleft.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.ui.components.remainingTimeLabel

/** Presentation only: retain the stored day difference and widget D-day labels. */
@Composable
internal fun dateScheduleCountdown(item: AdapterItem): String {
    val days = item.remainingDays
    return when {
        days == null || item.detailFacts?.validRange == false -> stringResource(R.string.home_date_unavailable)
        days < 0 -> {
            val elapsedDays = -days.toLong()
            pluralStringResource(R.plurals.home_days_elapsed, elapsedDays.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), elapsedDays)
        }
        days == 0 -> stringResource(R.string.home_date_ends_today)
        else -> stringResource(R.string.time_remaining_description, remainingTimeLabel(null, days))
    }
}

@Composable
internal fun dateScheduleCaption(item: AdapterItem): String =
    if (item.detailFacts?.phase == TimeRangePhase.Upcoming && item.detailFacts.validRange) {
        stringResource(R.string.home_date_upcoming_until, item.startLabel, item.endLabel)
    } else {
        item.dueText.ifBlank { stringResource(R.string.home_until_date, item.endLabel) }
    }
