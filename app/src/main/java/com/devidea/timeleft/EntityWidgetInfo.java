package com.devidea.timeleft;

import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class EntityWidgetInfo {
    @PrimaryKey
    public int widgetID;

    public int ItemID;
    public String type;

    public EntityWidgetInfo(int widgetID, int ItemID, String type) {
        this.widgetID = widgetID;
        this.ItemID = ItemID;
        this.type = type;
    }

}