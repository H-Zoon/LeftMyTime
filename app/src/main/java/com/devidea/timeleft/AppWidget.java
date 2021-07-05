package com.devidea.timeleft;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.room.Room;

/**
 * Implementation of App Widget functionality.
 */
public class AppWidget extends AppWidgetProvider {
    private static final String TAG = "AppWidget";


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        AppDatabase db = Room.databaseBuilder(context,
                AppDatabase.class, "WidgetInfo").allowMainThreadQueries().build();
        String action = intent.getAction();
        Log.d(TAG, "onReceive() action = " + action);

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            Bundle extras = intent.getExtras();

            int[] appWidgetIds = db.DatabaseDao().get();

            Log.d(TAG, "extras is not null");
            if (appWidgetIds != null && appWidgetIds.length > 0) {
                Log.d(TAG, "appWidgetIds is not null");
                this.onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
            }


        }
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.d(TAG, "update");
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {

            Log.d(TAG, "appWidgetId is " + String.valueOf(appWidgetId));

            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Intent intent = new Intent(context, AppWidget.class);

        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 50000, pendingIntent);
        Log.d(TAG, "alert on");

    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AppWidget.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.cancel(pendingIntent);//알람 해제
        pendingIntent.cancel(); //인텐트 해제
        Log.d(TAG, "alert off");
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        AppDatabase db = Room.databaseBuilder(context,
                AppDatabase.class, "WidgetInfo").allowMainThreadQueries().build();
        db.DatabaseDao().delete(appWidgetIds[0]);
        Log.d(TAG, "onDeleted done");
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        AppDatabase db = Room.databaseBuilder(context,
                AppDatabase.class, "WidgetInfo").allowMainThreadQueries().build();
        String value = db.DatabaseDao().get_summery(appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        if (value != null) {
            Intent intentR = new Intent(context, AppWidget.class);
            intentR.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentR, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.button2, pendingIntent);

            switch (value) {
                case "year":
                    views.setTextViewText(R.id.percent_text, "Year Left " + MainActivity.get_year() + "%");
                    appWidgetManager.updateAppWidget(appWidgetId, views);

                    views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(MainActivity.get_year()), false);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                    Log.d(TAG, "year update done");
                    break;

                case "month":
                    views.setTextViewText(R.id.percent_text, "Month Left " + MainActivity.get_month() + "%");
                    appWidgetManager.updateAppWidget(appWidgetId, views);

                    views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(MainActivity.get_month()), false);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                    Log.d(TAG, "month update done");
                    break;

                case "time":

                    views.setTextViewText(R.id.percent_text, "Time Left " + MainActivity.get_time() + "%");
                    //appWidgetManager.updateAppWidget(AppWidgetId, views);

                    views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(MainActivity.get_time()), false);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                    Log.d(TAG, "time update done");
                    break;

            }

        }

        appWidgetManager.updateAppWidget(appWidgetId, views);

    }

}

