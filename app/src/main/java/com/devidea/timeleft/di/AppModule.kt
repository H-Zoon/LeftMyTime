package com.devidea.timeleft.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.devidea.timeleft.InterfaceItem
import com.devidea.timeleft.ItemGenerate
import com.devidea.timeleft.database.AppDatabase
import com.devidea.timeleft.database.itemdata.ItemDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            // Schemas v1-v4 were never exported, so only those legacy installs fall
            // back to recreation. v5-v7 preserve rows when their base columns are intact.
            .fallbackToDestructiveMigrationFrom(dropAllTables = false, 1, 2, 3, 4)
            .addMigrations(
                AppDatabase.MIGRATION_5_7,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8
            )
            .build()

    @Provides
    fun provideItemDao(database: AppDatabase): ItemDao = database.itemDao()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GeneratorModule {

    @Binds
    abstract fun bindItemGenerator(impl: ItemGenerate): InterfaceItem
}
