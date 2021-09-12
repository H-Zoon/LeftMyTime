package com.devidea.timeleft;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import me.relex.circleindicator.CircleIndicator2;

public class MainActivity extends AppCompatActivity {

    //recyclerView 관련 객체
    CustomAdapter adapter;
    RecyclerView recyclerView;
    ArrayList<AdapterItem> adapterItemListArray;

    //핸들러
    Handler handler;

    static AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //database 객체 초기화
        appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "WidgetInfo").allowMainThreadQueries().build();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        adapterItemListArray = new ArrayList<>();


        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        //recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false)); // 상하 스크롤 //
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)) ; // 좌우 스크롤 //

        // PagerSnapHelper 추가 꼭 공부하기 !!
        PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
        pagerSnapHelper.attachToRecyclerView(recyclerView);

        //인디케이터 활성화 꼭 공부하기!!
        CircleIndicator2 indicator = findViewById(R.id.indicator);
        indicator.attachToRecyclerView(recyclerView, pagerSnapHelper);

        adapterItemListArray.add(new TimeInfoYear().setTimeItem());
        adapterItemListArray.add(new TimeInfoMonth().setTimeItem());
        adapterItemListArray.add(new TimeInfoDttm().setTimeItem());

        adapter = new CustomAdapter(adapterItemListArray);
        recyclerView.setAdapter(adapter);

        //인디케이터 활성화 꼭 공부하기!!
        adapter.registerAdapterDataObserver(indicator.getAdapterDataObserver());

        //초단위, 현재시간 update Thread
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    //핸들러
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            adapter.notifyItemChanged(adapter.getItemCount(), getTime()); // 리사이클러뷰 payload 호출
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

    public static String getYear() {
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_Day = new SimpleDateFormat("D", Locale.KOREA);
        int day = Integer.parseInt(format_Day.format(date));

        float YearPercent = ((float) day / 365) * 100;

        return String.format(Locale.getDefault(), "%.1f", YearPercent);
    }

    public static String getMonth() {
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_month_day = new SimpleDateFormat("d", Locale.KOREA);
        int month_day = Integer.parseInt(format_month_day.format(date));
        LocalDate newDate = LocalDate.of(2021, 3, 1); //해당 달의 일수를 돌려받을때 사용합니다.
        int lengthOfMon = newDate.lengthOfMonth();

        float MonthPercent = (float) month_day / lengthOfMon * 100;

        return String.format(Locale.getDefault(), "%.1f", MonthPercent);
    }

    public static String getTime() {
        long time = System.currentTimeMillis();
        Date date = new Date(time);
        SimpleDateFormat format_hour = new SimpleDateFormat("H", Locale.KOREA);
        SimpleDateFormat format_min = new SimpleDateFormat("m", Locale.KOREA);
        SimpleDateFormat format_sec = new SimpleDateFormat("s", Locale.KOREA);
        int hour = Integer.parseInt(format_hour.format(date));
        int min = Integer.parseInt(format_min.format(date));
        int sec = Integer.parseInt(format_sec.format(date));

        float TimePercent = ((((float) hour * 3600) + (min * 60) + sec) / 86400) * 100;

        return String.format(Locale.getDefault(), "%.1f", TimePercent);
    }

}