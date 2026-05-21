package com.devidea.timeleft.calc

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object TimeProgressCalculator {

    const val UPDATE_FLAG_DAY = 1
    const val UPDATE_FLAG_MONTH = 2

    private const val SECONDS_PER_DAY = 86_400f
    private val END_OF_DAY: LocalTime = LocalTime.of(23, 59, 59)

    fun dayProgress(now: LocalTime): DayProgress {
        val elapsed = Duration.between(LocalTime.MIDNIGHT, now).seconds.toFloat()
        return DayProgress(
            percentElapsed = elapsed / SECONDS_PER_DAY * 100f,
            durationLeft = Duration.between(now, END_OF_DAY)
        )
    }

    fun yearProgress(today: LocalDate): DateProgress {
        val total = today.lengthOfYear()
        return DateProgress(
            percentElapsed = today.dayOfYear.toFloat() / total * 100f,
            daysLeft = total - today.dayOfYear
        )
    }

    fun monthProgress(today: LocalDate): DateProgress {
        val total = today.lengthOfMonth()
        return DateProgress(
            percentElapsed = today.dayOfMonth.toFloat() / total * 100f,
            daysLeft = total - today.dayOfMonth
        )
    }

    fun customTimeProgress(start: LocalTime, end: LocalTime, now: LocalTime): CustomTimeProgress {
        if (!(now.isAfter(start) && now.isBefore(end))) return CustomTimeProgress.Idle
        val total = Duration.between(start, end).seconds.toFloat()
        val elapsed = Duration.between(start, now).seconds.toFloat()
        return CustomTimeProgress.Active(
            percentElapsed = elapsed / total * 100f,
            durationLeft = Duration.between(now, end)
        )
    }

    fun customDateProgress(start: LocalDate, end: LocalDate, today: LocalDate): CustomDateProgress {
        val total = ChronoUnit.DAYS.between(start, end).toInt()
        val elapsed = ChronoUnit.DAYS.between(start, today).toInt()
        val left = ChronoUnit.DAYS.between(today, end).toInt()
        return CustomDateProgress(
            percentElapsed = if (total == 0) Float.POSITIVE_INFINITY
            else elapsed.toFloat() / total * 100f,
            daysLeft = left,
            daysBetween = total
        )
    }

    // Returns the next (start, end) window when today has passed currentEnd.
    // Null when no shift applies (no recurrence flag, or today still within window).
    //
    // Preserves two known quirks from the original implementation, locked in by tests:
    //   1. Threshold uses `>` not `>=`, so updateRate == lengthOfMonth(next) skips ahead one month.
    //   2. When the next month cannot hold updateRate, year is taken from plusMonths(1) while
    //      the month is taken from plusMonths(2). This produces a past date at Dec→Jan boundaries.
    fun nextRecurrence(
        currentEnd: LocalDate,
        today: LocalDate,
        updateFlag: Int,
        updateRate: Int
    ): RecurrenceShift? {
        if (!today.isAfter(currentEnd)) return null
        return when (updateFlag) {
            UPDATE_FLAG_DAY -> RecurrenceShift(
                newStart = currentEnd,
                newEnd = currentEnd.plusDays(updateRate.toLong())
            )

            UPDATE_FLAG_MONTH -> {
                val plus1 = currentEnd.plusMonths(1)
                val plus2 = currentEnd.plusMonths(2)
                val monthValue =
                    if (plus1.lengthOfMonth() > updateRate) plus1.monthValue else plus2.monthValue
                RecurrenceShift(
                    newStart = currentEnd,
                    newEnd = LocalDate.of(plus1.year, monthValue, updateRate)
                )
            }

            else -> null
        }
    }
}
