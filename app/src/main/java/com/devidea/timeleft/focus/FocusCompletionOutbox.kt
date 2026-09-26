package com.devidea.timeleft.focus

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.withTransaction
import com.devidea.timeleft.database.AppDatabase
import com.devidea.timeleft.database.itemdata.ItemEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Device-local delivery state. Never included in schedule backups or synchronization. */
@Entity
data class FocusCompletionNotice(
    @PrimaryKey val itemId: Int,
    val completedAt: Long,
    val attempts: Int = 0,
    val nextAttemptAt: Long = 0,
) {
    fun matches(item: ItemEntity?): Boolean = item != null && item.id == itemId && item.deletedAt == null &&
        item.isFocusSession && item.focusState == FocusSession.COMPLETED && item.focusStoppedAt == completedAt

    fun attempted(now: Long): FocusCompletionNotice = copy(
        attempts = (attempts + 1).coerceAtMost(30),
        nextAttemptAt = now + minOf(MAX_RETRY_MILLIS, 60_000L * (1L shl attempts.coerceIn(0, 6))),
    )

    companion object { const val MAX_RETRY_MILLIS = 60 * 60_000L }
}

@Dao
interface FocusCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(notice: FocusCompletionNotice)

    @Query("SELECT * FROM FocusCompletionNotice ORDER BY completedAt, itemId")
    suspend fun pending(): List<FocusCompletionNotice>

    @Update
    suspend fun update(notice: FocusCompletionNotice)

    @Query("DELETE FROM FocusCompletionNotice WHERE itemId = :itemId AND completedAt = :completedAt")
    suspend fun acknowledge(itemId: Int, completedAt: Long)

    @Query("DELETE FROM FocusCompletionNotice WHERE itemId = :itemId")
    suspend fun removeForItem(itemId: Int)
}

@Singleton
class FocusCompletionOutbox @Inject constructor(private val database: AppDatabase) {
    /** Completion and its delivery intent commit together, including after a process restart. */
    suspend fun settle(now: Long, clock: FocusClockReading) = database.withTransaction {
        val items = database.itemDao()
        items.getItems().filter { it.focusState == FocusSession.RUNNING }.forEach { current ->
            val updated = FocusSession.settle(FocusSession.withClock(current, now, clock), now)
            if (updated != current) items.updateItem(updated)
            if (updated.focusState == FocusSession.COMPLETED) {
                database.focusCompletionDao().enqueue(FocusCompletionNotice(updated.id, requireNotNull(updated.focusStoppedAt)))
            }
        }
    }
}
