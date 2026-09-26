package com.devidea.timeleft.database.itemdata

import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.devidea.timeleft.ItemVisuals

@Entity(indices = [Index(value = ["stableId"], unique = true)])
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
    @ColumnInfo(defaultValue = "''") val stableId: String = "",
    @ColumnInfo(defaultValue = "0") val modifiedAt: Long = 0,
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "127") val weekdays: Int = 127,
    @ColumnInfo(defaultValue = "0") val endNextDay: Boolean = false,
    @ColumnInfo(defaultValue = "''") val focusState: String = "",
    val focusDurationMillis: Long? = null,
    val focusStartedAt: Long? = null,
    val focusResumedAt: Long? = null,
    val focusEndsAt: Long? = null,
    val focusRemainingMillis: Long? = null,
    @ColumnInfo(defaultValue = "0") val focusElapsedMillis: Long = 0,
    val focusStoppedAt: Long? = null,
    val focusResumedRealtime: Long? = null,
    val focusBootCount: Int? = null,
    @ColumnInfo(defaultValue = "0") val isTemplate: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false,
    val pinnedUntilMillis: Long? = null,
    @ColumnInfo(defaultValue = "0") val manualOrder: Long = 0,
    val occurrenceStartMillis: Long? = null,
    val occurrenceEndMillis: Long? = null,
    @ColumnInfo(defaultValue = "''") val calendarSourceKey: String = "",
)
