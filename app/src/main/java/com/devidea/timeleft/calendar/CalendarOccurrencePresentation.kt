package com.devidea.timeleft.calendar

import android.content.Context
import com.devidea.timeleft.*
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun occurrenceLabel(context: Context, millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val locale = context.resources.configuration.locales[0]
    val date = time.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "yMMMd"), locale))
    return context.getString(R.string.time_date_clock, date, formatClockTime(context, time.toLocalTime()))
}

internal fun timedOccurrenceItem(context: Context, item: ItemEntity, now: Long = System.currentTimeMillis()): AdapterItem {
    val start = requireNotNull(item.occurrenceStartMillis)
    val end = requireNotNull(item.occurrenceEndMillis)
    require(end > start)
    val phase = when { now < start -> TimeRangePhase.Upcoming; now >= end -> TimeRangePhase.Finished; else -> TimeRangePhase.Active }
    val total = ((end - start) / 1000).coerceAtLeast(1)
    val elapsed = ((now - start) / 1000).coerceIn(0, total)
    val remaining = ((end - now + 999) / 1000).coerceAtLeast(0)
    val untilStart = ((start - now + 999) / 1000).takeIf { phase == TimeRangePhase.Upcoming }
    val percent = elapsed.toFloat() / total * 100f
    val startLabel = occurrenceLabel(context, start)
    val endLabel = occurrenceLabel(context, end)
    val zonedStart = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault())
    val zonedEnd = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault())
    val finished = phase == TimeRangePhase.Finished
    val value = if (finished) context.getString(R.string.calendar_occurrence_finished)
        else formatRemainingTime(context, untilStart ?: remaining, null, showSeconds = true)
    return AdapterItem(id = item.id, title = item.title, type = ItemType.Time,
        isCalendarOccurrence = true, timePhase = phase, isExpired = finished,
        isPinned = item.isPinned && (item.pinnedUntilMillis ?: 0) > now && !finished, manualOrder = item.manualOrder,
        startsAtMillis = start, endsAtMillis = end, remainingSeconds = remaining, secondsUntilStart = untilStart,
        remainingSortKey = if (finished) Long.MAX_VALUE else untilStart ?: remaining,
        startLabel = startLabel, endLabel = endLabel,
        startString = context.getString(R.string.card_time_start, startLabel),
        endString = context.getString(R.string.card_time_end, endLabel),
        updateInfo = context.getString(R.string.calendar_copy_hint), countdownText = value, leftString = value,
        dueText = if (finished) value else context.getString(R.string.home_until_time, endLabel),
        category = item.category, colorKey = item.colorKey, iconKey = item.iconKey,
        reminderText = if (item.reminderOffsetDays < 0) "" else context.getString(ItemVisuals.reminderNameRes(ItemType.Time, item.reminderOffsetDays)),
        percent = percent,
        detailFacts = TimeDetailFacts(percent, elapsed, total, phase, secondsLeft = remaining, secondsUntilStart = untilStart,
            range = TimeDetailRange.Clock(zonedStart.toLocalTime(), zonedEnd.toLocalTime(), zonedStart.toLocalDate(),
                durationSeconds = total, startEpochSecond = start / 1000)))
}
