package com.devidea.timeleft.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.widget.WidgetDetailsActivity
import com.devidea.timeleft.widget.WidgetSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: TimeLeftRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val id = intent.getIntExtra(ReminderScheduler.EXTRA_ITEM_ID, 0)
                val item = repository.allItems().firstOrNull { it.id == id }
                if (item == null) {
                    ReminderScheduler.cancel(app, id)
                    return@launch
                }
                val claimed = ReminderScheduler.claim(app, item, intent)
                try {
                    if (claimed && app.canPostReminderNotifications()) {
                        ReminderScheduler.createChannel(app)
                        val text = when {
                            item.type == ItemType.Time -> app.getString(R.string.notification_reminder_due_in_minutes, item.title, item.reminderOffsetDays)
                            item.reminderOffsetDays == 0 -> app.getString(R.string.notification_reminder_due_today, item.title)
                            else -> app.getString(R.string.notification_reminder_due_in_days, item.title, item.reminderOffsetDays)
                        }
                        val target = WidgetDetailsActivity.createIntent(app, AppWidgetManager.INVALID_APPWIDGET_ID, WidgetSource.Custom, id)
                        val click = PendingIntent.getActivity(app, id, target, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        val notification = Notification.Builder(app, ReminderScheduler.CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_baseline_refresh_24)
                            .setContentTitle(app.getString(R.string.notification_reminder_title, item.title))
                            .setContentText(text).setContentIntent(click).setAutoCancel(true).build()
                        app.getSystemService(NotificationManager::class.java).notify(id, notification)
                    }
                } finally {
                    // Both date and time recurrence continue even when notification posting fails.
                    ReminderScheduler.schedule(app, item)
                }
            } catch (exception: Exception) {
                // Do not put titles or exact schedule values in diagnostics.
                Log.e("ReminderReceiver", "Reminder processing failed: ${exception.javaClass.simpleName}")
            } finally {
                pending.finish()
            }
        }
    }
}
