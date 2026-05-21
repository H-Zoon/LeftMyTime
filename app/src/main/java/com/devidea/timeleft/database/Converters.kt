package com.devidea.timeleft.database

import androidx.room.TypeConverter
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode

class Converters {
    @TypeConverter
    fun itemTypeToString(type: ItemType): String = type.raw

    @TypeConverter
    fun stringToItemType(raw: String): ItemType = ItemType.fromRaw(raw)

    @TypeConverter
    fun recurrenceModeToInt(mode: RecurrenceMode): Int = mode.raw

    @TypeConverter
    fun intToRecurrenceMode(raw: Int): RecurrenceMode = RecurrenceMode.fromRaw(raw)
}