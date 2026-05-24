package com.devidea.timeleft

import android.content.Context
import com.devidea.timeleft.calc.CustomTimeProgress
import com.devidea.timeleft.calc.TimeProgressCalculator
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemGenerate @Inject constructor(
    @ApplicationContext private val context: Context,
) : InterfaceItem {

    override fun timeItem(): AdapterItem {
        val progress = TimeProgressCalculator.dayProgress(LocalTime.now())
        val leftFormatted = LocalTime.ofSecondOfDay(progress.durationLeft.seconds)
            .format(HEADER_TIME_FORMATTER)
        val leftText = context.getString(R.string.home_time_left, leftFormatted)

        return AdapterItem(
            title = context.getString(R.string.home_today_title),
            percent = roundPercent(progress.percentElapsed),
            leftString = leftText,
            widgetString = leftText.substring(0, leftText.length - 3),
        )
    }

    override fun yearItem(): AdapterItem {
        val today = LocalDate.now()
        val progress = TimeProgressCalculator.yearProgress(today)
        return AdapterItem(
            title = context.getString(R.string.home_year_title, today.year),
            percent = roundPercent(progress.percentElapsed),
            leftString = context.getString(R.string.home_days_left, progress.daysLeft),
        )
    }

    override fun monthItem(): AdapterItem {
        val today = LocalDate.now()
        val progress = TimeProgressCalculator.monthProgress(today)
        val monthName = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return AdapterItem(
            title = context.getString(R.string.home_month_title, monthName),
            percent = roundPercent(progress.percentElapsed),
            leftString = context.getString(R.string.home_days_left, progress.daysLeft),
        )
    }

    override fun customTimeItem(itemEntity: ItemEntity): AdapterItem {
        val startTime = LocalTime.parse(itemEntity.startValue, STORAGE_TIME_FORMATTER)
        val endTime = LocalTime.parse(itemEntity.endValue, STORAGE_TIME_FORMATTER)

        val base = AdapterItem(
            id = itemEntity.id,
            title = itemEntity.title,
            startString = context.getString(R.string.card_time_start, startTime.toString()),
            endString = context.getString(R.string.card_time_end, endTime.toString()),
            updateInfo = context.getString(R.string.card_time_auto_start_hint),
            category = itemEntity.category,
            colorKey = itemEntity.colorKey,
            iconKey = itemEntity.iconKey,
            reminderText = reminderText(ItemType.Time, itemEntity.reminderOffsetDays),
        )

        return when (val result = TimeProgressCalculator.customTimeProgress(startTime, endTime, LocalTime.now())) {
            is CustomTimeProgress.Active -> {
                val leftFormatted = LocalTime.ofSecondOfDay(result.durationLeft.seconds)
                val leftText = context.getString(R.string.home_time_left, leftFormatted.toString())
                base.copy(
                    percent = roundPercent(result.percentElapsed),
                    leftString = leftText,
                    widgetString = leftText.substring(0, leftText.length - 3),
                    countdownText = leftFormatted.toString(),
                    dueText = context.getString(R.string.home_until_time, endTime.toString()),
                    remainingSortKey = result.durationLeft.seconds,
                )
            }
            CustomTimeProgress.Idle -> base.copy(
                percent = 100f,
                leftString = context.getString(R.string.card_time_idle_hint),
                widgetString = context.getString(R.string.card_time_widget_idle),
                countdownText = context.getString(R.string.home_waiting_countdown),
                dueText = context.getString(R.string.home_until_time, endTime.toString()),
            )
        }
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
            id = itemEntity.id,
            title = itemEntity.title,
            startString = context.getString(R.string.card_date_start, startDate.toString()),
            endString = context.getString(R.string.card_date_end, endDate.toString()),
            leftString = context.getString(R.string.card_dday_value, countdownText),
            percent = displayPercent,
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

    private fun reminderText(type: ItemType, offset: Int): String =
        if (offset == ItemVisuals.REMINDER_DISABLED) {
            ""
        } else {
            context.getString(ItemVisuals.reminderNameRes(type, offset))
        }

    private fun roundPercent(raw: Float): Float =
        String.format(Locale.getDefault(), "%.1f", raw).toFloat()

    private fun ddayText(daysLeft: Int): String =
        when {
            daysLeft > 0 -> context.getString(R.string.home_dday_before, daysLeft)
            daysLeft == 0 -> context.getString(R.string.home_dday_today)
            else -> context.getString(R.string.home_dday_after, -daysLeft)
        }

    companion object {
        private const val SECONDS_PER_DAY = 86_400L
        private val HEADER_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm:ss")
        private val STORAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:m")
        private val STORAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d")
    }
}
