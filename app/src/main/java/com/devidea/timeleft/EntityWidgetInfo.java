package com.devidea.timeleft;

import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class EntityWidgetInfo {
    @PrimaryKey
    public int widgetID;

    public String summery;

    public EntityWidgetInfo(int widgetID, String summery) {
        this.widgetID = widgetID;
        this.summery = summery;
    }
}