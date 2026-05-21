package com.devidea.timeleft.database.itemdata

enum class ItemType(val raw: String) {
    Time("Time"),
    Date("Month");

    companion object {
        fun fromRaw(raw: String): ItemType = entries.first { it.raw == raw }
    }
}