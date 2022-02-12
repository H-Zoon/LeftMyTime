package com.devidea.timeleft.widget

import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.app.AlarmManager
import android.widget.RemoteViews
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.App
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.R
import com.devidea.timeleft.datadase.AppDatabase
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppWidget : AppWidgetProvider() {

    private val appDatabase = AppDatabase.getDatabase(App.context())
    private val appWidgetIds: IntArray? = appDatabase.itemDao().get()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        Log.d("widget", "onReceive() action = $action")
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == action) {

            if (appWidgetIds != null) {
                onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds)
            }
        }
    }


    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            Log.d("widget", "appWidgetId is $appWidgetId")
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        val intent = Intent(context, AppWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime(),
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            pendingIntent
        )
        Log.d("widget", "alert on")
    }

    override fun onDisabled(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AppWidget::class.java)

        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        alarmManager.cancel(pendingIntent) //알람 해제
        pendingIntent.cancel() //인텐트 해제
        Log.d("widget", "alert off")
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appDatabase.itemDao().delete(appWidgetIds[0])
        Log.d("widget", "onDeleted done")
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {

        val type = appDatabase.itemDao().getType(appWidgetId)
        val views = RemoteViews(context.packageName, R.layout.app_widget)
        val updateIntent = Intent(context, AppWidget::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        val updatePendingIntent =
            PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_IMMUTABLE)

        views.setOnClickPendingIntent(R.id.refrash, updatePendingIntent)

        val activityPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.percent, activityPendingIntent)

        if (type != null) {
            when (type) {
                "embedYear" -> {
                    val year = MainActivity.ITEM_GENERATE.yearItem()
                    views.setTextViewText(
                        R.id.summery,
                        year.summery
                    )
                    views.setTextViewText(
                        R.id.percent,
                        year.percentString + "%"
                    )
                    views.setProgressBar(
                        R.id.progress,
                        100,
                        year.percentString
                        !!.toFloat().toInt(),
                        false
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                "embedMonth" -> {
                    val month = MainActivity.ITEM_GENERATE.yearItem()
                    views.setTextViewText(
                        R.id.summery,
                        month.summery
                    )
                    views.setTextViewText(
                        R.id.percent,
                        month.percentString + "%"
                    )
                    views.setProgressBar(
                        R.id.progress,
                        100,
                        month.percentString
                        !!.toFloat().toInt(),
                        false
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                "embedTime" -> {
                    val time = MainActivity.ITEM_GENERATE.yearItem()
                    views.setTextViewText(
                        R.id.summery,
                        time.summery
                    )
                    views.setTextViewText(
                        R.id.percent,
                        time.percentString + "%"
                    )
                    views.setProgressBar(
                        R.id.progress,
                        100,
                        time.percentString
                        !!.toFloat().toInt(),
                        false
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                else -> {
                    //widgetID를 통해 TypeID 검색후 getSelectItem 쿼리를 통해 해당 아이템 객체 불러옴
                    CoroutineScope(Dispatchers.IO).launch {  }
                    val itemEntity: ItemEntity =
                        appDatabase.itemDao().getSelectItem(
                            appDatabase.itemDao()
                                .getTypeID(appWidgetId)
                        )
                    val adapterItem = if (type == "Time") {
                        MainActivity.ITEM_GENERATE.customTimeItem(itemEntity)
                    } else {
                        MainActivity.ITEM_GENERATE.customMonthItem(itemEntity)
                    }
                    views.setTextViewText(R.id.summery, adapterItem.summery)
                    views.setTextViewText(R.id.percent, adapterItem.percentString + "%")
                    views.setProgressBar(
                        R.id.progress,
                        100,
                        adapterItem.percentString!!.toFloat().toInt(),
                        false
                    )

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }

            }
        }
        //Log.d("widget", type + "update done")
    }


}