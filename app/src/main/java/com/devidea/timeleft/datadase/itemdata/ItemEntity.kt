package com.devidea.timeleft.datadase.itemdata

import androidx.annotation.NonNull
import androidx.room.*


@Entity
data class ItemEntity(
    @NonNull var type: String,
    @NonNull var title: String,
    @NonNull var startValue: String,
    @NonNull var endValue: String,
    @NonNull var updateFlag: Int,
    @NonNull var updateRate: Int,
    @NonNull var alarmFlag: Boolean,
    @NonNull var alarmRate: Int,
    @NonNull var weekendAlarm: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

}