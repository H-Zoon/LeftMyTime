package com.devidea.timeleft

import com.devidea.timeleft.datadase.AppDatabase
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*

class ItemGenerate : InterfaceItem {


    private val appDatabase = AppDatabase.getDatabase(App.context())

    override fun timeItem(): AdapterItem {
        val adapterItem = AdapterItem()
        val now = LocalTime.now()
        val endValue = LocalTime.parse("23:59:59")
        val secondsValue = Duration.between(LocalTime.of(0, 0), now).seconds.toFloat()
        adapterItem.title = "오늘의 "
        adapterItem.percent =
            String.format(Locale.getDefault(), "%.1f", secondsValue / 86400 * 100).toFloat()
        adapterItem.leftString = "남은시간: " + LocalTime.ofSecondOfDay(
            Duration.between(
                now,
                endValue
            ).seconds
        )
        adapterItem.widgetString =  adapterItem.leftString.substring(0,  adapterItem.leftString.length - 3)

        return adapterItem
    }

    override fun yearItem(): AdapterItem {
        val adapterItem = AdapterItem()

        adapterItem.title = LocalDate.now().year.toString() + "년의 "
        adapterItem.percent = String.format(
            Locale.getDefault(),
            "%.1f",
            (LocalDate.now().dayOfYear.toFloat()/ LocalDate.now().lengthOfYear().toFloat()) * 100
        ).toFloat()

        adapterItem.leftString = "남은일: " + (LocalDate.now()
            .lengthOfYear() - LocalDate.now().dayOfYear) + "일"
        return adapterItem
    }

    override fun monthItem(): AdapterItem {
        val adapterItem = AdapterItem()
        val monthOfDay = LocalDate.now().dayOfMonth
        val lengthOfMonth = LocalDate.now().lengthOfMonth()
        val summery = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.KOREAN)

        adapterItem.title = summery + "의 "
        adapterItem.percent =
            String.format(
                Locale.getDefault(),
                "%.1f",
                monthOfDay.toFloat() / lengthOfMonth * 100
            )
                .toFloat()
        adapterItem.leftString = "남은일: " + (lengthOfMonth - monthOfDay) + "일"
        return adapterItem
    }

    override fun customTimeItem(itemEntity: ItemEntity): AdapterItem {

        val adapterItem = AdapterItem()
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:m")
        val startTime = LocalTime.parse(itemEntity.startValue, formatter) //시작시간
        val endTime = LocalTime.parse(itemEntity.endValue, formatter) // 종료시간
        val time = LocalTime.now() //현재 시간

        adapterItem.title = itemEntity.title
        adapterItem.startString = "설정시간: $startTime"
        adapterItem.endString = "종료시간: $endTime"
        adapterItem.updateInfo = "설정시간 이후 자동으로 시작"
        adapterItem.id = itemEntity.id

        if (time.isAfter(startTime) && time.isBefore(endTime)) {
            val range = Duration.between(startTime, endTime).seconds.toFloat()
            val sendTime = Duration.between(startTime, time).seconds.toFloat()
            adapterItem.leftString = "남은시간: " + LocalTime.ofSecondOfDay(
                Duration.between(
                    time,
                    endTime
                ).seconds
            )
            adapterItem.percent =
                String.format(Locale.getDefault(), "%.1f", sendTime / range * 100).toFloat()
        } else {
            adapterItem.percent = 100.toFloat()
            adapterItem.leftString = "설정시간이 지나면 계산해 드릴께요"
        }

        adapterItem.widgetString =  adapterItem.leftString.substring(0,  adapterItem.leftString.length - 3)

        return adapterItem
    }


    override fun customMonthItem(itemEntity: ItemEntity): AdapterItem {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-M-d")
        var itemInfo = itemEntity
        val adapterItem = AdapterItem()
        var startDate = LocalDate.parse(itemInfo.startValue, formatter)
        var endDate = LocalDate.parse(itemInfo.endValue, formatter)
        val updateRate = itemEntity.updateRate
        val today = LocalDate.now()

        /*
        if (itemEntity.alarmFlag) {
            adapterItem.alarmInfo = itemEntity.alarmRate.toString() + "일전 알림설정"
        } else {
            adapterItem.alarmInfo = "알람없음"
        }

         */

        val id = itemInfo.id

        //설정일
        var setDay = ChronoUnit.DAYS.between(startDate, endDate).toInt()

        //설정일에서 지난일
        var sendDay = ChronoUnit.DAYS.between(startDate, today).toInt()

        //설정일까지 남은일
        var leftDay = ChronoUnit.DAYS.between(today, endDate).toInt()

        //종료일로 넘어가고 자동 업데이트 체크한 경우
        if (today.isAfter(endDate)) {
            when (itemEntity.updateFlag) {
                1 -> appDatabase.itemDao().updateItem(
                    endDate.toString(),
                    endDate.plusDays(updateRate.toLong()).toString(),
                    itemInfo.id
                )
                2 -> if ((endDate.plusMonths(1)).lengthOfMonth() > updateRate) {
                    appDatabase.itemDao().updateItem(
                        endDate.toString(),
                        LocalDate.of(
                            endDate.plusMonths(1).year,
                            endDate.plusMonths(1).monthValue,
                            updateRate
                        ).toString(),
                        itemInfo.id
                    )
                } else {
                    appDatabase.itemDao().updateItem(
                        endDate.toString(),
                        LocalDate.of(
                            endDate.plusMonths(1).year,
                            endDate.plusMonths(2).monthValue,
                            updateRate
                        ).toString(),
                        itemInfo.id
                    )
                }
            }
            //ItemAlarmManager().alarmInit()

            itemInfo = appDatabase.itemDao().getSelectItem(id)
            startDate = LocalDate.parse(itemInfo.startValue, formatter)
            endDate = LocalDate.parse(itemInfo.endValue, formatter)

            setDay = ChronoUnit.DAYS.between(startDate, endDate).toInt()
            sendDay = ChronoUnit.DAYS.between(startDate, today).toInt()
            leftDay = ChronoUnit.DAYS.between(today, endDate).toInt()
        }

        val monthPercent = sendDay.toFloat() / setDay * 100

        if (monthPercent < 100) {
            adapterItem.percent =
                String.format(Locale.getDefault(), "%.1f", monthPercent).toFloat()
        } else {
            adapterItem.percent = 100F
        }

        adapterItem.title = itemEntity.title
        adapterItem.startString = "설정일: $startDate"
        adapterItem.endString = "종료일: $endDate"
        adapterItem.leftString = "남은일: D-$leftDay"

        when (itemEntity.updateFlag) {
            0 -> adapterItem.updateInfo = "100% 달성후 끝나는 일정."
            1 -> adapterItem.updateInfo = "종료 후 " +
                    itemEntity.updateRate + "일 뒤 반복되는 일정."
            2 -> adapterItem.updateInfo =
                "매 달 " + itemEntity.updateRate + "일에 반복되는 일정."
        }

        adapterItem.id = itemEntity.id

        return adapterItem
    }
}


