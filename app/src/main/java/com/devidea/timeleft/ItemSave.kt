package com.devidea.timeleft

import com.devidea.timeleft.datadase.AppDatabase
import com.devidea.timeleft.datadase.itemdata.ItemEntity

// 각 항목에 대한 값은 entity 의 주석 확인
class ItemSave {
    private val appDatabase = AppDatabase.getDatabase(App.context())

    fun saveMonthItem(summery: String, start: String, end: String, autoUpdateFlag: Int, updateRate: Int) {
        val entityItemInfo = ItemEntity(
            "Month",
            summery,
            start,
            end,
            autoUpdateFlag,
            updateRate
        )
        writeDatabase(entityItemInfo)
    }

    fun saveTimeItem(
        title: String,
        startValue: String,
        endValue: String,
    ) {
        val entityItemInfo =
            ItemEntity("Time", title, startValue, endValue, 0, 0)
        writeDatabase(entityItemInfo)
    }

    private fun writeDatabase(itemEntity: ItemEntity) {
        appDatabase!!.itemDao().saveItem(itemEntity)
        MainActivity.refreshItem()
    }
}