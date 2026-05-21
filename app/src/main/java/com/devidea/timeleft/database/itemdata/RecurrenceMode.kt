package com.devidea.timeleft.database.itemdata

enum class RecurrenceMode(val raw: Int) {
    None(0),
    Day(1),
    Month(2),
    TimeRange(3);

    companion object {
        fun fromRaw(raw: Int): RecurrenceMode = entries.first { it.raw == raw }
    }
}