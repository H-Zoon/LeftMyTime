package com.devidea.timeleft.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
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
        AppDatabase.getDatabase(context)

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