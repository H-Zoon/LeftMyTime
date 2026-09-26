package com.devidea.timeleft.calendar

import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

val ItemEntity.isTimedOccurrence: Boolean
    get() = occurrenceStartMillis != null || occurrenceEndMillis != null

/** An already expanded provider instance. Recurrence rules are never copied into daily ranges. */
data class CalendarOccurrence(
    val sourceKey: String,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val providerRevision: String = "",
) {
    fun toItem(zone: ZoneId): ItemEntity {
        require(end > begin)
        val start = Instant.ofEpochMilli(begin).atZone(if (allDay) ZoneOffset.UTC else zone)
        val finish = Instant.ofEpochMilli(end).atZone(if (allDay) ZoneOffset.UTC else zone)
        require(start.year in 1..9999 && finish.year in 1..9999)
        return ItemEntity(type = if (allDay) ItemType.Date else ItemType.Time, title = title,
            startValue = if (allDay) start.toLocalDate().toString() else "${start.hour}:${start.minute}",
            // Calendar Provider all-day END is exclusive and UTC, even on a non-UTC device.
            endValue = if (allDay) finish.toLocalDate().minusDays(1).toString() else "${finish.hour}:${finish.minute}",
            updateFlag = RecurrenceMode.None, updateRate = 0,
            occurrenceStartMillis = begin.takeUnless { allDay }, occurrenceEndMillis = end.takeUnless { allDay },
            calendarSourceKey = sourceKey)
    }
}

/** Provider IDs are local to this installation; hash the namespace rather than storing account data. */
internal fun calendarSourceKey(namespace: String, calendarId: Long, eventId: Long, occurrence: Long?): String =
    MessageDigest.getInstance("SHA-256").digest("$namespace/$calendarId/$eventId/${occurrence ?: "single"}".toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 255) }
