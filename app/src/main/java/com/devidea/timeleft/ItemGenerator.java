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

    public void saveItem(String summery, int end){
        SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");

        Date time = new Date();	 //현재 날짜
        Calendar cal = Calendar.getInstance();	 //날짜 계산을 위해 Calendar 추상클래스 선언 getInstance()메소드 사용
        cal.setTime(time);
        cal.add(Calendar.DATE, end);

        Log.d("srt", transFormat.format(time));
        Log.d("end", transFormat.format(cal.getTime()).toString());
        Log.d("summery", summery);
        String startDay = transFormat.format(time);
        String endDay = transFormat.format(cal.getTime());
        EntityItemInfo entityItemInfo = new EntityItemInfo(startDay, endDay, summery);
        appDatabase.DatabaseDao().saveItem(entityItemInfo);

    }

    public AdapterItem calDate(EntityItemInfo itemInfo) throws ParseException {

        AdapterItem adapterItem = new AdapterItem();

        @SuppressLint("SimpleDateFormat") SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");

        Date startDate = transFormat.parse(itemInfo.getStartDay());
        Date endDate = transFormat.parse(itemInfo.getEndDay());
        Date today = new Date();

        Long setDay = ((endDate.getTime()-startDate.getTime()) / (24*60*60*1000));
        Long leftDay = ((today.getTime() - startDate.getTime()) / (24*60*60*1000));

        float MonthPercent = (float) leftDay/setDay*100;

        Log.d("total%", String.format(Locale.getDefault(), "%.1f", MonthPercent));

        adapterItem.setStartDay(transFormat.format(startDate));
        adapterItem.setEndDay(transFormat.format(endDate));
        adapterItem.setLeftDay(String.valueOf(setDay));

        adapterItem.setPercentString(String.format(Locale.getDefault(), "%.1f", MonthPercent));
        adapterItem.setSummery(itemInfo.getSummery());
        adapterItem.setId(itemInfo.getId());

        return adapterItem;
    }


}
