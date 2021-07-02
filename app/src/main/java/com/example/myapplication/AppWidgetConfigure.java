package com.example.myapplication;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RemoteViews;
import androidx.annotation.Nullable;
import androidx.room.Room;

public class AppWidgetConfigure extends Activity {

    public AppWidgetConfigure() {
        super();
    }

    Button B_summit;
    int value = 0;

    int AppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "WidgetInfo").allowMainThreadQueries().build();


        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);
        // Set the view layout resource to use.
        setContentView(R.layout.appwidget_configure);
        // Find the widget id from the intent.
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

                switch(value) {
                    case 0:
                        views.setTextViewText(R.id.percent_text, "Year Left " + MainActivity.get_year() + "%");
                        appWidgetManager.updateAppWidget(AppWidgetId, views);

                        views.setProgressBar(R.id.progress,100,(int)Float.parseFloat(MainActivity.get_year()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        break;
                    case 1:
                        views.setTextViewText(R.id.percent_text, "Month Left " + MainActivity.get_month() + "%");
                        appWidgetManager.updateAppWidget(AppWidgetId, views);

                        views.setProgressBar(R.id.progress,100,(int)Float.parseFloat(MainActivity.get_month()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);
                        break;
                    case 2:

                        //설정버튼
                        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                        views.setOnClickPendingIntent(R.id.button, pendingIntent);

                        //새로고침
                        Intent intentR = new Intent(context, AppWidget.class);
                        intentR.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        int[] ids = new int[]{AppWidgetId};
                        intentR.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                        PendingIntent pendingIntentR = PendingIntent.getBroadcast(context, (AppWidgetId * -1), intentR, PendingIntent.FLAG_UPDATE_CURRENT);
                        views.setOnClickPendingIntent(R.id.button2, pendingIntentR);
                        //

                        views.setTextViewText(R.id.percent_text, "Time Left " + MainActivity.get_time() + "%");
                        //appWidgetManager.updateAppWidget(AppWidgetId, views);

                        views.setProgressBar(R.id.progress,100,(int)Float.parseFloat(MainActivity.get_time()), false);
                        appWidgetManager.updateAppWidget(AppWidgetId, views);

                        WidgetInfo w = new WidgetInfo(AppWidgetId, "time");
                        db.DatabaseDao().insertAll(w);

                        Log.d("help", "myname" + AppWidgetId);

                        break;

                }
                /*
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                    }
                }).start();


                 */
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        };

        B_summit = findViewById(R.id.summit_button);
        B_summit.setOnClickListener(mOnClickListener);

        RadioGroup.OnCheckedChangeListener radioGroupButtonChangeListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId) {
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


