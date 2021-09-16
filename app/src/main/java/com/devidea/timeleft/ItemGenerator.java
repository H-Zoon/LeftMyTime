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

    public void saveItem(String summery, int end, boolean autoUpdate) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");

        Date time = new Date();     //현재 날짜
        Calendar cal = Calendar.getInstance();     //날짜 계산을 위해 Calendar 추상클래스 선언 getInstance()메소드 사용
        cal.setTime(time);
        cal.add(Calendar.DATE, end);

        Log.d("save_srt", transFormat.format(time));
        Log.d("save_end", transFormat.format(cal.getTime()).toString());
        Log.d("summery", summery);
        Log.d("auto", String.valueOf(autoUpdate));
        String startDay = transFormat.format(time);
        String endDay = transFormat.format(cal.getTime());
        //todo : boolean값은 수정해야 합니다.
        EntityItemInfo entityItemInfo = new EntityItemInfo(startDay, endDay, summery, autoUpdate);
        appDatabase.DatabaseDao().saveItem(entityItemInfo);

    }

    public AdapterItem calDate(EntityItemInfo itemInfo) throws ParseException {

        AdapterItem adapterItem = new AdapterItem();

        @SuppressLint("SimpleDateFormat") SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");

        Date startDate = transFormat.parse(itemInfo.getStartDay());
        Date endDate = transFormat.parse(itemInfo.getEndDay());
        String todayString = transFormat.format(new Date());
        Date today = transFormat.parse(todayString);

        int setDay = (int) ((endDate.getTime() - startDate.getTime()) / (24 * 60 * 60 * 1000));
        int sendDay = (int) ((today.getTime() - startDate.getTime()) / (24 * 60 * 60 * 1000));
        int leftDay = (int) ((endDate.getTime() - today.getTime()) / (24 * 60 * 60 * 1000));

        if (today.compareTo(endDate) > 0) {
            if (itemInfo.isAutoUpdate()) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(endDate);
                cal.add(Calendar.DATE, setDay);
                String updateStart = transFormat.format(endDate);
                String updateEnd = transFormat.format(cal.getTime());
                int id = itemInfo.getId();
                appDatabase.DatabaseDao().updateItem(updateStart, updateEnd, id);
                Log.d("update", String.valueOf(setDay));

                float MonthPercent = (float) sendDay / setDay * 100;

                Log.d("total%", String.format(Locale.getDefault(), "%.1f", MonthPercent));

                adapterItem.setStartDay(transFormat.format(startDate));
                adapterItem.setEndDay(transFormat.format(endDate));
                adapterItem.setLeftDay(String.valueOf(leftDay));

                adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));

            } else {
                adapterItem.setStartDay(transFormat.format(startDate));
                adapterItem.setEndDay(transFormat.format(endDate));
                adapterItem.setLeftDay(String.valueOf(leftDay));

                adapterItem.setPercentString("100");

            }

        } else {
            float MonthPercent = (float) sendDay / setDay * 100;

            Log.d("total%", String.format(Locale.getDefault(), "%.1f", MonthPercent));

            adapterItem.setStartDay(transFormat.format(startDate));
            adapterItem.setEndDay(transFormat.format(endDate));
            adapterItem.setLeftDay(String.valueOf(leftDay));

            adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));
        }
        adapterItem.setAutoUpdate(itemInfo.isAutoUpdate());
        adapterItem.setSummery(itemInfo.getSummery());
        adapterItem.setId(itemInfo.getId());

        return adapterItem;
    }


}
