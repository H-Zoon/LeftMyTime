package com.devidea.timeleft

import androidx.room.Room
import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [EntityWidgetInfo::class, EntityItemInfo::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun DatabaseDao(): DatabaseDao

    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context): AppDatabase? {
            if (instance == null){
                synchronized(AppDatabase::class){
                    instance = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "appdatabase"
                    ).allowMainThreadQueries()
                .build()
                }
            }
            return instance
        }

    }
}