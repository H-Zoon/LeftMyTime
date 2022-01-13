package com.devidea.timeleft

import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.app.AlarmManager
import android.widget.RemoteViews
import android.content.Context
import android.os.SystemClock
import android.util.Log

class AppWidget : AppWidgetProvider() {

    private val appDatabase = AppDatabase.getInstance(App.context())

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        val appWidgetIds: IntArray
        Log.d("widget", "onReceive() action = $action")
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == action) {

            appWidgetIds = appDatabase!!.DatabaseDao().get()!!
            if (appWidgetIds.isNotEmpty()) {
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
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

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

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        alarmManager.cancel(pendingIntent) //알람 해제
        pendingIntent.cancel() //인텐트 해제
        Log.d("widget", "alert off")
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appDatabase!!.DatabaseDao().delete(appWidgetIds[0])
        Log.d("widget", "onDeleted done")
    }

    private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
    ) {
        var type: String? = null

        type = appDatabase!!.DatabaseDao().getType(appWidgetId)


        val views = RemoteViews(context.packageName, R.layout.app_widget)
        val intentR = Intent(context, AppWidget::class.java)
        intentR.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intentR, PendingIntent.FLAG_IMMUTABLE)

        views.setOnClickPendingIntent(R.id.refrash, pendingIntent)
        val appIntent = Intent(context, MainActivity::class.java)

        val pe = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.percent, pe)

        if (type != null) {
            when (type) {
                "embedYear" -> {
                    views.setTextViewText(
                            R.id.summery,
                            MainActivity.ITEM_GENERATE.yearItem().summery
                    )
                    views.setTextViewText(
                            R.id.percent,
                            MainActivity.ITEM_GENERATE.yearItem().percentString + "%"
                    )
                    views.setProgressBar(
                            R.id.progress,
                            100,
                            MainActivity.ITEM_GENERATE.yearItem().percentString
                            !!.toFloat().toInt(),
                            false
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                "embedMonth" -> {
                    views.setTextViewText(
                            R.id.summery,
                            MainActivity.ITEM_GENERATE.monthItem().summery
                    )
                    views.setTextViewText(
                            R.id.percent,
                            MainActivity.ITEM_GENERATE.monthItem()
                                    .percentString + "%"
                    )
                    views.setProgressBar(
                            R.id.progress,
                            100,
                            MainActivity.ITEM_GENERATE.monthItem().percentString
                            !!.toFloat().toInt(),
                            false
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                "embedTime" -> {
                    views.setTextViewText(
                            R.id.summery,
                            MainActivity.ITEM_GENERATE.timeItem().summery
                    )
                    views.setTextViewText(
                            R.id.percent,
                            MainActivity.ITEM_GENERATE.timeItem().percentString + "%"
                    )
                    views.setProgressBar(
                            R.id.progress,
                            100,
                            MainActivity.ITEM_GENERATE.timeItem().percentString
                            !!.toFloat().toInt(),
                            false
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                else -> {
                    val adapterItem: AdapterItem

                    //widgetID를 통해 TypeID 검색후 getSelectItem 쿼리를 통해 해당 아이템 객체 불러옴
                    val entityItemInfo: EntityItemInfo =
                            appDatabase.DatabaseDao().getSelectItem(
                                    appDatabase.DatabaseDao()
                                            .getTypeID(appWidgetId)
                            )
                    adapterItem = if (type == "Time") {
                        MainActivity.ITEM_GENERATE.customTimeItem(entityItemInfo)
                    } else {
                        MainActivity.ITEM_GENERATE.customMonthItem(entityItemInfo)
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