package com.devidea.timeleft.calc

import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.calendar.isTimedOccurrence
import java.time.Instant
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Weekday belongs to the start date, including a Monday night continuing into Tuesday. */
data class ScheduledTimeWindow(val start: ZonedDateTime, val end: ZonedDateTime, val phase: TimeRangePhase) {
    val totalSeconds: Long get() = Duration.between(start, end).seconds
}

fun ItemEntity.windowOn(day: LocalDate, zone: ZoneId): Pair<ZonedDateTime, ZonedDateTime>? {
    if (isTimedOccurrence) return null // A calendar copy must never acquire a daily recurrence.
    if (weekdays !in 1..127 || weekdays and (1 shl (day.dayOfWeek.value - 1)) == 0) return null
    val format = DateTimeFormatter.ofPattern("H:m")
    val startTime = runCatching { LocalTime.parse(startValue, format) }.getOrNull() ?: return null
    val endTime = runCatching { LocalTime.parse(endValue, format) }.getOrNull() ?: return null
    if ((!endNextDay && !endTime.isAfter(startTime)) || (endNextDay && endTime.isAfter(startTime))) return null
    val start = day.atTime(startTime).atZone(zone)
    val end = day.plusDays(if (endNextDay) 1 else 0).atTime(endTime).atZone(zone)
    if (!end.isAfter(start)) return null
    return start to end
}

fun scheduledTimeWindow(item: ItemEntity, now: ZonedDateTime): ScheduledTimeWindow? {
    if (item.isTimedOccurrence) {
        val start = Instant.ofEpochMilli(item.occurrenceStartMillis ?: return null).atZone(now.zone)
        val end = Instant.ofEpochMilli(item.occurrenceEndMillis ?: return null).atZone(now.zone)
        if (!end.isAfter(start)) return null
        return ScheduledTimeWindow(start, end, when {
            now.isBefore(start) -> TimeRangePhase.Upcoming
            !now.isBefore(end) -> TimeRangePhase.Finished
            else -> TimeRangePhase.Active
        })
    }
    val date = now.toLocalDate()
    val windows = (-1L..7L).mapNotNull { item.windowOn(date.plusDays(it), now.zone) }
    windows.firstOrNull { (start, end) -> !now.isBefore(start) && now.isBefore(end) }?.let {
        return ScheduledTimeWindow(it.first, it.second, TimeRangePhase.Active)
    }
    val next = windows.filter { it.first.isAfter(now) }.minByOrNull { it.first.toInstant() } ?: return null
    val finishedToday = windows.any { it.second.toLocalDate() == date && !it.second.isAfter(now) }
    return ScheduledTimeWindow(next.first, next.second, if (finishedToday) TimeRangePhase.Finished else TimeRangePhase.Upcoming)
}
