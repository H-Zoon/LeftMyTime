package com.devidea.timeleft;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.room.Room;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import me.relex.circleindicator.CircleIndicator2;

public class MainActivity extends AppCompatActivity {

    private final ArrayList<AdapterItem> adapterItemListArray = new ArrayList<>();
    private static final ArrayList<AdapterItem> CustomItemListArray = new ArrayList<>();

    private androidx.recyclerview.widget.RecyclerView recyclerView;
    private static androidx.recyclerview.widget.RecyclerView customItemRecyclerView;

    private static CustomRecyclerView customItemAdapter;

    public static final TimeInfoYear timeInfoYear = new TimeInfoYear();
    public static final TimeInfoMonth timeInfoMonth = new TimeInfoMonth();
    public static final TimeInfoTime timeInfoTime = new TimeInfoTime();
    public static final ItemGenerator itemGenerator = new ItemGenerator();
    private long backPressedTime = 0;
    public static AppDatabase appDatabase;

    private static ArrayList<Integer> position = new ArrayList<Integer>();
    private static ArrayList<Integer> itemID = new ArrayList<Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //database 객체 초기화
        appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "WidgetInfo").allowMainThreadQueries().build();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, androidx.recyclerview.widget.RecyclerView.HORIZONTAL, false)); // 좌우 스크롤 //

        adapterItemListArray.add(timeInfoTime.setTimeItem());
        adapterItemListArray.add(timeInfoMonth.setTimeItem());
        adapterItemListArray.add(timeInfoYear.setTimeItem());

        //recyclerView 관련 객체
        RecyclerView adapter = new RecyclerView(adapterItemListArray);
        recyclerView.setAdapter(adapter);

        // PagerSnapHelper 추가 꼭 공부하기 !!
        PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
        pagerSnapHelper.attachToRecyclerView(recyclerView);

        //인디케이터 활성화 꼭 공부하기!!
        CircleIndicator2 indicator = findViewById(R.id.indicator);
        indicator.attachToRecyclerView(recyclerView, pagerSnapHelper);

        //인디케이터 활성화 꼭 공부하기!!
        adapter.registerAdapterDataObserver(indicator.getAdapterDataObserver());

        //커스텀 항목에 대한 추가
        customItemRecyclerView = findViewById(R.id.recyclerview2);
        customItemRecyclerView.setLayoutManager(new LinearLayoutManager(this, androidx.recyclerview.widget.RecyclerView.VERTICAL, false)); // 상하 스크롤 //

        GetDBItem();

        Button button = findViewById(R.id.time_add);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] itemName = new String[2];
                itemName[0] = "시간범위 지정하기";
                itemName[1] = "날짜 지정하기";

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("원하시는 종류를 선택해주세요");

                builder.setItems(itemName, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                startActivity(new Intent(getApplicationContext(), CreateTimeActivity.class));
                                break;
                            case 1:
                                startActivity(new Intent(getApplicationContext(), CreateMonthActivity.class));
                                break;
                        }

                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_SHORT).show();
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        Handler handler = new Handler();
        //초단위, 현재시간 update Thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    //핸들러
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            for(int i=0; i<position.size(); i++){
                                customItemAdapter.notifyItemChanged(position.get(i), itemID.get(i));
                            }
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


    public static void GetDBItem() {
        CustomItemListArray.clear();
        position.clear();
        itemID.clear();

        if (appDatabase.DatabaseDao().getItem().size() != 0) {
            for (int i = 0; i < appDatabase.DatabaseDao().getItem().size(); i++) {
                if (appDatabase.DatabaseDao().getItem().get(i).getType().equals("Time")) {
                    CustomItemListArray.add(itemGenerator.generateTimeItem(appDatabase.DatabaseDao().getItem().get(i)));
                    position.add(i);
                    itemID.add(CustomItemListArray.get(i).getId());
                } else {
                    CustomItemListArray.add(itemGenerator.generateItem(appDatabase.DatabaseDao().getItem().get(i)));
                }

            }
        }
        //사용자가 추가한 부분의 아이템
        customItemAdapter = new CustomRecyclerView(CustomItemListArray);
        customItemRecyclerView.setAdapter(customItemAdapter);
    }

    @Override
    public void onBackPressed() {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        //뒤로가기 버튼 리스너에 쓰이는 변수
        long FINISH_INTERVAL_TIME = 2000;
        if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
            finish();
        } else {
            backPressedTime = tempTime;
            Toast.makeText(getApplicationContext(), "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }

    }

}