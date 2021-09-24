package com.devidea.timeleft;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.devidea.timeleft.MainActivity.appDatabase;
import static com.devidea.timeleft.MainActivity.itemGenerator;
import static com.devidea.timeleft.MainActivity.timeInfoTime;
import static com.devidea.timeleft.MainActivity.timeInfoMonth;
import static com.devidea.timeleft.MainActivity.timeInfoYear;

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

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

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

                switch (value) {
                    case "embedYear":
                        views.setTextViewText(R.id.percent_text, timeInfoYear.setTimeItem().getSummery() + timeInfoYear.setTimeItem().getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(timeInfoYear.setTimeItem().getPercentString()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, -1, value);
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);
                        break;

                    case "embedMonth":
                        views.setTextViewText(R.id.percent_text, timeInfoMonth.setTimeItem().getSummery() + timeInfoMonth.setTimeItem().getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(timeInfoMonth.setTimeItem().getPercentString()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, -1, value);
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);
                        break;

                    case "embedTime":
                        views.setTextViewText(R.id.percent_text, timeInfoTime.setTimeItem().getSummery() + timeInfoTime.setTimeItem().getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(timeInfoTime.setTimeItem().getPercentString()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, -1, value);
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);
                        break;

                    default:
                        EntityItemInfo entityItemInfo = appDatabase.DatabaseDao().getSelectItem(Integer.parseInt(value));
                        AdapterItem adapterItem;
                        if (entityItemInfo.getType().equals("Time")) {
                            adapterItem = itemGenerator.generateTimeItem(entityItemInfo);
                        } else {
                            adapterItem = itemGenerator.generateItem(entityItemInfo);
                        }
                        views.setTextViewText(R.id.percent_text, adapterItem.getSummery() +" 이(가) "+ adapterItem.getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(adapterItem.getPercentString()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, adapterItem.getId(), entityItemInfo.getType());
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);

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
                switch (checkedId) {
                    case R.id.yearButton:
                        value = "embedYear";
                        break;
                    case R.id.monthButton:
                        value = "embedMonth";
                        break;
                    case R.id.timeButton:
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
                if (isFirstSelected) {
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


