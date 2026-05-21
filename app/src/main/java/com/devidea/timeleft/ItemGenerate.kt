package com.devidea.timeleft

import com.devidea.timeleft.calc.CustomTimeProgress
import com.devidea.timeleft.calc.TimeProgressCalculator
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class ItemGenerate : InterfaceItem {

    override fun timeItem(): AdapterItem {
        val progress = TimeProgressCalculator.dayProgress(LocalTime.now())
        val leftFormatted = LocalTime.ofSecondOfDay(progress.durationLeft.seconds)
            .format(DateTimeFormatter.ofPattern("H:mm:ss"))

        return AdapterItem().apply {
            title = "오늘의 "
            percent = roundPercent(progress.percentElapsed)
            leftString = "남은시간: $leftFormatted"
            widgetString = leftString.substring(0, leftString.length - 3)
        }
    }

    override fun yearItem(): AdapterItem {
        val today = LocalDate.now()
        val progress = TimeProgressCalculator.yearProgress(today)
        return AdapterItem().apply {
            title = "${today.year}년의 "
            percent = roundPercent(progress.percentElapsed)
            leftString = "남은일: ${progress.daysLeft}일"
        }
    }

    override fun monthItem(): AdapterItem {
        val today = LocalDate.now()
        val progress = TimeProgressCalculator.monthProgress(today)
        val monthName = today.month.getDisplayName(TextStyle.FULL, Locale.KOREAN)
        return AdapterItem().apply {
            title = "${monthName}의 "
            percent = roundPercent(progress.percentElapsed)
            leftString = "남은일: ${progress.daysLeft}일"
        }
    }

    override fun customTimeItem(itemEntity: ItemEntity): AdapterItem {
        val formatter = DateTimeFormatter.ofPattern("H:m")
        val startTime = LocalTime.parse(itemEntity.startValue, formatter)
        val endTime = LocalTime.parse(itemEntity.endValue, formatter)

        val item = AdapterItem().apply {
            title = itemEntity.title
            startString = "설정시간: $startTime"
            endString = "종료시간: $endTime"
            updateInfo = "설정시간 이후 자동으로 시작"
            id = itemEntity.id
        }

        when (val result = TimeProgressCalculator.customTimeProgress(startTime, endTime, LocalTime.now())) {
            is CustomTimeProgress.Active -> {
                val leftFormatted = LocalTime.ofSecondOfDay(result.durationLeft.seconds)
                item.percent = roundPercent(result.percentElapsed)
                item.leftString = "남은시간: $leftFormatted"
                item.widgetString = item.leftString.substring(0, item.leftString.length - 3)
            }
            CustomTimeProgress.Idle -> {
                item.percent = 100f
                item.leftString = "설정시간이 지나면 계산해 드릴께요"
                item.widgetString = "남은시간: 00:00"
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
            startString = "설정일: $startDate"
            endString = "종료일: $endDate"
            leftString = "남은일: D-${progress.daysLeft}"
            percent = displayPercent
            updateInfo = when (itemEntity.updateFlag) {
                0 -> "100% 달성후 끝나는 일정."
                TimeProgressCalculator.UPDATE_FLAG_DAY ->
                    "종료 후 ${itemEntity.updateRate}일 뒤 반복되는 일정."
                TimeProgressCalculator.UPDATE_FLAG_MONTH ->
                    "매 달 ${itemEntity.updateRate}일에 반복되는 일정."
                else -> ""
            }
            id = itemEntity.id
        }
    }

    private fun roundPercent(raw: Float): Float =
        String.format(Locale.getDefault(), "%.1f", raw).toFloat()
}
