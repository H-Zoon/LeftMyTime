package com.devidea.timeleft.calc

import java.time.Duration
import java.time.LocalTime

enum class TimeRangePhase { Upcoming, Active, Finished }

data class TimeRangeSnapshot(
    val phase: TimeRangePhase,
    val secondsLeft: Long,
    val secondsUntilStart: Long,
    val percentElapsed: Float,
)

/** Daily, same-day windows. These are presentation values, never database state. */
fun timeRangeSnapshot(start: LocalTime, end: LocalTime, now: LocalTime): TimeRangeSnapshot {
    // Keep legacy invalid windows inactive; the editor prevents creating new ones.
    if (!end.isAfter(start)) return TimeRangeSnapshot(TimeRangePhase.Finished, 0, 0, 100f)
    val active = TimeProgressCalculator.customTimeProgress(start, end, now)
    return when {
        active is CustomTimeProgress.Active -> TimeRangeSnapshot(
            TimeRangePhase.Active, active.durationLeft.seconds.coerceAtLeast(0), 0,
            active.percentElapsed
        )
        now.isBefore(start) -> TimeRangeSnapshot(
            TimeRangePhase.Upcoming, 0, Duration.between(now, start).seconds.coerceAtLeast(0), 0f
        )
        else -> TimeRangeSnapshot(
            TimeRangePhase.Finished, 0,
            Duration.between(now, start).plusDays(1).seconds.coerceAtLeast(0), 100f
        )
    }
}
