package com.devidea.timeleft.repository

import com.devidea.timeleft.calc.TimeProgressCalculator
import com.devidea.timeleft.database.itemdata.ItemDao
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeLeftRepository @Inject constructor(
    private val itemDao: ItemDao,
) {
    val items: Flow<List<ItemEntity>> = itemDao.observeItems()

    suspend fun allItems(): List<ItemEntity> = itemDao.getItems()

    suspend fun getItem(id: Int): ItemEntity = itemDao.getSelectItem(id)

    suspend fun save(itemEntity: ItemEntity) {
        itemDao.saveItem(itemEntity)
    }

    suspend fun update(itemEntity: ItemEntity) {
        itemDao.updateItem(itemEntity)
    }

    suspend fun saveOrUpdate(itemEntity: ItemEntity) {
        if (itemEntity.id == 0) save(itemEntity) else update(itemEntity)
    }

    suspend fun delete(id: Int) {
        itemDao.deleteItem(id)
    }

    suspend fun advanceExpiredRecurrences(
        entities: List<ItemEntity>,
        today: LocalDate = LocalDate.now()
    ): List<ItemEntity> = entities.map { advanceExpiredRecurrence(it, today) }

    suspend fun advanceExpiredRecurrence(
        entity: ItemEntity,
        today: LocalDate = LocalDate.now()
    ): ItemEntity {
        if (entity.type == ItemType.Time) return entity

        val endDate = LocalDate.parse(entity.endValue, DATE_FORMATTER)
        val shift = TimeProgressCalculator.nextRecurrence(
            currentEnd = endDate,
            today = today,
            updateFlag = entity.updateFlag,
            updateRate = entity.updateRate
        ) ?: return entity

        itemDao.updateItem(
            shift.newStart.toString(),
            shift.newEnd.toString(),
            entity.id
        )

        return entity.copy(
            startValue = shift.newStart.toString(),
            endValue = shift.newEnd.toString()
        )
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d")
    }
}
