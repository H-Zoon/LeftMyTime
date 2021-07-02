package com.devidea.timeleft;

import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class WidgetInfo {
    @PrimaryKey
    public int widgetID;

    public String summery;

    public WidgetInfo(int widgetID, String summery) {
        this.widgetID = widgetID;
        this.summery = summery;
    }
}