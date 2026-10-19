package com.devidea.timeleft.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import com.devidea.timeleft.focus.canUseExactAlarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Best-effort boundary and snapshot refresh; the OS may defer delivery. A second, exact alarm
 * serves only countdown widgets that must be replaced the moment their Chronometer hits zero.
 */
class WidgetBoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetBoundarySchedule.ACTION && intent.action != WidgetBoundarySchedule.ACTION_EXACT) return
        WidgetBoundarySchedule.invalidateAlarm()
        Log.i("WidgetRefresh", "Boundary refresh received: exact=${intent.action == WidgetBoundarySchedule.ACTION_EXACT}")
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppWidget.updateAllWidgetsAndAwait(appContext, AppWidgetManager.getInstance(appContext))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("WidgetBoundary", "Boundary refresh failed: ${error.javaClass.simpleName}")
            } finally {
                // Even failure before the first render must re-arm the consumed one-shot alarm.
                try { WidgetBoundarySchedule.rescheduleSafely(appContext) }
                finally { pending.finish() }
            }
        }
    }
}

internal object WidgetBoundarySchedule {
    const val ACTION = "com.devidea.timeleft.widget.REFRESH_BOUNDARY"
    const val ACTION_EXACT = "com.devidea.timeleft.widget.REFRESH_BOUNDARY_EXACT"
    private const val PREFS = "widget_boundaries"
    private const val EXACT_PREFS = "widget_boundaries_exact"
    private const val WINDOW_MILLIS = 10 * 60_000L
    private var scheduledAtMillis: Long? = null
    private var scheduledExactAtMillis: Long? = null

    /** Called after delivery and on wall-clock/boot changes; never infer delivery from the time. */
    @Synchronized
    fun invalidateAlarm() {
        scheduledAtMillis = null
        scheduledExactAtMillis = null
    }

    fun rescheduleSafely(context: Context): Boolean = try {
        reschedule(context)
        true
    } catch (error: Exception) {
        Log.e("WidgetBoundary", "Boundary scheduling failed: ${error.javaClass.simpleName}")
        false
    }

    /** [exactMillis] is the instant a countdown reaches zero; [boundaryMillis] is the best-effort refresh. */
    @Synchronized
    fun record(context: Context, id: Int, boundaryMillis: Long?, exactMillis: Long? = null) {
        listOf(PREFS to boundaryMillis, EXACT_PREFS to exactMillis).forEach { (name, millis) ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit {
                if (millis == null) remove(id.toString()) else putLong(id.toString(), millis)
            }
        }
        reschedule(context)
    }

    @Synchronized
    fun reschedule(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val now = System.currentTimeMillis()
        val alarm = context.getSystemService(AlarmManager::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val action = PendingIntent.getBroadcast(context, 0, Intent(context, WidgetBoundaryReceiver::class.java).setAction(ACTION), flags)
        val next = nextWidgetRefreshAlarmMillis(retainedBoundaries(context, manager, PREFS), now)
        if (next == null) {
            alarm.cancel(action)
            scheduledAtMillis = null
        } else if (!keepWidgetRefreshAlarm(scheduledAtMillis, next)) {
            // set() can be batched much later even with battery saver off. Bound the requested
            // window while awake; RTC still does not wake the device or bypass Doze.
            alarm.setWindow(AlarmManager.RTC, next, WINDOW_MILLIS, action)
            scheduledAtMillis = next
        }

        val exactAction = PendingIntent.getBroadcast(context, 1, Intent(context, WidgetBoundaryReceiver::class.java).setAction(ACTION_EXACT), flags)
        val nextExact = nextWidgetRefreshAlarmMillis(retainedBoundaries(context, manager, EXACT_PREFS), now)
        if (nextExact == null) {
            alarm.cancel(exactAction)
            scheduledExactAtMillis = null
        } else if (!keepWidgetRefreshAlarm(scheduledExactAtMillis, nextExact)) {
            setExactBoundary(context, alarm, nextExact, exactAction)
            scheduledExactAtMillis = nextExact
        }
    }

    /** Drops widgets that no longer exist, so a deleted countdown cannot keep waking the device. */
    private fun retainedBoundaries(context: Context, manager: AppWidgetManager, name: String): List<Long> {
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val retained = prefs.all.filter { (key, value) ->
            key.toIntOrNull()?.let { manager.getAppWidgetInfo(it) != null } == true && value is Long
        }
        prefs.edit { (prefs.all.keys - retained.keys).forEach(::remove) }
        return retained.values.filterIsInstance<Long>()
    }

    private fun setExactBoundary(context: Context, alarm: AlarmManager, at: Long, action: PendingIntent) {
        try {
            // Waking is deliberate: the countdown must not run past zero while the screen is off.
            if (canUseExactAlarms(context)) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, action)
            else alarm.setWindow(AlarmManager.RTC, at, WINDOW_MILLIS, action)
        } catch (_: SecurityException) {
            // Access was withdrawn between the check and the call; the widget is made static on its next render.
            alarm.setWindow(AlarmManager.RTC, at, WINDOW_MILLIS, action)
        }
    }
}
