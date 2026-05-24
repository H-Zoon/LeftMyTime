package com.devidea.timeleft

import com.devidea.timeleft.database.itemdata.ItemType

object ItemVisuals {
    const val AUTO_COLOR_KEY = "auto"
    const val DEFAULT_ICON_KEY = "event"
    const val REMINDER_DISABLED = -1

    val colorKeys = listOf(
        AUTO_COLOR_KEY,
        "blue",
        "green",
        "amber",
        "red",
        "violet"
    )

    val iconKeys = listOf(
        DEFAULT_ICON_KEY,
        "work",
        "school",
        "flight",
        "cake",
        "fitness",
        "favorite"
    )

    private val dateReminderOffsets = listOf(
        REMINDER_DISABLED,
        0,
        1,
        3,
        7
    )

    private val timeReminderOffsets = listOf(
        REMINDER_DISABLED,
        5,
        10,
        30,
        60
    )

    fun reminderOffsets(type: ItemType): List<Int> = when (type) {
        ItemType.Time -> timeReminderOffsets
        ItemType.Date -> dateReminderOffsets
    }

    fun colorNameRes(key: String): Int = when (key) {
        AUTO_COLOR_KEY -> R.string.item_color_auto
        "blue" -> R.string.item_color_blue
        "green" -> R.string.item_color_green
        "amber" -> R.string.item_color_amber
        "red" -> R.string.item_color_red
        "violet" -> R.string.item_color_violet
        else -> R.string.item_color_auto
    }

    fun iconNameRes(key: String): Int = when (key) {
        "work" -> R.string.item_icon_work
        "school" -> R.string.item_icon_school
        "flight" -> R.string.item_icon_flight
        "cake" -> R.string.item_icon_cake
        "fitness" -> R.string.item_icon_fitness
        "favorite" -> R.string.item_icon_favorite
        else -> R.string.item_icon_event
    }

    fun reminderNameRes(type: ItemType, offset: Int): Int = when (type) {
        ItemType.Time -> when (offset) {
            5 -> R.string.item_reminder_five_minutes
            10 -> R.string.item_reminder_ten_minutes
            30 -> R.string.item_reminder_thirty_minutes
            60 -> R.string.item_reminder_one_hour
            else -> R.string.item_reminder_none
        }
        ItemType.Date -> when (offset) {
            0 -> R.string.item_reminder_due_day
            1 -> R.string.item_reminder_one_day
            3 -> R.string.item_reminder_three_days
            7 -> R.string.item_reminder_seven_days
            else -> R.string.item_reminder_none
        }
    }

    fun colorInt(key: String): Int = when (key) {
        "blue" -> 0xFF0B57D0.toInt()
        "green" -> 0xFF146C43.toInt()
        "amber" -> 0xFFB05A00.toInt()
        "red" -> 0xFFB3261E.toInt()
        "violet" -> 0xFF6D3CCB.toInt()
        else -> 0xFF4F46E5.toInt()
    }
}
