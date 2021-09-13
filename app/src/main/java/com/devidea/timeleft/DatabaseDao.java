package com.devidea.timeleft;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface DatabaseDao {

    @Insert
    void saveItem(EntityItemInfo entityItemInfo);

    @Query("SELECT * FROM EntityItemInfo")
    List<EntityItemInfo> getItem();

    @Query("DELETE FROM EntityItemInfo WHERE id = :ID")
    void deleteItem(int ID);

    @Insert
    void saveWidget(EntityWidgetInfo entityWidgetInfo);

    @Query("SELECT widgetID FROM EntityWidgetInfo")
    int[] get();

    @Query("SELECT summery FROM EntityWidgetInfo WHERE widgetID = :ID")
    String get_summery(int ID);


    @Query("DELETE FROM EntityWidgetInfo WHERE widgetID = :ID")
    void delete(int ID);


}
