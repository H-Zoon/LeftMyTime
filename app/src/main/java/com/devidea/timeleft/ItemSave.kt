package com.devidea.timeleft

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
        summery: String,
        start: String,
        end: String,
        autoUpdateFlag: Int,
        updateRate: Int,
        alarmRate: Int
    ) {
        val entityItemInfo = ItemEntity(
            "Month",
            summery,
            start,
            end,
            autoUpdateFlag,
            updateRate,
            alarmRate
        )
        writeDatabase(entityItemInfo)
    }

    fun saveTimeItem(
        title: String,
        startValue: String,
        endValue: String,
        autoUpdateFlag: Int,
        alarmRate: Int
    ) {
        val entityItemInfo =
            ItemEntity("Time", title, startValue, endValue, autoUpdateFlag, 0, alarmRate)
        writeDatabase(entityItemInfo)
    }

    private fun writeDatabase(itemEntity: ItemEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.itemDao().saveItem(itemEntity)
        }

        //test
        ItemAlarmManager().alarmInit()
    }
}