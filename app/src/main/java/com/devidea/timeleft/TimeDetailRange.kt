package com.devidea.timeleft

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/** Original bounds for read-only exploration; never parsed from translated UI labels. */
sealed interface TimeDetailRange {
    data class Clock(val start: LocalTime, val end: LocalTime, val occurrence: LocalDate,
        val durationSeconds: Long = Duration.between(start, end).seconds,
        val startEpochSecond: Long? = null) : TimeDetailRange
    data class Calendar(val start: LocalDate, val end: LocalDate, val includesFirstDay: Boolean = false) : TimeDetailRange

    val minimum: Long
        get() = if (this is Calendar && includesFirstDay) 1L else 0L
    val maximum: Long
        get() = when (this) {
            is Clock -> durationSeconds.coerceAtLeast(0)
            is Calendar -> (ChronoUnit.DAYS.between(start, end) + minimum).coerceAtLeast(0)
        }
    val step: Long
        get() = if (this is Clock && maximum >= 60) 60L else 1L
    val hapticStep: Long
        get() = when {
            this is Calendar -> 1L
            maximum >= 6 * 3_600 -> 900L
            maximum >= 300 -> 300L
            maximum >= 60 -> 60L
            else -> 5L
        }

    fun clamp(offset: Long): Long = offset.coerceIn(minimum.coerceAtMost(maximum), maximum)
    fun fraction(offset: Long): Float = if (maximum > 0) clamp(offset).toFloat() / maximum else 0f
    fun remaining(offset: Long): Long = maximum - clamp(offset)
    fun offsetAt(fraction: Float): Long {
        if (!fraction.isFinite()) return minimum.coerceAtMost(maximum)
        val raw = fraction.coerceIn(0f, 1f).toDouble() * maximum
        // Preserve both endpoints even when the final minute is partial.
        val lastRegular = maximum / step * step
        val beforeEnd = if (lastRegular == maximum) maximum - step else lastRegular
        if (raw >= (beforeEnd + maximum) / 2.0) return maximum
        return clamp((raw / step).roundToLong() * step)
    }
}
