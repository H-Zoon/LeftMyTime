package com.devidea.timeleft;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {EntityWidgetInfo.class, EntityItemInfo.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DatabaseDao DatabaseDao();
}