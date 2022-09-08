package com.devidea.timeleft.datadase.itemdata

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.room.*
import com.devidea.timeleft.widget.AppWidget


@Entity
data class ItemEntity(
    @NonNull var type: String,
    @NonNull var title: String,
    @NonNull var startValue: String,
    @NonNull var endValue: String,
    @NonNull var updateFlag: Int,
    @NonNull var updateRate: Int,

) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

}