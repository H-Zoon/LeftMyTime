package com.devidea.timeleft

import java.time.LocalDate
import java.time.LocalTime

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
        appDatabase!!.DatabaseDao().saveItem(entityItemInfo)
    }

    fun saveTimeItem(
        summery: String?,
        startValue: LocalTime?,
        endValue: LocalTime?,
        autoUpdate: Boolean
    ) {
        val entityItemInfo =
            EntityItemInfo("Time", startValue.toString(), endValue.toString(), summery, autoUpdate)
        appDatabase!!.DatabaseDao().saveItem(entityItemInfo)
    }
}