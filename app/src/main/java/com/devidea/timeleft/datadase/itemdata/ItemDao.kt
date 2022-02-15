package com.devidea.timeleft.datadase.itemdata

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert
    fun saveItem(itemEntity: ItemEntity)

    @Query("SELECT * FROM ItemEntity")
    fun getAllFlow(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM ItemEntity")
    fun getAll(): List<ItemEntity>

    @Query("DELETE FROM ItemEntity WHERE id = :ID")
    fun deleteItem(ID: Int)

    @Query("SELECT * FROM ItemEntity WHERE id = :ID")
    fun getSelectItem(ID: Int): ItemEntity

    @Query("UPDATE ItemEntity SET startValue = :updateStart, endValue = :updateEnd WHERE id = :ID")
    fun updateItem(updateStart: String?, updateEnd: String?, ID: Int)

    //위젯엔티티 삭제예정
    /*
    @Insert
    fun saveWidget(widgetEntity: WidgetEntity?)

    @Query("SELECT widgetID FROM WidgetEntity")
    fun get(): IntArray?

    @Query("SELECT type FROM WidgetEntity WHERE widgetID = :ID")
    fun getType(ID: Int): String?

    @Query("SELECT ItemID FROM WidgetEntity WHERE widgetID = :ID")
    fun getTypeID(ID: Int): Int

    @Query("DELETE FROM WidgetEntity WHERE widgetID = :ID")
    fun delete(ID: Int)

    @Query("DELETE FROM WidgetEntity WHERE ItemID = :ID")
    fun deleteCustomWidget(ID: Int)

     */
}