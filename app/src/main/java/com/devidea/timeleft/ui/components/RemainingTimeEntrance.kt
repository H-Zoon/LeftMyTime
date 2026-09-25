package com.devidea.timeleft.ui.components

import com.devidea.timeleft.ui.theme.Motion
import kotlin.math.ceil

/** Presentation only. Every sample is a valid duration between the target and original maximum. */
internal class RemainingTimeEntrance(
    maximum: Long,
    target: Long,
    private val inDays: Boolean,
) {
    val maximum = maximum.coerceAtLeast(0L)
    val target = target.coerceIn(0L, this.maximum)
    private val clockStart = if (inDays) this.maximum else descendingClockStart(this.maximum, this.target)
    private val startClock = ClockUnits(clockStart)
    private val targetClock = ClockUnits(this.target)

    fun valueAt(fraction: Float): Long {
        val progress = fraction.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 1f
        if (progress <= 0f) return maximum
        if (progress >= 1f) return target
        // With no intermediate borrowed value, count the duration gap directly.
        if (inDays || clockStart == target) return target + stepsLeft(maximum - target, progress)

        val borrowFraction = if (clockStart < maximum) Motion.DetailClockBorrowFraction else 0f
        if (progress < borrowFraction) {
            val eased = Motion.DetailEntranceEasing.transform(progress / borrowFraction)
            return clockStart + ceil((maximum - clockStart).toDouble() * (1.0 - eased)).toLong()
        }
        val unitProgress = ((progress - borrowFraction) / (1f - borrowFraction)).coerceIn(0f, 1f)
        // The normalized start is >= target in EVERY unit. Each component only decreases;
        // recombining once keeps the duration valid without cycling minutes/seconds repeatedly.
        val hours = targetClock.hours + stepsLeft(startClock.hours - targetClock.hours, unitProgress)
        val minutes = targetClock.minutes + stepsLeft(startClock.minutes - targetClock.minutes, unitProgress)
        val seconds = targetClock.seconds + stepsLeft(startClock.seconds - targetClock.seconds, unitProgress)
        return (hours * 3_600 + minutes * 60 + seconds).coerceIn(target, maximum)
    }

    /** Bring a large gap close quickly, then show the same six-step tail regardless of its size. */
    private fun stepsLeft(gap: Long, progress: Float): Long {
        if (gap <= 0L || progress >= 1f) return 0L
        if (progress <= 0f) return gap
        val tail = minOf(gap, Motion.DetailSettleSteps)
        val approach = if (gap > tail) Motion.DetailApproachFraction else 0f
        if (progress < approach) {
            val eased = Motion.DetailEntranceEasing.transform(progress / approach)
            return tail + ceil((gap - tail).toDouble() * (1.0 - eased)).toLong()
        }
        val tailProgress = (progress.toDouble() - approach) / (1.0 - approach)
        val remaining = (1.0 - tailProgress).coerceIn(0.0, 1.0)
        // Quadratic deceleration: gaps between successive integers grow toward the target.
        // Ceil reserves the final integer for completion rather than rounding to it early.
        return ceil(tail * remaining * remaining).toLong().coerceIn(0L, tail)
    }
}

private class ClockUnits(value: Long) {
    val hours = value / 3_600
    val minutes = value % 3_600 / 60
    val seconds = value % 60
}

/** Borrow only downwards (e.g. 1:00:00 -> 0:59:59), never invent 1:59:59 above the real maximum. */
private fun descendingClockStart(maximum: Long, target: Long): Long {
    val targetClock = ClockUnits(target)
    var start = maximum
    if (ClockUnits(start).minutes < targetClock.minutes) start = start / 3_600 * 3_600 - 1
    if (start % 60 < targetClock.seconds) {
        start = if (ClockUnits(start).minutes > targetClock.minutes) start / 60 * 60 - 1
            else start / 3_600 * 3_600 - 1
    }
    return start.coerceIn(target, maximum)
}
