package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DatabaseDao {
    @Insert
    void insertAll(WidgetInfo widgetInfo);

    @Query("SELECT widgetID FROM WidgetInfo WHERE summery = 'time'")
    int[] get();

}
