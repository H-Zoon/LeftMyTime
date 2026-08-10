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
            percentElapsed = if (total == 0) 100f else elapsed.toFloat() / total * 100f,
            daysLeft = left,
            daysBetween = total
        )
    }

    // Returns the next (start, end) window when today has passed currentEnd.
    // Null when no shift applies (no recurrence flag, or today still within window).
    //
    // Both modes preserve the original cycle length (currentEnd - currentStart).
    //
    // Day: a fixed gap of `updateRate` days follows the previous end before the new
    //      cycle starts — newStart = currentEnd + updateRate.
    //
    // Month: the new cycle starts on day `updateRate` of the earliest month strictly
    //        after currentEnd that contains such a day (skipping months whose length
    //        is shorter than updateRate, e.g. Feb when updateRate is 30 or 31).
    fun nextRecurrence(
        currentStart: LocalDate,
        currentEnd: LocalDate,
        today: LocalDate,
        updateFlag: RecurrenceMode,
        updateRate: Int
    ): RecurrenceShift? {
        if (!today.isAfter(currentEnd) || currentEnd.isBefore(currentStart)) return null
        val duration = ChronoUnit.DAYS.between(currentStart, currentEnd)
        return when (updateFlag) {
            RecurrenceMode.Day -> {
                if (updateRate < 1) return null
                val newStart = currentEnd.plusDays(updateRate.toLong())
                RecurrenceShift(
                    newStart = newStart,
                    newEnd = newStart.plusDays(duration)
                )
            }

            RecurrenceMode.Month -> {
                if (updateRate !in 1..31) return null
                var candidate = currentEnd.plusDays(1)
                while (candidate.lengthOfMonth() < updateRate || candidate.dayOfMonth > updateRate) {
                    candidate = candidate.plusMonths(1).withDayOfMonth(1)
                }
                val newStart = candidate.withDayOfMonth(updateRate)
                RecurrenceShift(
                    newStart = newStart,
                    newEnd = newStart.plusDays(duration)
                )
            }

            RecurrenceMode.None, RecurrenceMode.TimeRange -> null
        }
    }

    // Advances every expired cycle and returns the first recurrence whose end is
    // today or later. The caller can persist this final window with a single write.
    fun catchUpRecurrence(
        currentStart: LocalDate,
        currentEnd: LocalDate,
        today: LocalDate,
        updateFlag: RecurrenceMode,
        updateRate: Int
    ): RecurrenceShift? {
        if (!today.isAfter(currentEnd) || currentEnd.isBefore(currentStart)) return null

        val duration = ChronoUnit.DAYS.between(currentStart, currentEnd)
        return when (updateFlag) {
            RecurrenceMode.Day -> {
                if (updateRate < 1) return null

                val cycleStep = duration + updateRate.toLong()
                val daysPastEnd = ChronoUnit.DAYS.between(currentEnd, today)
                val expiredCycles = (daysPastEnd - 1) / cycleStep + 1
                val newEnd = currentEnd.plusDays(expiredCycles * cycleStep)
                RecurrenceShift(
                    newStart = newEnd.minusDays(duration),
                    newEnd = newEnd
                )
            }

            RecurrenceMode.Month -> {
                if (updateRate !in 1..31) return null

                var start = currentStart
                var end = currentEnd
                while (today.isAfter(end)) {
                    val next = nextRecurrence(
                        currentStart = start,
                        currentEnd = end,
                        today = today,
                        updateFlag = updateFlag,
                        updateRate = updateRate
                    ) ?: return null

                    if (!next.newEnd.isAfter(end)) return null
                    start = next.newStart
                    end = next.newEnd
                }
                RecurrenceShift(newStart = start, newEnd = end)
            }

            RecurrenceMode.None, RecurrenceMode.TimeRange -> null
        }
    }
}
