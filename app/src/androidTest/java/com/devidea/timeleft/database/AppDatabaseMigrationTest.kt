package com.devidea.timeleft.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrateOriginalVersion6_addsNewFieldsWithoutLosingRows() {
        helper.createDatabase("legacy-v6", 6).use { database ->
            database.execSQL(
                """
                INSERT INTO `ItemEntity` (
                    id, type, title, startValue, endValue, updateFlag, updateRate
                ) VALUES (1, 'Month', 'Launch', '2026-05-01', '2026-06-01', 0, 0)
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate("legacy-v6", 7, true, AppDatabase.MIGRATION_6_7)
            .use(::assertMigratedDefaultRow)
    }

    @Test
    fun migrateExpandedVersion6_preservesNewFieldValues() {
        helper.createDatabase("expanded-v6", 6).use { database ->
            database.execSQL("ALTER TABLE `ItemEntity` ADD COLUMN `category` TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE `ItemEntity` ADD COLUMN `colorKey` TEXT NOT NULL DEFAULT 'auto'")
            database.execSQL("ALTER TABLE `ItemEntity` ADD COLUMN `iconKey` TEXT NOT NULL DEFAULT 'event'")
            database.execSQL("ALTER TABLE `ItemEntity` ADD COLUMN `reminderOffsetDays` INTEGER NOT NULL DEFAULT -1")
            database.execSQL(
                """
                INSERT INTO `ItemEntity` (
                    id, type, title, startValue, endValue, updateFlag, updateRate,
                    category, colorKey, iconKey, reminderOffsetDays
                ) VALUES (
                    2, 'Month', 'Holiday', '2026-05-01', '2026-06-06', 0, 0,
                    'travel', 'blue', 'flight', 3
                )
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate("expanded-v6", 7, true, AppDatabase.MIGRATION_6_7)
            .use { database ->
                database.query(
                    "SELECT category, colorKey, iconKey, reminderOffsetDays FROM `ItemEntity` WHERE id = 2"
                ).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("travel", cursor.getString(0))
                    assertEquals("blue", cursor.getString(1))
                    assertEquals("flight", cursor.getString(2))
                    assertEquals(3, cursor.getInt(3))
                }
            }
    }

    @Test
    fun migrateVersion7WithLegacyIdentity_preservesBaseRows() {
        helper.createDatabase("legacy-marked-v7", 7).use { database ->
            database.execSQL("DROP TABLE `ItemEntity`")
            database.execSQL(
                """
                CREATE TABLE `ItemEntity` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `type` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `startValue` TEXT NOT NULL,
                    `endValue` TEXT NOT NULL,
                    `updateFlag` INTEGER NOT NULL,
                    `updateRate` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO `ItemEntity` (
                    id, type, title, startValue, endValue, updateFlag, updateRate
                ) VALUES (3, 'Month', 'Recovered', '2026-06-01', '2026-07-01', 0, 0)
                """.trimIndent()
            )
            database.execSQL(
                "UPDATE room_master_table SET identity_hash = '$LEGACY_IDENTITY_HASH' WHERE id = 42"
            )
        }

        helper.runMigrationsAndValidate(
            "legacy-marked-v7",
            8,
            true,
            AppDatabase.MIGRATION_7_8
        ).use { database ->
            database.query(
                "SELECT title, category, colorKey, iconKey, reminderOffsetDays " +
                    "FROM `ItemEntity` WHERE id = 3"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Recovered", cursor.getString(0))
                assertEquals("", cursor.getString(1))
                assertEquals("auto", cursor.getString(2))
                assertEquals("event", cursor.getString(3))
                assertEquals(-1, cursor.getInt(4))
            }
        }
    }

    @Test
    fun migrateVersion7WithCanonicalColumnsAndStaleIdentity_preservesExpandedRows() {
        helper.createDatabase("canonical-stale-v7", 7).use { database ->
            database.execSQL(
                """
                INSERT INTO `ItemEntity` (
                    id, type, title, startValue, endValue, updateFlag, updateRate,
                    category, colorKey, iconKey, reminderOffsetDays
                ) VALUES (
                    4, 'Month', 'Preserved', '2026-06-01', '2026-07-01', 0, 0,
                    'travel', 'blue', 'flight', 7
                )
                """.trimIndent()
            )
            database.execSQL(
                "UPDATE room_master_table SET identity_hash = '$LEGACY_IDENTITY_HASH' WHERE id = 42"
            )
        }

        helper.runMigrationsAndValidate(
            "canonical-stale-v7",
            8,
            true,
            AppDatabase.MIGRATION_7_8
        ).use { database ->
            database.query(
                "SELECT category, colorKey, iconKey, reminderOffsetDays " +
                    "FROM `ItemEntity` WHERE id = 4"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("travel", cursor.getString(0))
                assertEquals("blue", cursor.getString(1))
                assertEquals("flight", cursor.getString(2))
                assertEquals(7, cursor.getInt(3))
            }
        }
    }

    @Test
    fun migrateVersion7WithoutRequiredColumns_recreatesEmptyCanonicalTable() {
        helper.createDatabase("invalid-v7", 7).use { database ->
            database.execSQL("DROP TABLE `ItemEntity`")
            database.execSQL(
                "CREATE TABLE `ItemEntity` (`id` INTEGER PRIMARY KEY NOT NULL, `title` TEXT NOT NULL)"
            )
            database.execSQL("INSERT INTO `ItemEntity` (id, title) VALUES (5, 'Invalid')")
            database.execSQL(
                "UPDATE room_master_table SET identity_hash = '$LEGACY_IDENTITY_HASH' WHERE id = 42"
            )
        }

        helper.runMigrationsAndValidate(
            "invalid-v7",
            8,
            true,
            AppDatabase.MIGRATION_7_8
        ).use { database ->
            database.query("SELECT COUNT(*) FROM `ItemEntity`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private fun assertMigratedDefaultRow(database: SupportSQLiteDatabase) {
        database.query(
            "SELECT title, category, colorKey, iconKey, reminderOffsetDays FROM `ItemEntity` WHERE id = 1"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Launch", cursor.getString(0))
            assertEquals("", cursor.getString(1))
            assertEquals("auto", cursor.getString(2))
            assertEquals("event", cursor.getString(3))
            assertEquals(-1, cursor.getInt(4))
        }
    }

    companion object {
        private const val LEGACY_IDENTITY_HASH = "5243b631fd6982dc1b66c8d06278d66e"
    }
}
