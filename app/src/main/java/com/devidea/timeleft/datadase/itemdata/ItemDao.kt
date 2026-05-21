package com.devidea.timeleft.datadase.itemdata

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert
    suspend fun saveItem(itemEntity: ItemEntity)

    @Update
    suspend fun updateItem(itemEntity: ItemEntity)

    // AppWidgetConfigure에서 사용
    @get:Query("SELECT * FROM ItemEntity ORDER BY id ASC")
    val item: List<ItemEntity>

    @Query("SELECT * FROM ItemEntity ORDER BY id ASC")
    fun observeItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM ItemEntity ORDER BY id ASC")
    suspend fun getItems(): List<ItemEntity>

    @Query("DELETE FROM ItemEntity WHERE id = :ID")
    suspend fun deleteItem(ID: Int)

    @Query("SELECT * FROM ItemEntity WHERE id = :ID")
    suspend fun getSelectItem(ID: Int): ItemEntity

    @Query("UPDATE ItemEntity SET startValue = :updateStart, endValue = :updateEnd WHERE id = :ID")
    suspend fun updateItem(updateStart: String?, updateEnd: String?, ID: Int)
}
