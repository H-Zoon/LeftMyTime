package com.devidea.timeleft.design

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.content.Intent
import android.app.PendingIntent
import android.content.res.Configuration
import android.util.Log
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import com.devidea.timeleft.R
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.widget.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Debug-only host experiment: never updates a production widget or schedule. */
class WidgetSecondsProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("clicked")) {
            val message = "Probe action: ${intent.getStringExtra("clicked")}"
            Log.i(PROBE_TAG, message)
            setContentView(TextView(this).apply { text = message; textSize = 24f })
            return
        }
        val mode = intent.getStringExtra("mode") ?: "remote"
        val duration = intent.getIntExtra("seconds", 60).coerceIn(1, 3600)
        val start = System.currentTimeMillis() + intent.getIntExtra("start_delay", 0) * 1000L
        getSharedPreferences(PROBE_PREFS, MODE_PRIVATE).edit {
            putString("mode", mode)
            putLong("start_wall", start)
            putLong("end_wall", start + duration * 1000L)
            putLong("end_elapsed", SystemClock.elapsedRealtime() + duration * 1000L)
            putBoolean("dark", intent.getBooleanExtra("dark", false))
            putBoolean("board", intent.getBooleanExtra("board", false))
            putBoolean("long_title", intent.getBooleanExtra("long_title", false))
            putFloat("font_scale", intent.getFloatExtra("font_scale", 1f))
            putString("locale", intent.getStringExtra("locale") ?: java.util.Locale.getDefault().toLanguageTag())
        }
        val version = if (Build.VERSION.SDK_INT >= 35) RemoteViews.DrawInstructions.getSupportedVersion() else 0
        val message = getString(R.string.widget_seconds_probe_version, version, mode)
        Log.i(PROBE_TAG, message)
        setContentView(TextView(this).apply { text = message; textSize = 24f })
        val manager = AppWidgetManager.getInstance(this)
        val component = ComponentName(this, WidgetSecondsProbeProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        WidgetSecondsProbeProvider().onUpdate(this, manager, ids)
        if (intent.getBooleanExtra("pin", false)) manager.requestPinAppWidget(component, null, null)
    }
}

class WidgetSecondsProbeProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences(PROBE_PREFS, Context.MODE_PRIVATE)
        val mode = prefs.getString("mode", "remote")
        ids.forEach { id ->
            val view = try {
                if (mode == "production" && Build.VERSION.SDK_INT >= 35 && WidgetSecondsSupport.available()) {
                    val configured = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                        fontScale = prefs.getFloat("font_scale", 1f)
                        setLocale(java.util.Locale.forLanguageTag(prefs.getString("locale", "en") ?: "en"))
                    })
                    val start = prefs.getLong("start_wall", System.currentTimeMillis())
                    val end = prefs.getLong("end_wall", start + 60_000)
                    val format = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
                    val item = AdapterItem(id = 1, title = if (prefs.getBoolean("long_title", false))
                        "긴 제목과 큰 글자 / A long schedule title" else "Seconds QA", type = ItemType.Time,
                        startLabel = format.format(Instant.ofEpochMilli(start)), endLabel = format.format(Instant.ofEpochMilli(end)),
                        startsAtMillis = start, endsAtMillis = end)
                    val plan = WidgetSecondsPlan.create(WidgetConfiguration(WidgetSource.Custom, 1), item, start)!!
                    val palette = WidgetPalette.create("clay", prefs.getBoolean("dark", false),
                        if (prefs.getBoolean("board", false)) "time-board" else "time-focus")
                    widgetViewsForSizes(manager.getAppWidgetOptions(id), WidgetSource.Custom) { size ->
                        WidgetSecondsRenderer.create(configured, size, item, plan, palette, true).apply {
                            listOf(R.id.widgetRoot to "details", R.id.refresh to "refresh", R.id.widgetSecondsConfigure to "resize").forEach { (viewId, action) ->
                                val click = Intent(context, WidgetSecondsProbeActivity::class.java).putExtra("clicked", action)
                                    .setData(android.net.Uri.parse("timeleft://seconds-probe/$id/$action"))
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                setOnClickPendingIntent(viewId, PendingIntent.getActivity(context, viewId, click,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                            }
                        }
                    }
                } else if (mode?.startsWith("remote") == true && Build.VERSION.SDK_INT >= 35 &&
                    RemoteViews.DrawInstructions.getSupportedVersion() >= 6
                ) {
                    remoteProbe(context, prefs.getLong("end_wall", System.currentTimeMillis()), mode == "remote_epoch")
                } else {
                    RemoteViews(context.packageName, R.layout.widget_seconds_probe).apply {
                        setTextViewText(R.id.probe_status, context.getString(
                            if (mode == "chrono") R.string.widget_seconds_probe_remaining
                            else R.string.widget_seconds_probe_unsupported
                        ))
                        setChronometerCountDown(R.id.probe_clock, true)
                        setChronometer(R.id.probe_clock, prefs.getLong("end_elapsed", SystemClock.elapsedRealtime()), null, mode == "chrono")
                    }
                }
            } catch (error: Exception) {
                Log.e(PROBE_TAG, "Document creation failed", error)
                RemoteViews(context.packageName, R.layout.widget_seconds_probe).apply {
                    setTextViewText(R.id.probe_status, error.javaClass.simpleName)
                }
            }
            manager.updateAppWidget(id, view)
            Log.i(PROBE_TAG, "Published probe id=$id mode=$mode")
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        onUpdate(context, manager, intArrayOf(id))
    }
}

// These restricted authoring calls are isolated to the capability experiment, not a release API.
@SuppressLint("RestrictedApi")
@RequiresApi(35)
private fun remoteProbe(context: Context, endMillis: Long, integerClock: Boolean): RemoteViews {
    val version = RemoteViews.DrawInstructions.getSupportedVersion()
    val profile = if (version >= 7) RcPlatformProfiles.WIDGETS_V7 else RcPlatformProfiles.WIDGETS_V6
    val writer = RemoteComposeWriterAndroid(CreationDisplayInfo(320, 160, 160), context.getString(R.string.widget_seconds_probe_name), profile)
    writer.rcPaint.setColor(0xFFF7F4EF.toInt()).commit()
    writer.drawRect(0f, 0f, 320f, 160f)
    val raw = if (integerClock) {
        val delta = writer.integerExpression(endMillis / 1000, Rc.Time.INT_EPOCH_SECOND, Rc.IntegerExpression.L_SUB)
        // Epoch subtraction happens as integers before converting the short remaining interval.
        // The quantized clock dependency asks the host for a second tick, not animation frames.
        writer.floatExpression(writer.asFloatId(delta), Rc.Time.TIME_IN_SEC, 0f, Rc.FloatExpression.MUL, Rc.FloatExpression.ADD)
    } else writer.timeAttribute(writer.addTimeLong(endMillis), Rc.TimeAttributes.TIME_FROM_NOW_SEC)
    val remaining = writer.floatExpression(raw, 0f, Rc.FloatExpression.MAX, Rc.FloatExpression.CEIL)
    val number = writer.createTextFromFloat(remaining, 4, 0, 0)
    writer.rcPaint.setColor(0xFF211E1B.toInt()).setTextSize(16f).commit()
    writer.drawTextAnchored(context.getString(R.string.widget_seconds_probe_name), 16f, 28f, -1f, 0f, 0)
    writer.rcPaint.setTextSize(30f).commit()
    writer.drawTextAnchored(number, 16f, 80f, -1f, 0f, 0)
    writer.rcPaint.setTextSize(14f).commit()
    writer.conditionalOperations(Rc.Condition.GT, raw, 0f) {
        writer.drawTextAnchored(context.getString(R.string.widget_seconds_probe_remaining), 16f, 120f, -1f, 0f, 0)
    }
    writer.conditionalOperations(Rc.Condition.LTE, raw, 0f) {
        writer.drawTextAnchored(context.getString(R.string.widget_seconds_probe_finished), 16f, 120f, -1f, 0f, 0)
    }
    val bytes = writer.buffer().copyOf(writer.bufferSize())
    Log.i(PROBE_TAG, "Document version=$version bytes=${bytes.size}")
    return RemoteViews(RemoteViews.DrawInstructions.Builder(listOf(bytes)).build())
}

private const val PROBE_PREFS = "widget_seconds_probe"
private const val PROBE_TAG = "WidgetSecondsProbe"
