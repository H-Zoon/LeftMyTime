package com.devidea.timeleft;

import android.annotation.SuppressLint;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
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

    public void saveTimeItem(){

        Date time = new Date();     //현재 날짜
        Calendar cal = Calendar.getInstance();     //날짜 계산을 위해 Calendar 추상클래스 선언 getInstance()메소드 사용
        //cal.setTime(time);
        cal.add(Calendar.HOUR, 6);

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
