package com.devidea.timeleft.database

import androidx.room.Room
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.devidea.timeleft.database.itemdata.ItemDao
import com.devidea.timeleft.database.itemdata.ItemEntity

@Database(
    entities = [ItemEntity::class],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        // v5 → v6: drops alarmFlag/alarmRate/weekendAlarm; adds category/colorKey/iconKey/reminderOffsetDays.
        // Date items preserve "alarm N days before end" as reminderOffsetDays = alarmRate.
        // Time items can't be mapped cleanly (v5 used hours, v6 uses minutes) — disabled and re-set by user.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ItemEntity_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `startValue` TEXT NOT NULL,
                        `endValue` TEXT NOT NULL,
                        `updateFlag` INTEGER NOT NULL,
                        `updateRate` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `colorKey` TEXT NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `reminderOffsetDays` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `ItemEntity_new` (
                        id, type, title, startValue, endValue, updateFlag, updateRate,
                        category, colorKey, iconKey, reminderOffsetDays
                    )
                    SELECT
                        id, type, title, startValue, endValue, updateFlag, updateRate,
                        '' AS category,
                        'auto' AS colorKey,
                        'event' AS iconKey,
                        CASE
                            WHEN type = 'Month' AND alarmFlag = 1 THEN alarmRate
                            ELSE -1
                        END AS reminderOffsetDays
                    FROM `ItemEntity`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `ItemEntity`")
                db.execSQL("ALTER TABLE `ItemEntity_new` RENAME TO `ItemEntity`")
            }
        }

        @Synchronized
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "app_database"
                )
                    // v1–v4 schemas were never exported, so we cannot write proper migrations
                    // for them. v5 has a defined migration to v6 below.
                    // Any future schema change (v6 → v7 …) must add an explicit Migration.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
                    .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance

                instance
            }

        }
    }
}
