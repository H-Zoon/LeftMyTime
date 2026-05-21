package com.devidea.timeleft.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.App
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.activity.MainActivity.Companion.prefs
import com.devidea.timeleft.database.AppDatabase
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.repository.TimeLeftRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds?.forEach { id ->
            prefs.edit()
                .remove(id.toString())
                .remove("${id}option")
                .apply()
        }
    }

    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {

        val views = RemoteViews(context.packageName, R.layout.app_widget)

        val updateIntent = Intent(context, AppWidget::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        val updatePendingIntent =
            PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_IMMUTABLE)

        views.setOnClickPendingIntent(R.id.refresh, updatePendingIntent)

        val activityPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.percent, activityPendingIntent)
        when (prefs.getString(appWidgetId.toString(), "")) {
            "embedYear" -> {
                val item = MainActivity.ITEM_GENERATE.yearItem()
                views.setTextViewText(
                    R.id.summary,
                    item.title
                )
                if(prefs.getBoolean(appWidgetId.toString() + "option", false)){
                    views.setTextViewText(
                        R.id.percent,
                        item.leftString
                    )
                }else {
                    views.setTextViewText(
                        R.id.percent,
                        item.percent.toString() + "%"
                    )
                }
                views.setProgressBar(
                    R.id.progress,
                    100,
                    item.percent.toInt(),
                    false
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
            "embedMonth" -> {
                val item = MainActivity.ITEM_GENERATE.monthItem()
                views.setTextViewText(
                    R.id.summary,
                    item.title
                )
                if(prefs.getBoolean(appWidgetId.toString() + "option", false)){
                    views.setTextViewText(
                        R.id.percent,
                        item.leftString
                    )
                }else {
                    views.setTextViewText(
                        R.id.percent,
                        item.percent.toString() + "%"
                    )
                }
                views.setProgressBar(
                    R.id.progress,
                    100,
                    item.percent.toInt(),
                    false
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
            "embedTime" -> {
                val item = MainActivity.ITEM_GENERATE.timeItem()
                views.setTextViewText(
                    R.id.summary,
                    item.title
                )
                if(prefs.getBoolean(appWidgetId.toString() + "option", false)){
                    views.setTextViewText(
                        R.id.percent,
                        item.widgetString
                    )
                }else {
                    views.setTextViewText(
                        R.id.percent,
                        item.percent.toString() + "%"
                    )
                }
                views.setProgressBar(
                    R.id.progress,
                    100,
                    item.percent.toInt(),
                    false
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            else -> customWidgetInit(views, appWidgetManager, appWidgetId)
        }
    }

    private fun customWidgetInit(views: RemoteViews, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item : AdapterItem?
                val repository = TimeLeftRepository(AppDatabase.getDatabase(App.context()).itemDao())
                val itemList = repository.advanceExpiredRecurrence(
                    repository.getItem(prefs.getString(appWidgetId.toString(), "0")!!.toInt())
                )

                item = if (itemList.type == ItemType.Time) {
                    MainActivity.ITEM_GENERATE.customTimeItem(itemList)

                } else {
                    MainActivity.ITEM_GENERATE.customMonthItem(itemList)

                }

                views.setTextViewText(R.id.summary, item.title)
                if(prefs.getBoolean(appWidgetId.toString() + "option", false)){
                    if (itemList.type == ItemType.Time) {
                        views.setTextViewText(
                            R.id.percent,
                            item.widgetString
                        )
                    }else {
                        views.setTextViewText(
                            R.id.percent,
                            item.leftString
                        )
                    }
                }else {
                    views.setTextViewText(
                        R.id.percent,
                        item.percent.toString() + "%"
                    )
                }
                views.setProgressBar(
                    R.id.progress,
                    100,
                    item.percent.toInt(),
                    false
                )

                appWidgetManager.updateAppWidget(appWidgetId, views)

            }catch (e : NullPointerException){
                with(prefs.edit()) {
                    remove(appWidgetId.toString())
                    apply()
                }
            }
        }
    }

}

