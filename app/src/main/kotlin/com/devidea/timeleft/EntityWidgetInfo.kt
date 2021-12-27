package com.devidea.timeleft

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class EntityWidgetInfo constructor(
    @field:PrimaryKey var widgetID: Int,
    var ItemID: Int,
    var type: String?
)