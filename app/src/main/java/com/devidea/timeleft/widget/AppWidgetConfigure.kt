package com.devidea.timeleft.widget

import android.content.Intent
import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.os.Bundle
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.provider.Settings.Global.putInt
import android.util.Log
import android.view.View
import android.widget.*
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.App
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.R
import com.devidea.timeleft.databinding.ActivitySetRepeatBinding
import com.devidea.timeleft.databinding.AppwidgetConfigureBinding
import com.devidea.timeleft.datadase.AppDatabase
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import com.devidea.timeleft.datadase.itemdata.WidgetEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AppWidgetConfigure : Activity() {
    private lateinit var binding: AppwidgetConfigureBinding
    private lateinit var value: String
    private lateinit var itemList: List<ItemEntity>
    private lateinit var items: ArrayList<String>
    private lateinit var ids: ArrayList<Int>
    private lateinit var id: String

    var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    var context: Context = this@AppWidgetConfigure

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setResult(RESULT_CANCELED)
        setContentView(R.layout.appwidget_configure)

        binding = AppwidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CoroutineScope(Dispatchers.IO).launch {
            itemList = AppDatabase.getDatabase(App.context()).itemDao().item
            if (itemList.isNotEmpty()) {
                items = ArrayList()
                ids = ArrayList()
                for (i in itemList.indices) {
                    items.add(itemList[i].title)
                    ids.add(itemList[i].id)
                }
                adapterInit()
            }
        }

        val intent: Intent = intent
        val extras: Bundle? = intent.extras
        if (extras != null) {
            widgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }

        binding.save.setOnClickListener { v: View ->
            val context: Context = this
            val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(
                context.packageName,
                R.layout.app_widget
            )

            //위젯에 새로고침 버튼 추가
            val intentR = Intent(context, AppWidget::class.java)
            intentR.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val updateIntent: PendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intentR,
                PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.refrash, updateIntent)

            val appIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            views.setOnClickPendingIntent(R.id.percent, appIntent)


            when (value) {
                "embedYear" -> {
                    val item = MainActivity.ITEM_GENERATE.yearItem()
                    views.setTextViewText(R.id.summery, item.title)
                    views.setTextViewText(R.id.percent, item.percent.toString() + "%")
                    views.setProgressBar(
                        R.id.progress,
                        100,
                        item.percent.toInt(),
                        false
                    )
                    appWidgetManager.updateAppWidget(widgetId, views)

                }
                "embedMonth" -> {
                    val item = MainActivity.ITEM_GENERATE.monthItem()
                    views.setTextViewText(R.id.summery, item.title)
                    views.setTextViewText(R.id.percent, item.percent.toString() + "%")
                    views.setProgressBar(
                        R.id.progress,
                        100,
                        item.percent.toInt(),
                        false
                    )

                    appWidgetManager.updateAppWidget(widgetId, views)

                }
                "embedTime" -> {
                    val item = MainActivity.ITEM_GENERATE.timeItem()
                    views.setTextViewText(R.id.summery, item.title)
                    views.setTextViewText(R.id.percent, item.percent.toString() + "%")
                    views.setProgressBar(
                        R.id.progress,
                        100,
                        item.percent.toInt(),
                        false
                    )
                    appWidgetManager.updateAppWidget(widgetId, views)

                }

                "custom" -> {
                    customWidgetInit(views, appWidgetManager)

                }

                else -> {
                    setResult(RESULT_CANCELED)
                    Toast.makeText(context, "취소되었습니다", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            with(MainActivity.prefs.edit()) {
                putString(widgetId.toString(), value)
                apply()
            }

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }

        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val preView: TextView = findViewById(R.id.summery_preview)
            when (checkedId) {
                R.id.yearButton -> {
                    preView.text = MainActivity.ITEM_GENERATE.yearItem().title
                    value = "embedYear"
                    binding.spinner.isEnabled = false
                }
                R.id.monthButton -> {
                    preView.text = MainActivity.ITEM_GENERATE.monthItem().title
                    value = "embedMonth"
                    binding.spinner.isEnabled = false
                }
                R.id.timeButton -> {
                    preView.text = MainActivity.ITEM_GENERATE.timeItem().title
                    value = "embedTime"
                    binding.spinner.isEnabled = false
                }
                R.id.userItemButton -> {
                    preView.text = "제목이 표시됩니다"
                    value = "custom"
                    binding.spinner.isEnabled = true
                }
            }
        }

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                id = ids[position].toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

    }


    private fun adapterInit() {
        val adapter: ArrayAdapter<String?> =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, items as List<String?>)
        binding.spinner.adapter = adapter
    }

    private fun customWidgetInit(views: RemoteViews, appWidgetManager: AppWidgetManager) {
        CoroutineScope(Dispatchers.IO).launch {
            var item = AdapterItem()
            val itemList =
                AppDatabase.getDatabase(App.context()).itemDao().getSelectItem(id.toInt())

            if ((itemList.type == "Time")) {
                item = MainActivity.ITEM_GENERATE.customTimeItem(itemList)

            } else {
                item =
                    MainActivity.ITEM_GENERATE.customMonthItem(itemList)

            }
            views.setTextViewText(R.id.summery, item.title)
            views.setTextViewText(R.id.percent, item.percent.toString() + "%")
            views.setProgressBar(
                R.id.progress,
                100,
                item.percent.toInt(),
                false
            )

            appWidgetManager.updateAppWidget(widgetId, views)

            with(MainActivity.prefs.edit()) {
                putString(widgetId.toString(), id)
                apply()
            }

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }


}