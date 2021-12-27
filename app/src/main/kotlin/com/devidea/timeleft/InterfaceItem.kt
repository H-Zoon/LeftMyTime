package com.devidea.timeleft

open interface InterfaceItem {
    fun timeItem(): AdapterItem
    fun yearItem(): AdapterItem
    fun monthItem(): AdapterItem
    fun customTimeItem(itemInfo: EntityItemInfo?): AdapterItem
    fun customMonthItem(itemInfo: EntityItemInfo?): AdapterItem
}