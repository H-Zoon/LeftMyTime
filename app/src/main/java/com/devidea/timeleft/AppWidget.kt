package com.devidea.timeleft

import com.devidea.timeleft.EntityItemInfo
import com.devidea.timeleft.MainActivity
import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.appwidget.AppWidgetManager
import androidx.room.Room
import com.devidea.timeleft.AppDatabase
import com.devidea.timeleft.AppWidget
import android.app.PendingIntent
import android.app.AlarmManager
import android.widget.RemoteViews
import com.devidea.timeleft.R
import com.devidea.timeleft.AdapterItem
import androidx.room.Database
import com.devidea.timeleft.EntityWidgetInfo
import androidx.room.RoomDatabase
import com.devidea.timeleft.DatabaseDao
import androidx.room.Dao
import com.devidea.timeleft.InterfaceItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.devidea.timeleft.TopRecyclerView
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import me.relex.circleindicator.CircleIndicator2
import android.content.DialogInterface
import com.devidea.timeleft.CreateTimeActivity
import com.devidea.timeleft.CreateMonthActivity
import android.widget.Toast
import android.os.Looper
import com.devidea.timeleft.BottomRecyclerView
import com.devidea.timeleft.ItemGenerate
import androidx.room.PrimaryKey
import android.view.ViewGroup
import android.view.LayoutInflater
import android.animation.ObjectAnimator
import android.widget.ProgressBar
import android.app.Activity
import android.widget.Spinner
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.util.SparseBooleanArray
import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.widget.EditText
import com.devidea.timeleft.ItemSave
import android.app.TimePickerDialog.OnTimeSetListener
import android.widget.TimePicker
import android.app.TimePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.widget.DatePicker
import android.app.DatePickerDialog
import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.lang.Exception

class AppWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        val appWidgetIds: IntArray
        Log.d("widget", "onReceive() action = $action")
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE == action) {

            appWidgetIds = MainActivity.Companion.appDatabase!!.DatabaseDao().get()!!
            if (appWidgetIds != null && appWidgetIds.size > 0) {
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
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
        alarmManager.cancel(pendingIntent) //알람 해제
        pendingIntent.cancel() //인텐트 해제
        Log.d("widget", "alert off")
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        MainActivity.Companion.appDatabase!!.DatabaseDao().delete(appWidgetIds[0])

        Log.d("widget", "onDeleted done")
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            var type: String? = null
            try {
                type = MainActivity.Companion.appDatabase!!.DatabaseDao().getType(appWidgetId)
            } catch (e: Exception) {
                e.printStackTrace()
                if (MainActivity.Companion.appDatabase == null) {
                    MainActivity.Companion.appDatabase =
                        Room.databaseBuilder(context, AppDatabase::class.java, "ItemData")
                            .allowMainThreadQueries()
                            .build()
                    type = MainActivity.Companion.appDatabase!!.DatabaseDao().getType(appWidgetId)
                }
            }
            val views = RemoteViews(context.packageName, R.layout.app_widget)
            val intentR = Intent(context, AppWidget::class.java)
            intentR.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val pendingIntent =
                PendingIntent.getBroadcast(context, 0, intentR, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.refrash, pendingIntent)
            val appIntent = Intent(context, MainActivity::class.java)
            val pe = PendingIntent.getActivity(context, 0, appIntent, 0)
            views.setOnClickPendingIntent(R.id.percent, pe)

            if (type != null) {
                when (type) {
                    "embedYear" -> {
                        views.setTextViewText(
                            R.id.summery,
                            MainActivity.Companion.ITEM_GENERATE.yearItem().summery
                        )
                        views.setTextViewText(
                            R.id.percent,
                            MainActivity.Companion.ITEM_GENERATE.yearItem().percentString + "%"
                        )
                        views.setProgressBar(
                            R.id.progress,
                            100,
                            MainActivity.Companion.ITEM_GENERATE.yearItem().percentString
                            !!.toFloat() as Int,
                            false
                        )
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    "embedMonth" -> {
                        views.setTextViewText(
                            R.id.summery,
                            MainActivity.Companion.ITEM_GENERATE.monthItem().summery
                        )
                        views.setTextViewText(
                            R.id.percent,
                            MainActivity.Companion.ITEM_GENERATE.monthItem()
                                .percentString + "%"
                        )
                        views.setProgressBar(
                            R.id.progress,
                            100,
                            MainActivity.Companion.ITEM_GENERATE.monthItem().percentString
                            !!.toFloat() as Int,
                            false
                        )
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    "embedTime" -> {
                        views.setTextViewText(
                            R.id.summery,
                            MainActivity.Companion.ITEM_GENERATE.timeItem().summery
                        )
                        views.setTextViewText(
                            R.id.percent,
                            MainActivity.Companion.ITEM_GENERATE.timeItem().percentString + "%"
                        )
                        views.setProgressBar(
                            R.id.progress,
                            100,
                            MainActivity.Companion.ITEM_GENERATE.timeItem().percentString
                            !!.toFloat() as Int,
                            false
                        )
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    else -> {
                        val adapterItem: AdapterItem

                        //widgetID를 통해 TypeID 검색후 getSelectItem 쿼리를 통해 해당 아이템 객체 불러옴
                        val entityItemInfo: EntityItemInfo =
                            MainActivity.Companion.appDatabase!!.DatabaseDao().getSelectItem(
                                MainActivity.Companion.appDatabase!!.DatabaseDao()
                                    .getTypeID(appWidgetId)
                            )
                        adapterItem = if (type == "Time") {
                            MainActivity.Companion.ITEM_GENERATE.customTimeItem(entityItemInfo)
                        } else {
                            MainActivity.Companion.ITEM_GENERATE.customMonthItem(entityItemInfo)
                        }
                        views.setTextViewText(R.id.summery, adapterItem.summery)
                        views.setTextViewText(R.id.percent, adapterItem.percentString + "%")
                        views.setProgressBar(
                            R.id.progress,
                            100,
                            adapterItem.percentString?.toFloat() as Int,
                            false
                        )

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }

                }
            }
            //Log.d("widget", type + "update done")
        }

    }
}