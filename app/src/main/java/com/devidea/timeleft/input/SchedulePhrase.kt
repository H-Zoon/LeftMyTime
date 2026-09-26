package com.devidea.timeleft.input

import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import java.time.LocalDate
import java.time.LocalTime

sealed interface SchedulePhrase {
    data class Focus(val minutes: Int) : SchedulePhrase
    data class Draft(val item: ItemEntity) : SchedulePhrase
}

/** Deliberately small, local grammar. Never infer recurrence or silently repair invalid dates. */
object SchedulePhraseParser {
    private val focusKo = Regex("^(\\d{1,4})\\s*분\\s*집중$")
    private val focusEn = Regex("^focus\\s+(\\d{1,4})\\s*(?:m|min|minutes)$", RegexOption.IGNORE_CASE)
    private val range = Regex("^(매일|평일|daily|weekdays)\\s+(\\d{1,2}:\\d{2})\\s*[-–~]\\s*(?:(다음\\s*날|next\\s+day)\\s*)?(\\d{1,2}:\\d{2})\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val dates = Regex("^(\\d{4}-\\d{2}-\\d{2})\\s*(?:~|to|부터)\\s*(\\d{4}-\\d{2}-\\d{2})(?:\\s*까지)?\\s+(.+)$", RegexOption.IGNORE_CASE)

    fun parse(input: String): SchedulePhrase? = runCatching {
        require(input.length <= 512)
        val text = input.trim().replace(Regex("\\s+"), " ")
        (focusKo.matchEntire(text) ?: focusEn.matchEntire(text))?.let {
            val minutes = it.groupValues[1].toInt()
            require(minutes in 1..1440)
            return SchedulePhrase.Focus(minutes)
        }
        range.matchEntire(text)?.let {
            val recurrence = it.groupValues[1]
            val from = it.groupValues[2]
            val overnight = it.groupValues[3]
            val to = it.groupValues[4]
            val title = it.groupValues[5]
            fun clock(value: String): LocalTime = value.split(':').let { parts -> LocalTime.of(parts[0].toInt(), parts[1].toInt()) }
            val start = clock(from)
            val end = clock(to)
            val nextDay = overnight.isNotEmpty()
            require(if (nextDay) !end.isAfter(start) else end.isAfter(start))
            return SchedulePhrase.Draft(ItemEntity(type = ItemType.Time, title = title.trim(),
                startValue = "${start.hour}:${start.minute}", endValue = "${end.hour}:${end.minute}",
                updateFlag = RecurrenceMode.TimeRange, updateRate = 0,
                weekdays = if (recurrence.equals("weekdays", true) || recurrence == "평일") 31 else 127,
                endNextDay = nextDay))
        }
        dates.matchEntire(text)?.let {
            val start = LocalDate.parse(it.groupValues[1])
            val end = LocalDate.parse(it.groupValues[2])
            require(start.year in 1..9999 && end.year in 1..9999 && !end.isBefore(start))
            return SchedulePhrase.Draft(ItemEntity(type = ItemType.Date, title = it.groupValues[3].trim(),
                startValue = start.toString(), endValue = end.toString(), updateFlag = RecurrenceMode.None, updateRate = 0))
        }
        null
    }.getOrNull()
}
