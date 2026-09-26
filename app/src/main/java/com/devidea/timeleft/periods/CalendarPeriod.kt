package com.devidea.timeleft.periods

import android.content.Context
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.TimeDetailFacts
import com.devidea.timeleft.TimeDetailRange
import com.devidea.timeleft.calc.TimeRangePhase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

internal enum class CalendarPeriod(val title: Int) {
    Week(R.string.period_week), Quarter(R.string.period_quarter);

    fun bounds(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
        Week -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusDays(6) }
        Quarter -> LocalDate.of(today.year, ((today.monthValue - 1) / 3) * 3 + 1, 1).let { it to it.plusMonths(3).minusDays(1) }
    }
}

/** Same calendar convention as Month/Year: today's date is elapsed; remaining days exclude it. */
internal fun calendarPeriodItem(context: Context, period: CalendarPeriod, today: LocalDate = LocalDate.now()): AdapterItem {
    val (start, end) = period.bounds(today)
    val total = ChronoUnit.DAYS.between(start, end) + 1
    val elapsed = ChronoUnit.DAYS.between(start, today) + 1
    val remaining = ChronoUnit.DAYS.between(today, end).toInt()
    val percent = elapsed.toFloat() / total * 100f
    return AdapterItem(title = context.getString(period.title), remainingDays = remaining,
        startLabel = start.toString(), endLabel = end.toString(), percent = percent,
        leftString = context.getString(R.string.home_days_left, remaining),
        detailFacts = TimeDetailFacts(percent, elapsed, total, TimeRangePhase.Active,
            inDays = true, includesToday = true, range = TimeDetailRange.Calendar(start, end, includesFirstDay = true)))
}
