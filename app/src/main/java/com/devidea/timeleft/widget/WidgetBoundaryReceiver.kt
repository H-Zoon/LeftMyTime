package com.devidea.timeleft.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Non-waking, best-effort boundary and snapshot refresh; the OS may defer delivery. */
class WidgetBoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetBoundarySchedule.ACTION) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppWidget.updateAllWidgetsAndAwait(appContext, AppWidgetManager.getInstance(appContext))
            } catch (error: Exception) {
                Log.e("WidgetBoundary", "Boundary refresh failed: ${error.javaClass.simpleName}")
            } finally { pending.finish() }
        }
    }
}

internal object WidgetBoundarySchedule {
    const val ACTION = "com.devidea.timeleft.widget.REFRESH_BOUNDARY"
    private const val PREFS = "widget_boundaries"

    @Synchronized
    fun record(context: Context, id: Int, boundaryMillis: Long?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            if (boundaryMillis == null) remove(id.toString()) else putLong(id.toString(), boundaryMillis)
        }
        reschedule(context)
    }

    @Synchronized
    fun reschedule(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val retained = prefs.all.filter { (key, value) ->
            key.toIntOrNull()?.let { manager.getAppWidgetInfo(it) != null } == true && value is Long
        }
        prefs.edit { (prefs.all.keys - retained.keys).forEach(::remove) }
        val alarm = context.getSystemService(AlarmManager::class.java)
        val action = PendingIntent.getBroadcast(context, 0, Intent(context, WidgetBoundaryReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = nextWidgetRefreshAlarmMillis(retained.values.filterIsInstance<Long>(), now)
        if (next == null) alarm.cancel(action) else alarm.set(AlarmManager.RTC, next, action)
    }
}
