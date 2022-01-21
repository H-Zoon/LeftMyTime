package com.devidea.timeleft.datadase.itemdata

import androidx.room.*
import com.devidea.timeleft.EntityWidgetInfo

@Dao
interface ItemDao {
    @Insert
    fun saveItem(itemEntity: ItemEntity?)

    // AppWidgetConfigure에서 사용
    @get:Query("SELECT * FROM ItemEntity")
    val item: List<ItemEntity?>

    @Query("DELETE FROM ItemEntity WHERE id = :ID")
    fun deleteItem(ID: Int)

    @Query("SELECT * FROM ItemEntity WHERE id = :ID")
    fun getSelectItem(ID: Int): ItemEntity

    @Query("UPDATE ItemEntity SET startValue = :updateStart, endValue = :updateEnd WHERE id = :ID")
    fun updateItem(updateStart: String?, updateEnd: String?, ID: Int)

    //위젯엔티티 삭제예정
    @Insert
    fun saveWidget(entityWidgetInfo: EntityWidgetInfo?)

    @Query("SELECT widgetID FROM EntityWidgetInfo")
    fun get(): IntArray?

    @Query("SELECT type FROM EntityWidgetInfo WHERE widgetID = :ID")
    fun getType(ID: Int): String?

    @Query("SELECT ItemID FROM EntityWidgetInfo WHERE widgetID = :ID")
    fun getTypeID(ID: Int): Int

    @Query("DELETE FROM EntityWidgetInfo WHERE widgetID = :ID")
    fun delete(ID: Int)

    @Query("DELETE FROM EntityWidgetInfo WHERE ItemID = :ID")
    fun deleteCustomWidget(ID: Int)


}