package com.devidea.timeleft.datadase.itemdata

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class WidgetEntity constructor(
    @field:PrimaryKey var widgetID: Int,
    var ItemID: Int,
    var type: String?
)