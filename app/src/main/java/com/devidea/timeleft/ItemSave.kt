package com.devidea.timeleft

import java.time.LocalDate
import java.time.LocalTime

class ItemSave {
    fun saveMonthItem(summery: String?, end: Int, autoUpdate: Boolean) {
        val entityItemInfo = EntityItemInfo(
            "Month",
            LocalDate.now().toString(),
            LocalDate.now().plusDays(end.toLong()).toString(),
            summery,
            autoUpdate
        )
        MainActivity.Companion.appDatabase!!.DatabaseDao().saveItem(entityItemInfo)
    }

    fun saveTimeItem(
        summery: String?,
        startValue: LocalTime?,
        endValue: LocalTime?,
        autoUpdate: Boolean
    ) {
        val entityItemInfo =
            EntityItemInfo("Time", startValue.toString(), endValue.toString(), summery, autoUpdate)
        MainActivity.Companion.appDatabase!!.DatabaseDao().saveItem(entityItemInfo)
    }
}