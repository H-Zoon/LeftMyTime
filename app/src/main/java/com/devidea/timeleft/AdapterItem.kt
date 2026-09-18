package com.devidea.timeleft

import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemType

data class AdapterItem(
    val id: Int = 0,
    val title: String = "",
    val percent: Float = 0f,
    val startString: String = "",
    val endString: String = "",
    val leftString: String = "",
    val updateInfo: String = "",
    val widgetString: String = "",
    val countdownText: String = "",
    val dueText: String = "",
    val recurrenceText: String = "",
    val category: String = "",
    val colorKey: String = ItemVisuals.AUTO_COLOR_KEY,
    val iconKey: String = ItemVisuals.DEFAULT_ICON_KEY,
    val reminderText: String = "",
    val remainingSortKey: Long = Long.MAX_VALUE,
    val isExpired: Boolean = false,
    val type: ItemType? = null,
    val timePhase: TimeRangePhase? = null,
    val remainingSeconds: Long? = null,
    val remainingDays: Int? = null,
    val secondsUntilStart: Long? = null,
    val startLabel: String = "",
    val endLabel: String = "",
    val currentLabel: String = "",
)
