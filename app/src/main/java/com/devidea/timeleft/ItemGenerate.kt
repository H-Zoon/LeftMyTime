package com.devidea.timeleft

import com.devidea.timeleft.focus.FocusSession
import com.devidea.timeleft.focus.isFocusSession
import com.devidea.timeleft.calendar.isTimedOccurrence
import android.content.Context
import com.devidea.timeleft.calc.scheduledTimeWindow
import java.time.ZonedDateTime
import com.devidea.timeleft.calc.CustomTimeProgress
import com.devidea.timeleft.calc.TimeProgressCalculator
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.calc.timeRangeSnapshot
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemGenerate @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : InterfaceItem {

    override fun timeItem(): AdapterItem {
        val snapshotTime = LocalDateTime.now()
        val now = snapshotTime.toLocalTime()
        val progress = TimeProgressCalculator.dayProgress(now)
        val leftTime = LocalTime.ofSecondOfDay(progress.durationLeft.seconds.coerceIn(0, 86399))
        val leftText = context.getString(
            R.string.home_time_left,
            leftTime.format(HEADER_TIME_FORMATTER)
        )

        return AdapterItem(
            title = context.getString(R.string.home_today_title),
            remainingSeconds = progress.durationLeft.seconds.coerceAtLeast(0),
            startLabel = formatClockTime(context, LocalTime.MIDNIGHT),
            endLabel = formatClockTime(context, LocalTime.of(23, 59, 59), seconds = true),
            percent = roundPercent(progress.percentElapsed),
            detailFacts = TimeDetailFacts(
                percentElapsed = progress.percentElapsed,
                elapsed = now.toSecondOfDay().toLong(), total = SECONDS_PER_DAY,
                phase = if (progress.durationLeft.isNegative || progress.durationLeft.isZero) TimeRangePhase.Finished else TimeRangePhase.Active,
                secondsLeft = progress.durationLeft.secondsForDisplay(),
                range = TimeDetailRange.Clock(LocalTime.MIDNIGHT, LocalTime.of(23, 59, 59), snapshotTime.toLocalDate()),
            ),
            leftString = leftText,
            widgetString = context.getString(
                R.string.home_time_left,
                leftTime.format(WIDGET_TIME_FORMATTER)
            ),
        )
    }

    override fun yearItem(): AdapterItem {
        val today = LocalDate.now()
        val progress = TimeProgressCalculator.yearProgress(today)
        return AdapterItem(
            title = context.getString(R.string.home_year_title, today.year),
            remainingDays = progress.daysLeft,
            startLabel = today.withDayOfYear(1).toString(),
            endLabel = today.withDayOfYear(today.lengthOfYear()).toString(),
            percent = roundPercent(progress.percentElapsed),
            detailFacts = TimeDetailFacts(progress.percentElapsed, today.dayOfYear.toLong(), today.lengthOfYear().toLong(),
                TimeRangePhase.Active, inDays = true, includesToday = true,
                range = TimeDetailRange.Calendar(today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()), includesFirstDay = true)),
            leftString = context.getString(R.string.home_days_left, progress.daysLeft),
        )
    }

    override fun monthItem(): AdapterItem {
        val today = LocalDate.now()
        val progress = TimeProgressCalculator.monthProgress(today)
        val monthName = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return AdapterItem(
            title = context.getString(R.string.home_month_title, monthName),
            remainingDays = progress.daysLeft,
            startLabel = today.withDayOfMonth(1).toString(),
            endLabel = today.withDayOfMonth(today.lengthOfMonth()).toString(),
            percent = roundPercent(progress.percentElapsed),
            detailFacts = TimeDetailFacts(progress.percentElapsed, today.dayOfMonth.toLong(), today.lengthOfMonth().toLong(),
                TimeRangePhase.Active, inDays = true, includesToday = true,
                range = TimeDetailRange.Calendar(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()), includesFirstDay = true)),
            leftString = context.getString(R.string.home_days_left, progress.daysLeft),
        )
    }

    override fun customTimeItem(itemEntity: ItemEntity): AdapterItem {
        if (itemEntity.isFocusSession) return focusItem(itemEntity)
        if (itemEntity.isTimedOccurrence) return com.devidea.timeleft.calendar.timedOccurrenceItem(context, itemEntity)
        val now = ZonedDateTime.now()
        val window = requireNotNull(scheduledTimeWindow(itemEntity, now))
        val active = window.phase == TimeRangePhase.Active
        val left = if (active) Duration.between(now, window.end).secondsForDisplay() else null
        val untilStart = if (!active) Duration.between(now, window.start).secondsForDisplay() else null
        val total = window.totalSeconds
        val elapsed = if (active) Duration.between(window.start, now).seconds.coerceIn(0, total) else 0L
        val percent = if (total > 0) elapsed.toFloat() / total * 100f else 0f
        val includeDate = itemEntity.endNextDay || window.start.toLocalDate() != now.toLocalDate()
        fun label(value: ZonedDateTime): String {
            val clock = formatClockTime(context, value.toLocalTime())
            if (!includeDate) return clock
            val day = value.toLocalDate().format(DateTimeFormatter.ofPattern(
                android.text.format.DateFormat.getBestDateTimePattern(context.resources.configuration.locales[0], "Md"),
                context.resources.configuration.locales[0]))
            return context.getString(R.string.time_date_clock, day, clock)
        }
        val startLabel = label(window.start)
        val endLabel = label(window.end)
        val weekdays = java.time.DayOfWeek.values().filter { itemEntity.weekdays and (1 shl (it.value - 1)) != 0 }
            .joinToString(" · ") { it.getDisplayName(TextStyle.SHORT, context.resources.configuration.locales[0]) }
        val updateInfo = if (itemEntity.weekdays == 127) context.getString(R.string.card_time_auto_start_hint)
            else context.getString(R.string.card_time_weekdays, weekdays)
        val leftText = formatRemainingTime(context, seconds = left, days = null, showSeconds = true)
        return AdapterItem(
            type = ItemType.Time, timePhase = window.phase,
            remainingSeconds = left, secondsUntilStart = untilStart,
            startsAtMillis = window.start.toInstant().toEpochMilli(), endsAtMillis = window.end.toInstant().toEpochMilli(),
            startLabel = startLabel, endLabel = endLabel, currentLabel = formatClockTime(context, now.toLocalTime()),
            detailFacts = TimeDetailFacts(percent, elapsed, total, window.phase,
                secondsLeft = left, secondsUntilStart = untilStart,
                range = TimeDetailRange.Clock(window.start.toLocalTime(), window.end.toLocalTime(), window.start.toLocalDate(),
                    durationSeconds = total, startEpochSecond = window.start.toEpochSecond())),
            id = itemEntity.id, title = itemEntity.title, isPinned = isPinned(itemEntity), manualOrder = itemEntity.manualOrder,
            startString = context.getString(R.string.card_time_start, startLabel),
            endString = context.getString(R.string.card_time_end, endLabel),
            updateInfo = updateInfo,
            category = itemEntity.category, colorKey = itemEntity.colorKey, iconKey = itemEntity.iconKey,
            reminderText = reminderText(ItemType.Time, itemEntity.reminderOffsetDays),
            percent = percent,
            leftString = if (active) leftText else context.getString(R.string.card_time_idle_hint),
            widgetString = if (active) formatRemainingTime(context, seconds = left, days = null) else context.getString(R.string.card_time_widget_idle),
            countdownText = if (active) leftText else context.getString(R.string.home_waiting_countdown),
            dueText = context.getString(R.string.home_until_time, endLabel),
            remainingSortKey = left ?: untilStart ?: Long.MAX_VALUE,
        )
    }

    private fun focusItem(raw: ItemEntity): AdapterItem {
        val now = System.currentTimeMillis()
        val item = FocusSession.settle(FocusSession.withClock(raw, now, com.devidea.timeleft.focus.readFocusClock(context)), now)
        val total = requireNotNull(item.focusDurationMillis) / 1000L
        val remaining = (FocusSession.remaining(item, now) + 999L) / 1000L
        val elapsed = FocusSession.elapsed(item, now) / 1000L
        val running = item.focusState == FocusSession.RUNNING
        val paused = item.focusState == FocusSession.PAUSED
        val phase = if (running) TimeRangePhase.Active else if (paused) TimeRangePhase.Upcoming else TimeRangePhase.Finished
        val status = context.getString(when (item.focusState) {
            FocusSession.RUNNING -> R.string.focus_running
            FocusSession.PAUSED -> R.string.focus_paused
            FocusSession.COMPLETED -> R.string.focus_completed
            else -> R.string.focus_stopped
        })
        val start = item.focusStartedAt?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()) }
        val end = (item.focusStoppedAt ?: item.focusEndsAt)?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()) }
        val startLabel = start?.let { formatClockTime(context, it.toLocalTime()) }.orEmpty()
        val endLabel = end?.let { formatClockTime(context, it.toLocalTime()) } ?: context.getString(R.string.focus_paused)
        val percent = if (total > 0) elapsed.toFloat() / total * 100 else 0f
        return AdapterItem(id = item.id, title = item.title, type = ItemType.Time, isPinned = isPinned(item) && !(!running && !paused), manualOrder = item.manualOrder,
            isFocusSession = true, focusState = item.focusState, timePhase = phase,
            remainingSeconds = remaining, remainingSortKey = if (running) remaining else Long.MAX_VALUE,
            startsAtMillis = item.focusStartedAt, endsAtMillis = item.focusEndsAt,
            startLabel = context.getString(R.string.focus_ruler_start),
            endLabel = formatRemainingTime(context, total, null),
            startString = context.getString(R.string.card_time_start, startLabel),
            endString = context.getString(R.string.card_time_end, endLabel), dueText = status, updateInfo = context.getString(R.string.focus_one_off),
            leftString = formatRemainingTime(context, remaining, null, showSeconds = true),
            countdownText = if (running || paused) formatRemainingTime(context, remaining, null, showSeconds = true) else status,
            percent = percent, isExpired = !running && !paused,
            detailFacts = TimeDetailFacts(percent, elapsed, total, phase, secondsLeft = remaining),
        )
    }

    override fun customMonthItem(itemEntity: ItemEntity): AdapterItem {
        val today = LocalDate.now()
        val startDate = LocalDate.parse(itemEntity.startValue, STORAGE_DATE_FORMATTER)
        val endDate = LocalDate.parse(itemEntity.endValue, STORAGE_DATE_FORMATTER)

        val progress = TimeProgressCalculator.customDateProgress(startDate, endDate, today)
        val displayPercent =
            if (progress.percentElapsed < 100f) roundPercent(progress.percentElapsed) else 100f

        val updateInfo = when (itemEntity.updateFlag) {
            RecurrenceMode.None ->
                context.getString(R.string.card_update_info_none)
            RecurrenceMode.Day ->
                context.getString(R.string.card_update_info_day, itemEntity.updateRate)
            RecurrenceMode.Month ->
                context.getString(R.string.card_update_info_month, itemEntity.updateRate)
            RecurrenceMode.TimeRange -> ""
        }
        val recurrenceText = when (itemEntity.updateFlag) {
            RecurrenceMode.Day -> context.getString(R.string.card_recurrence_day, itemEntity.updateRate)
            RecurrenceMode.Month -> context.getString(R.string.card_recurrence_month, itemEntity.updateRate)
            RecurrenceMode.None, RecurrenceMode.TimeRange -> ""
        }

        val countdownText = ddayText(progress.daysLeft)

        return AdapterItem(
            type = ItemType.Date,
            remainingDays = progress.daysLeft,
            startLabel = startDate.toString(),
            endLabel = endDate.toString(),
            id = itemEntity.id,
            isPinned = isPinned(itemEntity), manualOrder = itemEntity.manualOrder,
            title = itemEntity.title,
            startString = context.getString(R.string.card_date_start, startDate.toString()),
            endString = context.getString(R.string.card_date_end, endDate.toString()),
            leftString = context.getString(R.string.card_dday_value, countdownText),
            percent = displayPercent,
            detailFacts = TimeDetailFacts(
                // A same-day future range has zero duration in the legacy calculator;
                // its detail still needs to communicate that it has not started.
                percentElapsed = if (today.isBefore(startDate)) 0f else progress.percentElapsed,
                elapsed = ChronoUnit.DAYS.between(startDate, today).coerceIn(0, progress.daysBetween.toLong().coerceAtLeast(0)),
                total = progress.daysBetween.toLong().coerceAtLeast(0), inDays = true,
                phase = when {
                    today.isBefore(startDate) -> TimeRangePhase.Upcoming
                    today.isAfter(endDate) -> TimeRangePhase.Finished
                    else -> TimeRangePhase.Active
                },
                validRange = !endDate.isBefore(startDate),
                range = TimeDetailRange.Calendar(startDate, endDate),
            ),
            updateInfo = updateInfo,
            countdownText = countdownText,
            dueText = context.getString(R.string.home_until_date, endDate.toString()),
            recurrenceText = recurrenceText,
            category = itemEntity.category,
            colorKey = itemEntity.colorKey,
            iconKey = itemEntity.iconKey,
            reminderText = reminderText(ItemType.Date, itemEntity.reminderOffsetDays),
            remainingSortKey = if (progress.daysLeft >= 0) progress.daysLeft.toLong() * SECONDS_PER_DAY else Long.MAX_VALUE,
            isExpired = progress.daysLeft < 0,
        )
    }

    private fun isPinned(item: ItemEntity): Boolean = item.isPinned && (item.pinnedUntilMillis ?: Long.MAX_VALUE) > System.currentTimeMillis()

    private fun reminderText(type: ItemType, offset: Int): String =
        if (offset == ItemVisuals.REMINDER_DISABLED) {
            ""
        } else {
            context.getString(ItemVisuals.reminderNameRes(type, offset))
        }

    private fun ddayText(daysLeft: Int): String =
        when {
            daysLeft > 0 -> context.getString(R.string.home_dday_before, daysLeft)
            daysLeft == 0 -> context.getString(R.string.home_dday_today)
            else -> context.getString(R.string.home_dday_after, -daysLeft)
        }

    companion object {
        private const val SECONDS_PER_DAY = 86_400L
        private val HEADER_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm:ss")
        private val WIDGET_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm")
        private val STORAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:m")
        private val STORAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d")
    }
}
