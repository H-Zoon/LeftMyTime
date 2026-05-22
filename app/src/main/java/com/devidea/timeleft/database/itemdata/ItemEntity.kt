package com.devidea.timeleft.database.itemdata

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.devidea.timeleft.ItemVisuals

@Entity
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: ItemType,
    val title: String,
    val startValue: String,
    val endValue: String,
    val updateFlag: RecurrenceMode,
    val updateRate: Int,
    val category: String = "",
    val colorKey: String = ItemVisuals.AUTO_COLOR_KEY,
    val iconKey: String = ItemVisuals.DEFAULT_ICON_KEY,
    val reminderOffsetDays: Int = ItemVisuals.REMINDER_DISABLED,
)
