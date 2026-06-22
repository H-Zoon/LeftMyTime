package com.devidea.timeleft.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devidea.timeleft.database.itemdata.ItemDao
import com.devidea.timeleft.database.itemdata.ItemEntity

@Database(
    entities = [ItemEntity::class],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        const val DATABASE_NAME = "app_database"

        private fun createV7ItemTable(db: SupportSQLiteDatabase) {
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
        }

        private fun finishItemTableReplacement(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `ItemEntity`")
            db.execSQL("ALTER TABLE `ItemEntity_new` RENAME TO `ItemEntity`")
        }

        private fun itemColumns(db: SupportSQLiteDatabase): Set<String> =
            db.query("PRAGMA table_info(`ItemEntity`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                buildSet {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(nameIndex))
                    }
                }
            }

        // v5 -> v7: drops alarmFlag/alarmRate/weekendAlarm; adds category/colorKey/iconKey/reminderOffsetDays.
        // Date items preserve "alarm N days before end" as reminderOffsetDays = alarmRate.
        // Time items cannot be mapped cleanly (v5 used hours, v7 uses minutes), so they are disabled.
        internal val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createV7ItemTable(db)
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
                finishItemTableReplacement(db)
            }
        }

        // v6 was released with the seven-column schema, then inadvertently reused for the
        // expanded schema. Rebuild either variant into canonical v7 without dropping rows.
        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = itemColumns(db)
                val hasExpandedColumns = setOf(
                    "category",
                    "colorKey",
                    "iconKey",
                    "reminderOffsetDays"
                ).all(columns::contains)

                createV7ItemTable(db)
                val expandedValues = if (hasExpandedColumns) {
                    "`category`, `colorKey`, `iconKey`, `reminderOffsetDays`"
                } else {
                    "'' AS `category`, 'auto' AS `colorKey`, " +
                        "'event' AS `iconKey`, -1 AS `reminderOffsetDays`"
                }
                db.execSQL(
                    """
                    INSERT INTO `ItemEntity_new` (
                        id, type, title, startValue, endValue, updateFlag, updateRate,
                        category, colorKey, iconKey, reminderOffsetDays
                    )
                    SELECT
                        id, type, title, startValue, endValue, updateFlag, updateRate,
                        $expandedValues
                    FROM `ItemEntity`
                    """.trimIndent()
                )
                finishItemTableReplacement(db)
            }
        }

        // Some released databases report user_version=7 while retaining the original v6
        // identity/table. Preserve rows whenever the seven required base columns are intact;
        // otherwise recreate the canonical table empty and let Room write the v8 identity.
        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = itemColumns(db)
                db.execSQL("DROP TABLE IF EXISTS `ItemEntity_new`")

                if (CANONICAL_COLUMNS.all(columns::contains)) return

                createV7ItemTable(db)
                if (BASE_COLUMNS.all(columns::contains)) {
                    val category = columns.valueOrDefault("category", "''")
                    val colorKey = columns.valueOrDefault("colorKey", "'auto'")
                    val iconKey = columns.valueOrDefault("iconKey", "'event'")
                    val reminderOffsetDays = columns.valueOrDefault(
                        "reminderOffsetDays",
                        "-1"
                    )
                    db.execSQL(
                        """
                        INSERT INTO `ItemEntity_new` (
                            id, type, title, startValue, endValue, updateFlag, updateRate,
                            category, colorKey, iconKey, reminderOffsetDays
                        )
                        SELECT
                            id, type, title, startValue, endValue, updateFlag, updateRate,
                            $category, $colorKey, $iconKey, $reminderOffsetDays
                        FROM `ItemEntity`
                        """.trimIndent()
                    )
                }
                finishItemTableReplacement(db)
            }
        }

        private fun Set<String>.valueOrDefault(column: String, defaultSql: String): String =
            if (column in this) "`$column`" else "$defaultSql AS `$column`"

        private val BASE_COLUMNS = setOf(
            "id",
            "type",
            "title",
            "startValue",
            "endValue",
            "updateFlag",
            "updateRate"
        )
        private val CANONICAL_COLUMNS = BASE_COLUMNS + setOf(
            "category",
            "colorKey",
            "iconKey",
            "reminderOffsetDays"
        )
    }
}
