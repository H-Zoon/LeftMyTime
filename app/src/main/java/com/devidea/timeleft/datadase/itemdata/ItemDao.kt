package com.devidea.timeleft.datadase.itemdata

import androidx.room.*

@Dao
interface ItemDao {
    @Insert
    fun saveItem(itemEntity: ItemEntity)

    // AppWidgetConfigure에서 사용
    @get:Query("SELECT * FROM ItemEntity")
    val item: List<ItemEntity>

    @Query("DELETE FROM ItemEntity WHERE id = :ID")
    fun deleteItem(ID: Int)

    @Query("SELECT * FROM ItemEntity WHERE id = :ID")
    fun getSelectItem(ID: Int): ItemEntity

    @Query("UPDATE ItemEntity SET startValue = :updateStart, endValue = :updateEnd WHERE id = :ID")
    fun updateItem(updateStart: String?, updateEnd: String?, ID: Int)
}