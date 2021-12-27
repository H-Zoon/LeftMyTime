package com.devidea.timeleft

import androidx.room.*

@Entity
class EntityItemInfo constructor(
    var type: String,
    var startValue: String,
    var endValue: String,
    var summery: String?,
    var isAutoUpdate: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

}