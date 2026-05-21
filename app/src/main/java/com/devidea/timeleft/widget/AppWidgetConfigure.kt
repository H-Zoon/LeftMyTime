package com.devidea.timeleft.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RemoteViews
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.databinding.AppwidgetConfigureBinding
import com.devidea.timeleft.repository.TimeLeftRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AppWidgetConfigure : AppCompatActivity() {

    @Inject lateinit var repository: TimeLeftRepository
    @Inject lateinit var prefs: SharedPreferences

    private lateinit var binding: AppwidgetConfigureBinding
    private lateinit var value: String
    private lateinit var itemList: List<ItemEntity>
    private lateinit var items: ArrayList<String>
    private lateinit var ids: ArrayList<Int>
    private lateinit var id: String

    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val context: Context = this@AppWidgetConfigure

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        binding = AppwidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.spinner.isEnabled = false

        lifecycleScope.launch {
            itemList = withContext(Dispatchers.IO) { repository.allItems() }
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

        binding.save.setOnClickListener {
            val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(
                context.packageName,
                R.layout.app_widget
            )

            val intentR = Intent(context, AppWidget::class.java)
            intentR.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val updateIntent: PendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intentR,
                PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.refresh, updateIntent)

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
                    "custom" -> customWidgetInit(appWidgetManager)
                }

                if (value != "custom") {
                    prefs.edit()
                        .putString(widgetId.toString(), value)
                        .putBoolean("${widgetId}option", binding.option.isChecked)
                        .apply()

                    val resultValue = Intent()
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    setResult(RESULT_OK, resultValue)
                    AppWidget().updateAppWidget(this, appWidgetManager, widgetId)
                    finish()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    getString(R.string.widget_configure_select_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
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

    private fun customWidgetInit(appWidgetManager: AppWidgetManager) {
        lifecycleScope.launch {
            val exists = withContext(Dispatchers.IO) {
                runCatching { repository.getItem(id.toInt()) }.isSuccess
            }

            if (!exists) {
                finish()
                return@launch
            }

            prefs.edit()
                .putString(widgetId.toString(), id)
                .putBoolean("${widgetId}option", binding.option.isChecked)
                .apply()

            val resultValue = Intent()
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, resultValue)
            AppWidget().updateAppWidget(this@AppWidgetConfigure, appWidgetManager, widgetId)
            finish()
        }
    }
}