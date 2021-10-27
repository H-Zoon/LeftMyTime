package com.devidea.timeleft;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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

    @Query("SELECT * FROM EntityItemInfo WHERE id = :ID")
    EntityItemInfo getSelectItem(int ID);

    @Query("UPDATE EntityItemInfo SET startValue = :updateStart, endValue = :updateEnd WHERE id = :ID")
    void updateItem(String updateStart, String updateEnd, int ID);

    //위젯엔티티

    @Insert
    void saveWidget(EntityWidgetInfo entityWidgetInfo);

    @Query("SELECT widgetID FROM EntityWidgetInfo")
    int[] get();

    @Query("SELECT type FROM EntityWidgetInfo WHERE widgetID = :ID")
    String getType(int ID);

    @Query("SELECT ItemID FROM EntityWidgetInfo WHERE widgetID = :ID")
    int getTypeID(int ID);

    @Query("DELETE FROM EntityWidgetInfo WHERE widgetID = :ID")
    void delete(int ID);

    @Query("DELETE FROM EntityWidgetInfo WHERE ItemID = :ID")
    void deleteCustomWidget(int ID);


}
