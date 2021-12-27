package com.devidea.timeleft

import androidx.room.*

@Dao
interface DatabaseDao {
    @Insert
    fun saveItem(entityItemInfo: EntityItemInfo?)

    @get:Query("SELECT * FROM EntityItemInfo")
    val item: List<EntityItemInfo?>

    @Query("DELETE FROM EntityItemInfo WHERE id = :ID")
    fun deleteItem(ID: Int)

    @Query("SELECT * FROM EntityItemInfo WHERE id = :ID")
    fun getSelectItem(ID: Int): EntityItemInfo

    @Query("UPDATE EntityItemInfo SET startValue = :updateStart, endValue = :updateEnd WHERE id = :ID")
    fun updateItem(updateStart: String?, updateEnd: String?, ID: Int)

    //위젯엔티티
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