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

    public EntityItemInfo(String startDay, String endDay, String summery) {
        this.startDay = startDay;
        this.endDay = endDay;
        this.summery = summery;
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
}
