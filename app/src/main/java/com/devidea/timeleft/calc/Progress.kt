package com.devidea.timeleft.calc

import java.time.Duration
import java.time.LocalDate

data class DayProgress(
    val percentElapsed: Float,
    val durationLeft: Duration
)

data class DateProgress(
    val percentElapsed: Float,
    val daysLeft: Int
)

sealed class CustomTimeProgress {
    data class Active(
        val percentElapsed: Float,
        val durationLeft: Duration
    ) : CustomTimeProgress()

    object Idle : CustomTimeProgress()
}

data class CustomDateProgress(
    val percentElapsed: Float,
    val daysLeft: Int,
    val daysBetween: Int
)

data class RecurrenceShift(
    val newStart: LocalDate,
    val newEnd: LocalDate
)
