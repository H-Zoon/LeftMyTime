package com.devidea.timeleft.datadase.itemdata

import androidx.annotation.NonNull
import androidx.room.*

@Entity
data class ItemEntity(
    @NonNull var type: String,
    @NonNull var summery: String,
    @NonNull var startValue: String,
    @NonNull var endValue: String,
    @NonNull var autoUpdateFlag: Int,
    @NonNull var updateRate: Int
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

}