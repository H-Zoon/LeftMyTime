package com.devidea.timeleft.widget

import android.content.Intent
import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.*
import com.devidea.timeleft.App
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.R
import com.devidea.timeleft.databinding.AppwidgetConfigureBinding
import com.devidea.timeleft.datadase.AppDatabase
import com.devidea.timeleft.datadase.itemdata.ItemEntity
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
        binding.spinner.isEnabled = false

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
            } else {
                binding.userItemButton.isEnabled = false
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

            try {
                when (value) {
                    "embedYear" -> appWidgetManager.updateAppWidget(widgetId, views)

                    "embedMonth" -> appWidgetManager.updateAppWidget(widgetId, views)

                    "embedTime" -> appWidgetManager.updateAppWidget(widgetId, views)

                    "custom" -> customWidgetInit(views, appWidgetManager)
                }

                if (value != "custom") {
                    with(MainActivity.prefs.edit()) {
                        putString(widgetId.toString(), value)
                        putBoolean(widgetId.toString() + "option", binding.option.isChecked)
                    }.apply()

                    val resultValue = Intent()
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    setResult(RESULT_OK, resultValue)
                    AppWidget().updateAppWidget(App.context(), appWidgetManager, widgetId)
                    finish()
                }

            } catch (e: Exception) {
                Toast.makeText(this, "하나를 정해주세요", Toast.LENGTH_SHORT).show()
            }

        }

        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.yearButton -> {
                    value = "embedYear"
                    binding.spinner.isEnabled = false
                }
                R.id.monthButton -> {
                    value = "embedMonth"
                    binding.spinner.isEnabled = false
                }
                R.id.timeButton -> {
                    value = "embedTime"
                    binding.spinner.isEnabled = false
                }
                R.id.userItemButton -> {
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
            val itemList =
                AppDatabase.getDatabase(App.context()).itemDao().getSelectItem(id.toInt())

            if ((itemList.type == "Time")) {
                MainActivity.ITEM_GENERATE.customTimeItem(itemList)

            } else {
                MainActivity.ITEM_GENERATE.customMonthItem(itemList)
            }

            appWidgetManager.updateAppWidget(widgetId, views)

            with(MainActivity.prefs.edit()) {
                putString(widgetId.toString(), id)
                putBoolean(widgetId.toString() + "option", binding.option.isChecked)
            }.apply()

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, resultValue)

            AppWidget().updateAppWidget(App.context(), appWidgetManager, widgetId)
            finish()
        }
    }
}