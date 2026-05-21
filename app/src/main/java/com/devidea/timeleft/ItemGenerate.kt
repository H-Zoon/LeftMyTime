package com.devidea.timeleft

import com.devidea.timeleft.calc.CustomTimeProgress
import com.devidea.timeleft.calc.TimeProgressCalculator
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ItemGenerate : InterfaceItem {

    private val context get() = App.context()

    override fun timeItem(): AdapterItem {
        val progress = TimeProgressCalculator.dayProgress(LocalTime.now())
        val leftFormatted = LocalTime.ofSecondOfDay(progress.durationLeft.seconds)
            .format(DateTimeFormatter.ofPattern("H:mm:ss"))
        val leftText = context.getString(R.string.home_time_left, leftFormatted)

        return AdapterItem().apply {
            title = context.getString(R.string.home_today_title)
            percent = roundPercent(progress.percentElapsed)
            leftString = leftText
            widgetString = leftText.substring(0, leftText.length - 3)
        }
    }

    override fun yearItem(): AdapterItem {
        val today = LocalDate.now()
        val progress = TimeProgressCalculator.yearProgress(today)
        return AdapterItem().apply {
            title = context.getString(R.string.home_year_title, today.year)
            percent = roundPercent(progress.percentElapsed)
            leftString = context.getString(R.string.home_days_left, progress.daysLeft)
        }
    }

    override fun monthItem(): AdapterItem {
        val today = LocalDate.now()
        val progress = TimeProgressCalculator.monthProgress(today)
        val monthName = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return AdapterItem().apply {
            title = context.getString(R.string.home_month_title, monthName)
            percent = roundPercent(progress.percentElapsed)
            leftString = context.getString(R.string.home_days_left, progress.daysLeft)
        }
    }

    override fun customTimeItem(itemEntity: ItemEntity): AdapterItem {
        val formatter = DateTimeFormatter.ofPattern("H:m")
        val startTime = LocalTime.parse(itemEntity.startValue, formatter)
        val endTime = LocalTime.parse(itemEntity.endValue, formatter)

        val item = AdapterItem().apply {
            title = itemEntity.title
            startString = context.getString(R.string.card_time_start, startTime.toString())
            endString = context.getString(R.string.card_time_end, endTime.toString())
            updateInfo = context.getString(R.string.card_time_auto_start_hint)
            id = itemEntity.id
        }

        when (val result = TimeProgressCalculator.customTimeProgress(startTime, endTime, LocalTime.now())) {
            is CustomTimeProgress.Active -> {
                val leftFormatted = LocalTime.ofSecondOfDay(result.durationLeft.seconds)
                val leftText = context.getString(R.string.home_time_left, leftFormatted.toString())
                item.percent = roundPercent(result.percentElapsed)
                item.leftString = leftText
                item.widgetString = leftText.substring(0, leftText.length - 3)
            }
            CustomTimeProgress.Idle -> {
                item.percent = 100f
                item.leftString = context.getString(R.string.card_time_idle_hint)
                item.widgetString = context.getString(R.string.card_time_widget_idle)
            }
        }
        return item
    }

    override fun customMonthItem(itemEntity: ItemEntity): AdapterItem {
        val formatter = DateTimeFormatter.ofPattern("yyyy-M-d")
        val today = LocalDate.now()
        val startDate = LocalDate.parse(itemEntity.startValue, formatter)
        val endDate = LocalDate.parse(itemEntity.endValue, formatter)

        val progress = TimeProgressCalculator.customDateProgress(startDate, endDate, today)
        val displayPercent =
            if (progress.percentElapsed < 100f) roundPercent(progress.percentElapsed) else 100f

        return AdapterItem().apply {
            title = itemEntity.title
            startString = context.getString(R.string.card_date_start, startDate.toString())
            endString = context.getString(R.string.card_date_end, endDate.toString())
            leftString = context.getString(R.string.card_days_left_dday, progress.daysLeft)
            percent = displayPercent
            updateInfo = when (itemEntity.updateFlag) {
                0 -> context.getString(R.string.card_update_info_none)
                TimeProgressCalculator.UPDATE_FLAG_DAY ->
                    context.getString(R.string.card_update_info_day, itemEntity.updateRate)
                TimeProgressCalculator.UPDATE_FLAG_MONTH ->
                    context.getString(R.string.card_update_info_month, itemEntity.updateRate)
                else -> ""
            }
            id = itemEntity.id
        }
    }

    private fun roundPercent(raw: Float): Float =
        String.format(Locale.getDefault(), "%.1f", raw).toFloat()
}