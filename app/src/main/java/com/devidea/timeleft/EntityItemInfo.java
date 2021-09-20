package com.devidea.timeleft;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EntityItemInfo {
    @PrimaryKey(autoGenerate = true)
    int id = 0;

    public String startDay;
    public String endDay;
    public String summery;
    public boolean autoUpdate;

    public EntityItemInfo(String startDay, String endDay, String summery, boolean autoUpdate) {
        this.startDay = startDay;
        this.endDay = endDay;
        this.summery = summery;
        this.autoUpdate = autoUpdate;
    }

    public int getId() {
        return id;
    }

    public String getStartDay() {
        return startDay;
    }

    public String getEndDay() {
        return endDay;
    }

    public String getSummery() {
        return summery;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }
}
