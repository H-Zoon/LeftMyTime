package com.devidea.timeleft.repository

import androidx.room.withTransaction
import com.devidea.timeleft.database.AppDatabase
import com.devidea.timeleft.focus.isFocusSession
import com.devidea.timeleft.calendar.isTimedOccurrence
import com.devidea.timeleft.database.itemdata.ItemDao
import com.devidea.timeleft.database.itemdata.ItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeLeftRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val database: AppDatabase,
) {
    val items: Flow<List<ItemEntity>> = itemDao.observeItems()
    val templates: Flow<List<ItemEntity>> = itemDao.observeTemplates()

    suspend fun allItems(): List<ItemEntity> = itemDao.getItems()

    suspend fun getItem(id: Int): ItemEntity = itemDao.getSelectItem(id)

    suspend fun save(itemEntity: ItemEntity): ItemEntity {
        val saved = itemEntity.copy(id = 0, stableId = UUID.randomUUID().toString(),
            modifiedAt = System.currentTimeMillis(), deletedAt = null, manualOrder = System.currentTimeMillis())
        val id = itemDao.saveItem(saved).toInt()
        return saved.copy(id = id)
    }

    suspend fun update(itemEntity: ItemEntity): ItemEntity {
        val current = itemDao.getSelectItem(itemEntity.id)
        val saved = itemEntity.copy(stableId = current.stableId, isTemplate = current.isTemplate,
            isPinned = current.isPinned, pinnedUntilMillis = current.pinnedUntilMillis, manualOrder = current.manualOrder,
            modifiedAt = maxOf(System.currentTimeMillis(), current.modifiedAt + 1), deletedAt = null)
        itemDao.updateItem(saved)
        return saved
    }

    suspend fun saveOrUpdate(itemEntity: ItemEntity): ItemEntity {
        return if (itemEntity.id == 0) save(itemEntity) else update(itemEntity)
    }

    suspend fun delete(id: Int) = database.withTransaction {
        val current = itemDao.getSelectItem(id)
        itemDao.deleteItem(id, maxOf(System.currentTimeMillis(), current.modifiedAt + 1))
        database.focusCompletionDao().removeForItem(id)
    }

    suspend fun saveTemplate(id: Int) {
        val source = getItem(id)
        require(!source.isFocusSession && !source.isTemplate && !source.isTimedOccurrence)
        save(source.copy(id = 0, isTemplate = true, isPinned = false, pinnedUntilMillis = null, calendarSourceKey = ""))
    }

    suspend fun pin(id: Int, until: Long?) = database.withTransaction {
        val now = System.currentTimeMillis()
        val target = getItem(id)
        if (target.isPinned && (target.pinnedUntilMillis ?: Long.MAX_VALUE) > now) {
            itemDao.updateItem(target.copy(isPinned = false, pinnedUntilMillis = null, modifiedAt = maxOf(now, target.modifiedAt + 1)))
        } else {
            require(until != null && until > now && !target.isTemplate)
            itemDao.getItems().filter { it.isPinned }.forEach {
                itemDao.updateItem(it.copy(isPinned = false, pinnedUntilMillis = null, modifiedAt = maxOf(now, it.modifiedAt + 1)))
            }
            itemDao.updateItem(target.copy(isPinned = true, pinnedUntilMillis = until, modifiedAt = maxOf(now, target.modifiedAt + 1)))
        }
    }

    suspend fun expirePins() {
        val now = System.currentTimeMillis()
        val expired = itemDao.getItems().filter { it.isPinned && (it.pinnedUntilMillis ?: Long.MAX_VALUE) <= now }
        if (expired.isEmpty()) return
        database.withTransaction {
            expired.forEach { previous ->
                val current = getItem(previous.id)
                if (current.isPinned && (current.pinnedUntilMillis ?: Long.MAX_VALUE) <= now)
                    itemDao.updateItem(current.copy(isPinned = false, pinnedUntilMillis = null, modifiedAt = maxOf(now, current.modifiedAt + 1)))
            }
        }
    }

    suspend fun move(id: Int, delta: Int) = database.withTransaction {
        val ordered = itemDao.getItems().sortedWith(compareBy<ItemEntity> { it.manualOrder }.thenBy { it.id }).toMutableList()
        val index = ordered.indexOfFirst { it.id == id }
        val destination = index + delta
        if (index < 0 || destination !in ordered.indices) return@withTransaction
        val moving = ordered.removeAt(index)
        ordered.add(destination, moving)
        val now = System.currentTimeMillis()
        ordered.forEachIndexed { position, item ->
            if (item.manualOrder != position.toLong())
                itemDao.updateItem(item.copy(manualOrder = position.toLong(), modifiedAt = maxOf(now, item.modifiedAt + 1)))
        }
    }

}
