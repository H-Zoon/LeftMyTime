package com.devidea.timeleft.notification

import com.devidea.timeleft.focus.isFocusSession
import com.devidea.timeleft.calendar.isTimedOccurrence
import com.devidea.timeleft.calc.windowOn
import com.devidea.timeleft.calc.TimeProgressCalculator
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A future occurrence, computed without moving the saved range or opening any screen. */
data class ReminderPlan(val triggerMillis: Long, val occurrenceEnd: String)

object ReminderPlanner {
    fun next(
        item: ItemEntity,
        after: Instant,
        zone: ZoneId,
        dateReminderTime: LocalTime,
    ): ReminderPlan? {
        if (item.isFocusSession || item.reminderOffsetDays < 0) return null
        return runCatching {
            val now = after.atZone(zone)
            when (item.type) {
                ItemType.Time -> {
                    if (item.isTimedOccurrence) {
                        val end = Instant.ofEpochMilli(item.occurrenceEndMillis ?: return null)
                        val trigger = end.minusSeconds(item.reminderOffsetDays.toLong() * 60)
                        return trigger.takeIf { it.isAfter(after) }?.let { ReminderPlan(it.toEpochMilli(), end.toString()) }
                    }
                    if (item.weekdays !in 1..127 || item.reminderOffsetDays > 10_080) return null
                    ( -1L..15L ).asSequence()
                        .mapNotNull { item.windowOn(now.toLocalDate().plusDays(it), zone) }
                        .map { (_, end) -> end to end.minusMinutes(item.reminderOffsetDays.toLong()) }
                        .firstOrNull { (_, trigger) -> trigger.toInstant().isAfter(after) }
                        ?.let { (end, trigger) -> ReminderPlan(trigger.toInstant().toEpochMilli(), end.toLocalDateTime().toString()) }
                }
                ItemType.Date -> {
                    var start = LocalDate.parse(item.startValue, DATE_FORMAT)
                    var end = LocalDate.parse(item.endValue, DATE_FORMAT)
                    if (end.isBefore(start)) return null
                    // An early reminder may belong to a recurrence after the currently visible one.
                    val targetEnd = now.toLocalDate().plusDays(item.reminderOffsetDays.toLong())
                    TimeProgressCalculator.catchUpRecurrence(start, end, targetEnd, item.updateFlag, item.updateRate)?.let {
                        start = it.newStart
                        end = it.newEnd
                    }
                    var trigger = end.minusDays(item.reminderOffsetDays.toLong()).atTime(dateReminderTime).atZone(zone)
                    if (!trigger.toInstant().isAfter(after)) {
                        val next = TimeProgressCalculator.nextRecurrence(start, end, end.plusDays(1), item.updateFlag, item.updateRate)
                            ?: return null
                        end = next.newEnd
                        trigger = end.minusDays(item.reminderOffsetDays.toLong()).atTime(dateReminderTime).atZone(zone)
                    }
                    if (!trigger.toInstant().isAfter(after)) return null
                    ReminderPlan(trigger.toInstant().toEpochMilli(), end.toString())
                }
            }
        }.getOrNull()
    }

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-M-d")
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("H:m")
}
