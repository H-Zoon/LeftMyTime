package com.devidea.timeleft;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class EntityWidgetInfo {
    @PrimaryKey
    public int widgetID;

    public int typeID;
    public String type;

    public EntityWidgetInfo(int widgetID, int typeID, String type) {
        this.widgetID = widgetID;
        this.typeID = typeID;
        this.type = type;
    }

}