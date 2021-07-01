package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //recyclerView 관련 객체
    CustomAdapter adapter;
    RecyclerView recyclerView;
    ArrayList<ItemList> itemListArrayList;

    //리스트 getter, setter
    ItemList itemList;
    ItemList itemList2;
    ItemList itemList3;

    //현재시간
    TextView timeView;

    //핸들러
    Handler handler;

    //위젯 구분에 사용되는 sharedPreferences
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
        editor = pref.edit();


        //현재시간 설정
        timeView = findViewById(R.id.timenow);
        timeView.setText(time_now());

        itemListArrayList = new ArrayList<>();
        ////왤까?
        itemList = new ItemList();
        itemList2 = new ItemList();
        itemList3 = new ItemList();


        recyclerView = (RecyclerView)findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,RecyclerView.VERTICAL, false)); // 상하 스크롤 //
        //recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)) ; // 좌우 스크롤 //

        itemList.setSummery("Year Left is");
        itemList.setPercent_string(get_year());
        itemListArrayList.add(itemList);

        itemList2.setSummery("Month Left is");
        itemList2.setPercent_string(get_month());
        itemListArrayList.add(itemList2);

        itemList3.setSummery("Time Left is");
        itemList3.setPercent_string(get_time());
        itemListArrayList.add(itemList3);

        adapter = new CustomAdapter(itemListArrayList);
        recyclerView.setAdapter(adapter);

        //초단위, 현재시간 update Thread
        new Thread(new Runnable() {
            @Override
            public void run() {

                while(true){
                    //핸들러
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            adapter.notifyItemChanged(adapter.getItemCount() - 1, get_time()); // 리사이클러뷰 payload 호출
                            timeView.setText(time_now()); //현재시간
                        }
                    });

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static String get_year() {
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_Day = new SimpleDateFormat("D", Locale.KOREA);
        int day = Integer.parseInt(format_Day.format(date));

        float YearPercent = ((float)day/365)*100;

        return String.format(Locale.getDefault(),"%.1f", YearPercent);
    }

    public static String get_month() {
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_month_day = new SimpleDateFormat("d", Locale.KOREA);
        int month_day = Integer.parseInt(format_month_day.format(date));
        LocalDate newDate = LocalDate.of(2021, 3,1); //해당 달의 일수를 돌려받을때 사용합니다.
        int lengthOfMon = newDate.lengthOfMonth();

        float MonthPercent = (float)month_day/lengthOfMon*100;

        return String.format(Locale.getDefault(),"%.1f", MonthPercent);
    }

    public static String get_time(){
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_hour = new SimpleDateFormat("H", Locale.KOREA);
        SimpleDateFormat format_min = new SimpleDateFormat("m", Locale.KOREA);
        SimpleDateFormat format_sec = new SimpleDateFormat("s", Locale.KOREA);
        int hour = Integer.parseInt(format_hour.format(date));
        int min = Integer.parseInt(format_min.format(date));
        int sec = Integer.parseInt(format_sec.format(date));

        float TimePercent = ((((float)hour*3600)+(min*60)+sec)/86400)*100;

        return String.format(Locale.getDefault(),"%.1f", TimePercent);
    }

    public String time_now() {
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_time = new SimpleDateFormat("HH:mm.ss", Locale.KOREA);
        return format_time.format(date);
    }
}