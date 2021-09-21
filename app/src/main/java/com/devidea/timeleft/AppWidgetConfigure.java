package com.devidea.timeleft;

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
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.ParseException;
import java.util.ArrayList;

import static com.devidea.timeleft.MainActivity.appDatabase;
import static com.devidea.timeleft.MainActivity.timeInfoTime;
import static com.devidea.timeleft.MainActivity.timeInfoMonth;
import static com.devidea.timeleft.MainActivity.timeInfoYear;

public class AppWidgetConfigure extends Activity {

    public AppWidgetConfigure() {
        super();
    }

    Button summitButton;
    String value = "0";
    int AppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    EntityWidgetInfo entityWidgetInfo;

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
                    case "year":
                        views.setTextViewText(R.id.percent_text, timeInfoYear.setTimeItem().getSummery() + timeInfoYear.setTimeItem().getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(timeInfoYear.setTimeItem().getPercentString()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, value);
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);
                        break;

                    case "month":
                        views.setTextViewText(R.id.percent_text, timeInfoMonth.setTimeItem().getSummery() + timeInfoMonth.setTimeItem().getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(timeInfoMonth.setTimeItem().getPercentString()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, value);
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);
                        break;

                    case "time":
                        views.setTextViewText(R.id.percent_text, timeInfoTime.setTimeItem().getSummery() + timeInfoTime.setTimeItem().getPercentString() + "%");
                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(timeInfoTime.setTimeItem().getPercentString()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        entityWidgetInfo = new EntityWidgetInfo(AppWidgetId, value);
                        appDatabase.DatabaseDao().saveWidget(entityWidgetInfo);
                        break;

                }

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        };

        summitButton = findViewById(R.id.summit_button);
        summitButton.setOnClickListener(mOnClickListener);

        RadioGroup.OnCheckedChangeListener radioGroupButtonChangeListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.yearButton:
                        value = "year";
                        break;
                    case R.id.monthButton:
                        value = "month";
                        break;
                    case R.id.timeButton:
                        value = "time";
                        break;
                }
            }
        };

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(radioGroupButtonChangeListener);

    }
}


