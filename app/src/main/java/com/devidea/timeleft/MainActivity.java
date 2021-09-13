package com.devidea.timeleft;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.Button;

import java.text.ParseException;
import java.util.ArrayList;

import me.relex.circleindicator.CircleIndicator2;

public class MainActivity extends AppCompatActivity {

    //recyclerView 관련 객체
    private CustomAdapter adapter;
    private RecyclerView recyclerView;
    private final ArrayList<AdapterItem> adapterItemListArray = new ArrayList<>();;

    public static final TimeInfoYear timeInfoYear = new TimeInfoYear();
    public static final TimeInfoMonth timeInfoMonth= new TimeInfoMonth();
    public static final TimeInfoTime timeInfoTime = new TimeInfoTime();

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

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        //recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false)); // 상하 스크롤 //
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)) ; // 좌우 스크롤 //

        // PagerSnapHelper 추가 꼭 공부하기 !!
        PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
        pagerSnapHelper.attachToRecyclerView(recyclerView);

        //인디케이터 활성화 꼭 공부하기!!
        CircleIndicator2 indicator = findViewById(R.id.indicator);
        indicator.attachToRecyclerView(recyclerView, pagerSnapHelper);

        adapterItemListArray.add(timeInfoYear.setTimeItem());
        adapterItemListArray.add(timeInfoMonth.setTimeItem());
        adapterItemListArray.add(timeInfoTime.setTimeItem());


        ItemGenerator itemGenerator = new ItemGenerator();
        try {
            if(appDatabase.DatabaseDao().getItem().size()!=0) {
                for(int i = 0; i<appDatabase.DatabaseDao().getItem().size(); i++) {
                    itemGenerator.calDate(appDatabase.DatabaseDao().getItem().get(i));
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

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
                            adapter.notifyItemChanged(adapter.getItemCount(), timeInfoTime.setTimeItem().getPercentString()); // 리사이클러뷰 payload 호출
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

        Button button = findViewById(R.id.time_add);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CreateItemActivity.class);
                startActivity(intent);

            }
        });
    }

}