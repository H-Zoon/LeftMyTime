package com.devidea.timeleft.datadase

import androidx.room.Room
import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import com.devidea.timeleft.datadase.itemdata.ItemDao
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import com.devidea.timeleft.datadase.itemdata.WidgetEntity

@Database(
    entities = [ItemEntity::class],
    version = 5,
    exportSchema = false
)
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
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                instance
            }

        }
    }
}