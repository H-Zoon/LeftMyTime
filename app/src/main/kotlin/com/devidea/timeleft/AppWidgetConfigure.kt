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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import me.relex.circleindicator.CircleIndicator2
import android.content.DialogInterface
import com.devidea.timeleft.CreateTimeActivity
import com.devidea.timeleft.CreateMonthActivity
import android.os.Looper
import com.devidea.timeleft.BottomRecyclerView
import com.devidea.timeleft.ItemGenerate
import androidx.room.PrimaryKey
import android.view.ViewGroup
import android.view.LayoutInflater
import android.animation.ObjectAnimator
import android.app.Activity
import android.util.SparseBooleanArray
import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import com.devidea.timeleft.ItemSave
import android.app.TimePickerDialog.OnTimeSetListener
import android.app.TimePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.*
import java.lang.Exception
import java.util.ArrayList

class AppWidgetConfigure constructor() : Activity() {
    var summitButton: Button? = null
    var spinner: Spinner? = null
    var checkBox: CheckBox? = null
    var radioGroup: RadioGroup? = null
    var value: String = "0"
    var AppWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    var entityWidgetInfo: EntityWidgetInfo? = null
    var context: Context = this@AppWidgetConfigure
    var isFirstSelected: Boolean = false
    var Preview: TextView? = null
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        /*
        if (MainActivity.Companion.appDatabase == null) {
            try {
                MainActivity.Companion.appDatabase =
                    Room.databaseBuilder(context, AppDatabase::class.java, "ItemData")
                        .allowMainThreadQueries()
                        .build()
            } catch (e: Exception) {
                Toast.makeText(context, "어플리케이션 실행 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
            }
        }

        //setResult = canceled 설정. 최종 summit 전 뒤로가기시 widget 취소
        setResult(RESULT_CANCELED)
        //위젯 레이아웃 설정
        setContentView(R.layout.appwidget_configure)
        // Intent에서 widget id 가져오기
        val intent: Intent = getIntent()
        val extras: Bundle? = intent.getExtras()
        if (extras != null) {
            AppWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        // If they gave us an intent without the widget id, just bail.
        if (AppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }
        val mOnClickListener: View.OnClickListener = object : View.OnClickListener {
            public override fun onClick(v: View) {
                Log.d("click", value)
                val context: Context = this@AppWidgetConfigure
                // appwidget 인스턴스 가져오기
                val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
                val views: RemoteViews = RemoteViews(
                    context.getPackageName(),
                    R.layout.app_widget
                )

                //위젯에 새로고침 버튼 추가
                val intentR: Intent = Intent(context, AppWidget::class.java)
                intentR.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                val pendingIntent: PendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intentR,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.refrash, pendingIntent)
                val appIntent: Intent = Intent(context, MainActivity::class.java)
                val pe: PendingIntent = PendingIntent.getActivity(context, 0, appIntent, 0)
                views.setOnClickPendingIntent(R.id.percent, pe)
                when (value) {
                    "embedYear" -> {
                        views.setTextViewText(
                            R.id.summery,
                            MainActivity.Companion.ITEM_GENERATE.yearItem().getSummery()
                        )
                        views.setTextViewText(
                            R.id.percent,
                            MainActivity.Companion.ITEM_GENERATE.yearItem().getPercentString() + "%"
                        )
                        views.setProgressBar(
                            R.id.progress,
                            100,
                            MainActivity.Companion.ITEM_GENERATE.yearItem().getPercentString()
                                .toFloat() as Int,
                            false
                        )
                        appWidgetManager.updateAppWidget(AppWidgetId, views)
                        entityWidgetInfo = EntityWidgetInfo(AppWidgetId, -1, value)
                        MainActivity.Companion.appDatabase!!.DatabaseDao()
                            .saveWidget(entityWidgetInfo)
                    }
                    "embedMonth" -> {
                        views.setTextViewText(
                            R.id.summery,
                            MainActivity.Companion.ITEM_GENERATE.monthItem().getSummery()
                        )
                        views.setTextViewText(
                            R.id.percent,
                            MainActivity.Companion.ITEM_GENERATE.monthItem()
                                .getPercentString() + "%"
                        )
                        views.setProgressBar(
                            R.id.progress,
                            100,
                            MainActivity.Companion.ITEM_GENERATE.monthItem().getPercentString()
                                .toFloat() as Int,
                            false
                        )
                        appWidgetManager.updateAppWidget(AppWidgetId, views)
                        entityWidgetInfo = EntityWidgetInfo(AppWidgetId, -1, value)
                        MainActivity.Companion.appDatabase!!.DatabaseDao()
                            .saveWidget(entityWidgetInfo)
                    }
                    "embedTime" -> {
                        views.setTextViewText(
                            R.id.summery,
                            MainActivity.Companion.ITEM_GENERATE.timeItem().getSummery()
                        )
                        views.setTextViewText(
                            R.id.percent,
                            MainActivity.Companion.ITEM_GENERATE.timeItem().getPercentString() + "%"
                        )
                        views.setProgressBar(
                            R.id.progress,
                            100,
                            MainActivity.Companion.ITEM_GENERATE.timeItem().getPercentString()
                                .toFloat() as Int,
                            false
                        )
                        appWidgetManager.updateAppWidget(AppWidgetId, views)
                        entityWidgetInfo = EntityWidgetInfo(AppWidgetId, -1, value)
                        MainActivity.Companion.appDatabase!!.DatabaseDao()
                            .saveWidget(entityWidgetInfo)
                    }
                    "0" -> {
                        setResult(RESULT_CANCELED)
                        Toast.makeText(context, "취소되었습니다", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    else -> {
                        val entityItemInfo: EntityItemInfo =
                            MainActivity.Companion.appDatabase!!.DatabaseDao()
                                .getSelectItem(value.toInt())
                        val adapterItem: AdapterItem
                        if ((entityItemInfo.getType() == "Time")) {
                            adapterItem =
                                MainActivity.Companion.ITEM_GENERATE.customTimeItem(entityItemInfo)
                        } else {
                            adapterItem =
                                MainActivity.Companion.ITEM_GENERATE.customMonthItem(entityItemInfo)
                        }
                        views.setTextViewText(R.id.summery, adapterItem.getSummery())
                        views.setTextViewText(R.id.percent, adapterItem.getPercentString() + "%")
                        views.setProgressBar(
                            R.id.progress,
                            100,
                            adapterItem.getPercentString().toFloat() as Int,
                            false
                        )
                        appWidgetManager.updateAppWidget(AppWidgetId, views)
                        entityWidgetInfo = EntityWidgetInfo(
                            AppWidgetId,
                            adapterItem.getId(),
                            entityItemInfo.getType()
                        )
                        MainActivity.Companion.appDatabase!!.DatabaseDao()
                            .saveWidget(entityWidgetInfo)
                    }
                }
                val resultValue: Intent = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            }
        }
        val radioGroupButtonChangeListener: RadioGroup.OnCheckedChangeListener =
            object : RadioGroup.OnCheckedChangeListener {
                public override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
                    Preview = findViewById(R.id.summery_preview)
                    when (checkedId) {
                        R.id.yearButton -> {
                            Preview.setText(
                                MainActivity.Companion.ITEM_GENERATE.yearItem().getSummery()
                            )
                            value = "embedYear"
                        }
                        R.id.monthButton -> {
                            Preview.setText(
                                MainActivity.Companion.ITEM_GENERATE.monthItem().getSummery()
                            )
                            value = "embedMonth"
                        }
                        R.id.timeButton -> {
                            Preview.setText(
                                MainActivity.Companion.ITEM_GENERATE.timeItem().getSummery()
                            )
                            value = "embedTime"
                        }
                    }
                }
            }
        radioGroup = findViewById(R.id.radioGroup)
        summitButton = findViewById(R.id.summit_button)
        spinner = findViewById(R.id.spinner)
        checkBox = findViewById(R.id.checkBox)
        radioGroup.setOnCheckedChangeListener(radioGroupButtonChangeListener)
        summitButton.setOnClickListener(mOnClickListener)
        spinner.setEnabled(false)
        val itemName: ArrayList<String?> = ArrayList()
        val entityItemInfo: List<EntityItemInfo?> =
            MainActivity.Companion.appDatabase!!.DatabaseDao().getItem()
        for (i in MainActivity.Companion.appDatabase!!.DatabaseDao().getItem().indices) {
            itemName.add(
                MainActivity.Companion.appDatabase!!.DatabaseDao().getItem().get(i).getSummery()
            )
        }
        checkBox.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View) {
                if (MainActivity.Companion.appDatabase!!.DatabaseDao().getItem().size != 0) {
                    if (checkBox.isChecked()) {
                        for (i in 0 until radioGroup.getChildCount()) {
                            radioGroup.getChildAt(i).setEnabled(false)
                        }
                        value = entityItemInfo.get(0).getId().toString()
                        Preview = findViewById(R.id.summery_preview)
                        Preview.setText(entityItemInfo.get(0).getSummery())
                        spinner.setSelection(0)
                        isFirstSelected = true
                        spinner.setEnabled(true)
                    } else {
                        for (i in 0 until radioGroup.getChildCount()) {
                            radioGroup.getChildAt(i).setEnabled(true)
                        }
                        isFirstSelected = false
                        spinner.setEnabled(false)
                    }
                } else {
                    checkBox.setChecked(false)
                    Toast.makeText(context, "저장된 항목이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        })
        val adapter: ArrayAdapter<String?> =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, itemName)
        spinner.setAdapter(adapter)
        spinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            public override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                Preview = findViewById(R.id.summery_preview)
                Preview.setText(entityItemInfo.get(position).getSummery())
                if (isFirstSelected) {
                    Preview.setText(entityItemInfo.get(position).getSummery())
                    value = entityItemInfo.get(position).getId().toString()
                    Log.d("value", value)
                }
            }

            public override fun onNothingSelected(parent: AdapterView<*>?) {}
        })

         */
    }
}