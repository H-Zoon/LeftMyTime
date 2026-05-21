package com.devidea.timeleft.calc

import com.devidea.timeleft.database.itemdata.RecurrenceMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object TimeProgressCalculator {

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
    // For UPDATE_FLAG_MONTH, walks forward month-by-month until a month long enough
    // to hold `updateRate` (day of month) is found — year and month are always taken
    // from the same candidate to keep the boundary correct (incl. Dec→Jan).
    fun nextRecurrence(
        currentEnd: LocalDate,
        today: LocalDate,
        updateFlag: RecurrenceMode,
        updateRate: Int
    ): RecurrenceShift? {
        if (!today.isAfter(currentEnd)) return null
        return when (updateFlag) {
            RecurrenceMode.Day -> RecurrenceShift(
                newStart = currentEnd,
                newEnd = currentEnd.plusDays(updateRate.toLong())
            )

            RecurrenceMode.Month -> {
                var candidate = currentEnd.plusMonths(1)
                while (candidate.lengthOfMonth() < updateRate) {
                    candidate = candidate.plusMonths(1)
                }
                RecurrenceShift(
                    newStart = currentEnd,
                    newEnd = LocalDate.of(candidate.year, candidate.monthValue, updateRate)
                )
            }

            RecurrenceMode.None, RecurrenceMode.TimeRange -> null
        }
    }
}
