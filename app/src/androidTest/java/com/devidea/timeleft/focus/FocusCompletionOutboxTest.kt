package com.devidea.timeleft.focus

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.devidea.timeleft.database.AppDatabase
import com.devidea.timeleft.repository.TimeLeftRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneOffset
import java.util.UUID

class FocusCompletionOutboxTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun reopeningAfterCompletionCommitKeepsOnePendingNotice() = runBlocking {
        val name = "focus-outbox-${UUID.randomUUID()}"
        try {
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().useDatabase { database ->
                database.itemDao().saveItem(FocusSession.create("Focus", 1, 1_000_000, ZoneOffset.UTC, FocusClockReading(0, 1)))
                FocusCompletionOutbox(database).settle(1_060_000, FocusClockReading(60_000, 1))
            }
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().useDatabase { database ->
                val item = database.itemDao().getItems().single()
                assertEquals(FocusSession.COMPLETED, item.focusState)
                assertEquals(60_000L, item.focusElapsedMillis)
                assertTrue(database.focusCompletionDao().pending().single().matches(item))
                FocusCompletionOutbox(database).settle(1_120_000, FocusClockReading(120_000, 1))
                assertEquals(1, database.focusCompletionDao().pending().size)
                TimeLeftRepository(database.itemDao(), database).delete(item.id)
                assertTrue(database.focusCompletionDao().pending().isEmpty())
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun rolledBackCompletionNeverLeavesASeparateDeliveryIntent() = runBlocking {
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build().useDatabase { database ->
            database.itemDao().saveItem(FocusSession.create("Focus", 1, 1_000_000, ZoneOffset.UTC, FocusClockReading(0, 1)))
            try {
                database.withTransaction {
                    FocusCompletionOutbox(database).settle(1_060_000, FocusClockReading(60_000, 1))
                    error("Simulated interrupted transaction")
                }
            } catch (_: IllegalStateException) { }
            assertEquals(FocusSession.RUNNING, database.itemDao().getItems().single().focusState)
            assertTrue(database.focusCompletionDao().pending().isEmpty())
        }
    }

    // RoomDatabase exposes close(), but is not java.io.Closeable in the current Room version.
    private suspend fun AppDatabase.useDatabase(block: suspend (AppDatabase) -> Unit) {
        try { block(this) } finally { close() }
    }
}
