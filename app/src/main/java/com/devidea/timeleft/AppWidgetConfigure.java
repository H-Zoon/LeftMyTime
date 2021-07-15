package com.devidea.timeleft;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RemoteViews;

import androidx.room.Room;

import static com.devidea.timeleft.MainActivity.appDatabase;

public class AppWidgetConfigure extends Activity {

    public AppWidgetConfigure() {
        super();
    }

    Button summitButton;
    int value = 0;
    int AppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

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
                views.setOnClickPendingIntent(R.id.button2, pendingIntent);

                switch (value) {
                    case 0:
                        views.setTextViewText(R.id.percent_text, "Year Left " + MainActivity.getYear() + "%");
                        appWidgetManager.updateAppWidget(AppWidgetId, views);

                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(MainActivity.getYear()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        WidgetInfo y = new WidgetInfo(AppWidgetId, "year");
                        appDatabase.DatabaseDao().insertAll(y);
                        break;

                    case 1:
                        views.setTextViewText(R.id.percent_text, "Month Left " + MainActivity.getMonth() + "%");
                        appWidgetManager.updateAppWidget(AppWidgetId, views);

                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(MainActivity.getMonth()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        WidgetInfo m = new WidgetInfo(AppWidgetId, "month");
                        appDatabase.DatabaseDao().insertAll(m);
                        break;
                    case 2:

                        views.setTextViewText(R.id.percent_text, "Time Left " + MainActivity.getTime() + "%");
                        //appWidgetManager.updateAppWidget(AppWidgetId, views);

                        views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(MainActivity.getTime()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);

                        WidgetInfo w = new WidgetInfo(AppWidgetId, "time");
                        appDatabase.DatabaseDao().insertAll(w);

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
                        value = 0;
                        break;
                    case R.id.monthButton:
                        value = 1;
                        break;
                    case R.id.timeButton:
                        value = 2;
                        break;

                }
            }
        };

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(radioGroupButtonChangeListener);

    }
}


