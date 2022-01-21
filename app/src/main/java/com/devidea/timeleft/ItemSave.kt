package com.devidea.timeleft

import com.devidea.timeleft.datadase.AppDatabase
import com.devidea.timeleft.datadase.itemdata.ItemEntity

class ItemSave {
    private val appDatabase = AppDatabase.getDatabase(App.context())

    fun saveMonthItem(summery: String, start: String, end: String, autoUpdate: Boolean, updateRate: Int) {
        val entityItemInfo = ItemEntity(
            "Month",
            summery,
            start,
            end,
            autoUpdate,
            updateRate
        )
        writeDatabase(entityItemInfo)
    }

    fun saveTimeItem(
        title: String,
        startValue: String,
        endValue: String,
        autoUpdate: Boolean
    ) {
        val entityItemInfo =
            ItemEntity("Time", title, startValue, endValue, autoUpdate, 0)
        writeDatabase(entityItemInfo)
    }

    private fun writeDatabase(itemEntity: ItemEntity) {
        appDatabase!!.itemDao().saveItem(itemEntity)
        MainActivity.refreshItem()
    }
}