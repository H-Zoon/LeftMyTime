package com.devidea.timeleft;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;

import static com.devidea.timeleft.MainActivity.ITEM_GENERATE;
import static com.devidea.timeleft.MainActivity.appDatabase;


public class AppWidgetConfigure extends Activity {

    public AppWidgetConfigure() {
        super();
    }

    Button summitButton;
    Spinner spinner;
    CheckBox checkBox;
    RadioGroup radioGroup;
    String value = "0";
    int AppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    EntityWidgetInfo entityWidgetInfo;
    Context context = AppWidgetConfigure.this;
    boolean isFirstSelected = false;

    TextView Preview;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (appDatabase == null) {
            try {
                appDatabase = Room.databaseBuilder(context, AppDatabase.class, "ItemData")
                        .allowMainThreadQueries()
                        .build();

            } catch (Exception e) {
                Toast.makeText(context, "어플리케이션 실행 후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
            }

        }

        //setResult = canceled 설정. 최종 summit 전 뒤로가기시 widget 취소
        setResult(RESULT_CANCELED);
        //위젯 레이아웃 설정
        setContentView(R.layout.appwidget_configure);
        // Intent에서 widget id 가져오기
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            AppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // If they gave us an intent without the widget id, just bail.
        if (AppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("click", value);
                final Context context = AppWidgetConfigure.this;
                // appwidget 인스턴스 가져오기
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

                RemoteViews views = new RemoteViews(context.getPackageName(),
                        R.layout.app_widget);

                //위젯에 새로고침 버튼 추가
                Intent intentR = new Intent(context, AppWidget.class);
                intentR.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentR, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.refrash, pendingIntent);

                Intent appIntent=new Intent(context, MainActivity.class);
                PendingIntent pe=PendingIntent.getActivity(context, 0, appIntent, 0);
                views.setOnClickPendingIntent(R.id.percent, pe);

                switch (value) {
                    case "embedYear":
                        views.setTextViewText(R.id.summery, ITEM_GENERATE.yearItem().getSummery());
                        views.setTextViewText(R.id.percent, ITEM_GENERATE.yearItem().getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(ITEM_GENERATE.yearItem().getPercentString()), false);

                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, -1, value);
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);
                        break;

                    case "embedMonth":
                        views.setTextViewText(R.id.summery, ITEM_GENERATE.monthItem().getSummery());
                        views.setTextViewText(R.id.percent, ITEM_GENERATE.monthItem().getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(ITEM_GENERATE.monthItem().getPercentString()), false);

                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, -1, value);
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);
                        break;

                    case "embedTime":
                        views.setTextViewText(R.id.summery, ITEM_GENERATE.timeItem().getSummery());
                        views.setTextViewText(R.id.percent, ITEM_GENERATE.timeItem().getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(ITEM_GENERATE.timeItem().getPercentString()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, -1, value);
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);
                        break;

                    case "0":
                        setResult(RESULT_CANCELED);
                        Toast.makeText(context, "취소되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                        break;

                    default:
                        EntityItemInfo entityItemInfo = appDatabase.DatabaseDao().getSelectItem(Integer.parseInt(value));
                        AdapterItem adapterItem;
                        if (entityItemInfo.getType().equals("Time")) {
                            adapterItem = ITEM_GENERATE.customTimeItem(entityItemInfo);
                        } else {
                            adapterItem = ITEM_GENERATE.customMonthItem(entityItemInfo);
                        }
                        views.setTextViewText(R.id.summery, adapterItem.getSummery());
                        views.setTextViewText(R.id.percent, adapterItem.getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(adapterItem.getPercentString()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, adapterItem.getId(), entityItemInfo.getType());
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);
                        break;

                }

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        };


        RadioGroup.OnCheckedChangeListener radioGroupButtonChangeListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Preview = findViewById(R.id.summery_preview);
                switch (checkedId) {
                    case R.id.yearButton:
                        Preview.setText(ITEM_GENERATE.yearItem().getSummery());
                        value = "embedYear";
                        break;
                    case R.id.monthButton:
                        Preview.setText(ITEM_GENERATE.monthItem().getSummery());
                        value = "embedMonth";
                        break;
                    case R.id.timeButton:
                        Preview.setText(ITEM_GENERATE.timeItem().getSummery());
                        value = "embedTime";
                        break;
                }
            }
        };

        radioGroup = findViewById(R.id.radioGroup);
        summitButton = findViewById(R.id.summit_button);
        spinner = findViewById(R.id.spinner);
        checkBox = findViewById(R.id.checkBox);

        radioGroup.setOnCheckedChangeListener(radioGroupButtonChangeListener);
        summitButton.setOnClickListener(mOnClickListener);
        spinner.setEnabled(false);

        ArrayList<String> itemName = new ArrayList<>();
        List<EntityItemInfo> entityItemInfo = appDatabase.DatabaseDao().getItem();
        for (int i = 0; i < appDatabase.DatabaseDao().getItem().size(); i++) {
            itemName.add(appDatabase.DatabaseDao().getItem().get(i).getSummery());
        }

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (appDatabase.DatabaseDao().getItem().size() != 0) {
                    if (checkBox.isChecked()) {
                        for (int i = 0; i < radioGroup.getChildCount(); i++) {
                            radioGroup.getChildAt(i).setEnabled(false);
                        }
                        value = String.valueOf(entityItemInfo.get(0).getId());

                        Preview = findViewById(R.id.summery_preview);
                        Preview.setText(entityItemInfo.get(0).getSummery());
                        spinner.setSelection(0);

                        isFirstSelected = true;
                        spinner.setEnabled(true);
                    } else {
                        for (int i = 0; i < radioGroup.getChildCount(); i++) {
                            radioGroup.getChildAt(i).setEnabled(true);
                        }
                        isFirstSelected = false;
                        spinner.setEnabled(false);
                    }
                } else {
                    checkBox.setChecked(false);
                    Toast.makeText(context, "저장된 항목이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemName);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Preview = findViewById(R.id.summery_preview);
                Preview.setText(entityItemInfo.get(position).getSummery());

                if (isFirstSelected) {
                    Preview.setText(entityItemInfo.get(position).getSummery());
                    value = String.valueOf(entityItemInfo.get(position).getId());
                    Log.d("value", value);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}


