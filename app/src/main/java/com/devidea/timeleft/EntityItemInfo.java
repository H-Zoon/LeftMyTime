package com.devidea.timeleft;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EntityItemInfo {
    @PrimaryKey(autoGenerate = true)
    int id = 0;

    public String type;
    public String startValue;
    public String endValue;
    public String summery;
    public boolean autoUpdate;

    public EntityItemInfo(String type, String startValue, String endValue, String summery, boolean autoUpdate) {
        this.type = type;
        this.startValue = startValue;
        this.endValue = endValue;
        this.summery = summery;
        this.autoUpdate = autoUpdate;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getStartValue() {
        return startValue;
    }

    public String getEndValue() {
        return endValue;
    }

    public String getSummery() {
        return summery;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }
}
