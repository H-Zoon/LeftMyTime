package com.devidea.timeleft

import com.devidea.timeleft.database.itemdata.ItemEntity

interface InterfaceItem {
    fun timeItem(): AdapterItem
    fun yearItem(): AdapterItem
    fun monthItem(): AdapterItem
    fun customTimeItem(itemEntity: ItemEntity): AdapterItem
    fun customMonthItem(itemEntity: ItemEntity): AdapterItem
}