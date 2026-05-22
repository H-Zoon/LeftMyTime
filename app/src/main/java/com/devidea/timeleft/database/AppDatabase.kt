package com.devidea.timeleft.database

import androidx.room.Room
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

        @Synchronized
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "app_database"
                )
                    // Pre-v6 schemas were never exported, so we cannot write proper migrations
                    // for them. Allow destructive fallback ONLY from those legacy versions.
                    // Any future schema change (v6 → v7 …) must add an explicit Migration.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5)
                    .build()
                INSTANCE = instance

                instance
            }

        }
    }
}
