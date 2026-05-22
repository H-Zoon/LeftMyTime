package com.devidea.timeleft

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
    val remainingSortKey: Long = Long.MAX_VALUE,
    val isExpired: Boolean = false,
)
