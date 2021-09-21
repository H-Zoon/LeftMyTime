package com.devidea.timeleft;

import android.annotation.SuppressLint;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.devidea.timeleft.MainActivity.appDatabase;

public class ItemGenerator {

    public void saveMonthItem(String summery, int end, boolean autoUpdate) {

        LocalDate time = LocalDate.now();     //현재 날짜
        LocalDate addTime = LocalDate.now().plusDays(end);

        String startValue = String.valueOf(time);
        String endValue = String.valueOf(addTime);

        //todo : type값 가변형으로 변경.
        EntityItemInfo entityItemInfo = new EntityItemInfo("month", startValue, endValue, summery, autoUpdate);
        appDatabase.DatabaseDao().saveItem(entityItemInfo);

    }

    public void saveTimeItem(String summery, LocalTime startValue, LocalTime endValue, boolean autoUpdate){

        EntityItemInfo entityItemInfo = new EntityItemInfo("time", String.valueOf(startValue), String.valueOf(endValue), summery, autoUpdate);
        appDatabase.DatabaseDao().saveItem(entityItemInfo);

    }

    public AdapterItem generateTimeItem(EntityItemInfo itemInfo) {

        AdapterItem adapterItem = new AdapterItem();

        LocalTime startValue = LocalTime.parse(itemInfo.getStartValue()); //시작시간
        LocalTime endValue = LocalTime.parse(itemInfo.getEndValue()); // 종료시간
        LocalTime time = LocalTime.now(); //현재 시간

        adapterItem.setAutoUpdate(itemInfo.isAutoUpdate());
        adapterItem.setSummery(itemInfo.getSummery());
        adapterItem.setStartDay(String.valueOf(startValue));
        adapterItem.setEndDay(String.valueOf(endValue));
        adapterItem.setId(itemInfo.getId());

        if(time.isAfter(startValue) && time.isBefore(endValue)){
            float range = Duration.between(startValue,endValue).getSeconds();
            float sendTime = Duration.between(startValue,time).getSeconds();

            adapterItem.setLeftDay(String.valueOf(LocalTime.ofSecondOfDay(Duration.between(time,endValue).getSeconds())));

            float timePercent = (sendTime/range)*100;

            adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", timePercent));

        }
        else {
            adapterItem.setPercentString("100");
        }

        return adapterItem;

    }



    public AdapterItem generateItem(EntityItemInfo itemInfo){

        AdapterItem adapterItem = new AdapterItem();

        LocalDate startDate = LocalDate.parse(itemInfo.getStartValue());
        LocalDate endDate = LocalDate.parse(itemInfo.getEndValue());
        LocalDate today = LocalDate.now();

        //설정일
        int setDay = endDate.getDayOfMonth()-startDate.getDayOfMonth();
        //설정일에서 지난일
        int sendDay = today.getDayOfMonth()-startDate.getDayOfMonth();
        //설정일까지 남은일
        int leftDay = endDate.getDayOfMonth()-today.getDayOfMonth();

        if (today.compareTo(endDate) > 0) {
            if (itemInfo.isAutoUpdate()) {
                String updateStart = String.valueOf(endDate);
                String updateEnd = String.valueOf(endDate.plusDays(setDay));
                int id = itemInfo.getId();
                appDatabase.DatabaseDao().updateItem(updateStart, updateEnd, id);
                Log.d("update", String.valueOf(setDay));

                float MonthPercent = (float) sendDay / setDay * 100;

                adapterItem.setStartDay(String.valueOf(startDate));
                adapterItem.setEndDay(String.valueOf(endDate));
                adapterItem.setLeftDay(String.valueOf(leftDay));

                adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));

            } else {
                adapterItem.setStartDay(String.valueOf(startDate));
                adapterItem.setEndDay(String.valueOf(endDate));
                adapterItem.setLeftDay(String.valueOf(leftDay));
                adapterItem.setPercentString("100");

            }

        } else {
            float MonthPercent = (float) sendDay / setDay * 100;

            adapterItem.setStartDay(String.valueOf(startDate));
            adapterItem.setEndDay(String.valueOf(endDate));
            adapterItem.setLeftDay(String.valueOf(leftDay));

            adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));
        }
        adapterItem.setAutoUpdate(itemInfo.isAutoUpdate());
        adapterItem.setSummery(itemInfo.getSummery());
        adapterItem.setId(itemInfo.getId());

        return adapterItem;
    }


}
