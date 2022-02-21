package com.devidea.timeleft

import com.devidea.timeleft.activity.MainActivity.Companion.UPDATE_FLAG_FOR_TIME
import com.devidea.timeleft.alarm.ItemAlarmManager
import com.devidea.timeleft.datadase.AppDatabase
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 각 항목에 대한 값은 entity 의 주석 확인
class ItemSave {
    private val appDatabase = AppDatabase.getDatabase(App.context())

    fun saveMonthItem(
        title: String,
        startValue: String,
        endValue: String,
        updateFlag: Int,
        updateRate: Int,
        alarmFlag: Boolean,
        alarmRate: Int
    ) {
        val entityItemInfo = ItemEntity(
            "Month",
            title,
            startValue,
            endValue,
            updateFlag,
            updateRate,
            alarmFlag,
            alarmRate,
            false
        )
        writeDatabase(entityItemInfo)
    }

    fun saveTimeItem(
        title: String,
        startValue: String,
        endValue: String,
        alarmFlag: Boolean,
        alarmRate: Int,
        weekendFlag: Boolean
    ) {
        val entityItemInfo =
            ItemEntity(
                "Time",
                title,
                startValue,
                endValue,
                UPDATE_FLAG_FOR_TIME,
                0,
                alarmFlag,
                alarmRate,
                weekendFlag
            )
        writeDatabase(entityItemInfo)
    }

    private fun writeDatabase(itemEntity: ItemEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.itemDao().saveItem(itemEntity)
            //test
            ItemAlarmManager().alarmInit()
        }


    }
}