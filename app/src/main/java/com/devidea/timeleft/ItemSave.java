package com.devidea.timeleft;

import java.time.LocalDate;
import java.time.LocalTime;


import static com.devidea.timeleft.MainActivity.appDatabase;

public class ItemSave {

    public void saveMonthItem(String summery, int end, boolean autoUpdate) {

        EntityItemInfo entityItemInfo = new EntityItemInfo("Month", String.valueOf(LocalDate.now()), String.valueOf(LocalDate.now().plusDays(end)), summery, autoUpdate);
        appDatabase.DatabaseDao().saveItem(entityItemInfo);

    }

    public void saveTimeItem(String summery, LocalTime startValue, LocalTime endValue, boolean autoUpdate) {

        EntityItemInfo entityItemInfo = new EntityItemInfo("Time", String.valueOf(startValue), String.valueOf(endValue), summery, autoUpdate);
        appDatabase.DatabaseDao().saveItem(entityItemInfo);

    }

}
