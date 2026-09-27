package com.devidea.timeleft.notification

import android.app.AlarmManager
import com.devidea.timeleft.focus.FocusCoordinator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var focus: FocusCoordinator
    @Inject lateinit var reminders: ReminderCoordinator

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                focus.refresh()
                reminders.refresh(force = true)
                com.devidea.timeleft.widget.AppWidget.updateAllWidgetsAndAwait(context,
                    android.appwidget.AppWidgetManager.getInstance(context))
            } catch (exception: Exception) {
                Log.e("ReminderRefresh", "System refresh failed: ${exception.javaClass.simpleName}")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED, AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    }
}
