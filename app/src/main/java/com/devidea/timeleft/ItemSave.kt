package com.devidea.timeleft

import java.time.LocalDate

class ItemSave {
    private val appDatabase = AppDatabase.getInstance(App.context())

    fun saveMonthItem(summery: String?, end: Int, autoUpdate: Boolean) {
        val entityItemInfo = EntityItemInfo(
            "Month",
            LocalDate.now().toString(),
            LocalDate.now().plusDays(end.toLong()).toString(),
            summery,
            autoUpdate
        )
        writeDatabase(entityItemInfo)
    }

    fun saveTimeItem(
        summery: String,
        startValue: String,
        endValue: String,
        autoUpdate: Boolean
    ) {
        val entityItemInfo =
            EntityItemInfo("Time", startValue, endValue, summery, autoUpdate)
        writeDatabase(entityItemInfo)
    }

    private fun writeDatabase(entityItemInfo: EntityItemInfo) {
        appDatabase!!.DatabaseDao().saveItem(entityItemInfo)
        MainActivity.refreshItem()
    }
}