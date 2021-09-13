package com.devidea.timeleft;

import android.annotation.SuppressLint;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

import static com.devidea.timeleft.MainActivity.appDatabase;

public class ItemGenerator {

    public void genDate(String summery, int endDay){
        SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");

        Date time = new Date();	 //현재 날짜
        Calendar cal = Calendar.getInstance();	 //날짜 계산을 위해 Calendar 추상클래스 선언 getInstance()메소드 사용
        cal.setTime(time);
        cal.add(Calendar.DATE, endDay);

        Log.d("srt", transFormat.format(time));
        Log.d("end", transFormat.format(cal.getTime()).toString());
        Log.d("summery", summery);
        String startday = transFormat.format(time);
        String enddat = transFormat.format(cal.getTime());
        EntityItemInfo entityItemInfo = new EntityItemInfo(startday, enddat, summery);
        appDatabase.DatabaseDao().saveItem(entityItemInfo);

    }

    public void calDate(EntityItemInfo itemInfo) throws ParseException {

        @SuppressLint("SimpleDateFormat") SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");

        Date startDate = transFormat.parse(itemInfo.getStartDay());
        Date endDate = transFormat.parse(itemInfo.getEndDay());
        Date today = new Date();

        Long setDay = ((endDate.getTime()-startDate.getTime()) / (24*60*60*1000));
        Long leftDay = ((startDate.getTime() - today.getTime()) / (24*60*60*1000));


        String percent = String.valueOf(leftDay/setDay*100);

        Log.d("setDay", String.valueOf(setDay));
        Log.d("leftDay", String.valueOf(leftDay));
        Log.d("total%", percent);
    }


}
