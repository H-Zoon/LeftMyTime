package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class AppWidget extends AppWidgetProvider {
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        Log.d("help", "onReceive() action = " + action);

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            Bundle extras = intent.getExtras();

            if (extras != null) {
                int[] appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                Log.d("help", "extras is not null");
                if (appWidgetIds != null && appWidgetIds.length > 0) {
                    Log.d("help", "appWidgetIds is not null onReceive() action = " + action);
                    this.onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
                }
            }
        }
    }



    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.d("help", "hi");
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {

            Log.d("help", String.valueOf(appWidgetId));

            //updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Intent intent = new Intent(context, AppWidget.class);

        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, pendingIntent);
        Log.d("help", "alrton");
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AppWidget.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.cancel(pendingIntent);//알람 해제
        pendingIntent.cancel(); //인텐트 해제
        Log.d("help", "off");
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        Log.d("widget2", String.valueOf(appWidgetId));

        String value = loadTitlePref(context, appWidgetId);

        switch (value) {
            case "year":
                views.setTextViewText(R.id.percent_text, "Year Left " + MainActivity.get_year() + "%");
                // Instruct the widget manager to update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views);

                views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(MainActivity.get_year()), false);
                appWidgetManager.updateAppWidget(appWidgetId, views);
                Log.d("help", "year");
                break;

            case "month":
                views.setTextViewText(R.id.percent_text, "Month Left " + MainActivity.get_month() + "%");
                appWidgetManager.updateAppWidget(appWidgetId, views);

                views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(MainActivity.get_month()), false);
                appWidgetManager.updateAppWidget(appWidgetId, views);
                Log.d("help", "month");
                break;

            case "time":
                /*
                views.setTextViewText(R.id.percent_text, "Time Left " + MainActivity.get_time() + "%");
                appWidgetManager.updateAppWidget(appWidgetId, views);

                views.setProgressBar(R.id.progress, 100, (int) Float.parseFloat(MainActivity.get_time()), false);
                appWidgetManager.updateAppWidget(appWidgetId, views);

                //설정버튼
                //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, AppWidgetConfigure, 0);
                //views.setOnClickPendingIntent(R.id.button, pendingIntent);

                //새로고침
                Intent intent2 = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                views.setOnClickFillInIntent(R.id.button2, intent2);

                Log.d("help", "time update");

                 */
                break;
        }

    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences("WidgetConfigure", Context.MODE_PRIVATE);

        return prefs.getString(String.valueOf(appWidgetId), "time");

    }


}