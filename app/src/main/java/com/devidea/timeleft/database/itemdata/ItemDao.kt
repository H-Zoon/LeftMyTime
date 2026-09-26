package com.devidea.timeleft.database.itemdata

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert
    suspend fun saveItem(itemEntity: ItemEntity): Long

    @Update
    suspend fun updateItem(itemEntity: ItemEntity)

    // AppWidgetConfigure에서 사용
    @get:Query("SELECT * FROM ItemEntity WHERE deletedAt IS NULL AND isTemplate = 0 ORDER BY id ASC")
    val item: List<ItemEntity>

    @Query("SELECT * FROM ItemEntity WHERE deletedAt IS NULL AND isTemplate = 0 ORDER BY id ASC")
    fun observeItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM ItemEntity WHERE deletedAt IS NULL AND isTemplate = 0 ORDER BY id ASC")
    suspend fun getItems(): List<ItemEntity>

    @Query("UPDATE ItemEntity SET deletedAt = :at, modifiedAt = :at WHERE id = :ID AND deletedAt IS NULL")
    suspend fun deleteItem(ID: Int, at: Long)

    @Query("SELECT * FROM ItemEntity WHERE deletedAt IS NULL AND isTemplate = 1 ORDER BY manualOrder, id")
    fun observeTemplates(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM ItemEntity ORDER BY id ASC")
    suspend fun getAllRecords(): List<ItemEntity>

    @Query("SELECT * FROM ItemEntity WHERE id = :ID AND deletedAt IS NULL")
    suspend fun getSelectItem(ID: Int): ItemEntity

    @Query("UPDATE ItemEntity SET startValue = :updateStart, endValue = :updateEnd WHERE id = :ID")
    suspend fun updateItem(updateStart: String?, updateEnd: String?, ID: Int)
}
