package com.example.myapplication;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DatabaseDao {
    @Insert
    void insertAll(WidgetInfo widgetInfo);

    @Query("SELECT widgetID FROM WidgetInfo")
    int[] get();

    @Query("SELECT summery FROM WidgetInfo WHERE widgetID = :ID")
    String get_summery(int ID);



    @Query("DELETE FROM WidgetInfo WHERE widgetID = :ID")
    void delete(int ID);





}
