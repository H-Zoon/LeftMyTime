package com.devidea.timeleft.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.database.itemdata.ItemType

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!context.canPostReminderNotifications()) return

        ReminderScheduler.createChannel(context)

        val itemId = intent.getIntExtra(ReminderScheduler.EXTRA_ITEM_ID, 0)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val endValue = intent.getStringExtra(ReminderScheduler.EXTRA_END_VALUE).orEmpty()
        val type = intent.getStringExtra(ReminderScheduler.EXTRA_ITEM_TYPE)
            ?.let { runCatching { ItemType.valueOf(it) }.getOrNull() }
            ?: ItemType.Date
        val offset = intent.getIntExtra(ReminderScheduler.EXTRA_OFFSET_DAYS, 0)
        val notificationTitle = context.getString(R.string.notification_reminder_title, title)
        val notificationText = when (type) {
            ItemType.Time -> context.getString(R.string.notification_reminder_due_in_minutes, title, offset)
            ItemType.Date -> if (offset == 0) {
                context.getString(R.string.notification_reminder_due_today, title)
            } else {
                context.getString(R.string.notification_reminder_due_in_days, title, offset)
            }
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            itemId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = notificationBuilder(context)
            .setSmallIcon(R.drawable.ic_baseline_refresh_24)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(itemId, notification)

        if (type == ItemType.Time) {
            ReminderScheduler.scheduleNextTimeReminder(context, itemId, title, endValue, offset)
        }
    }

    @Suppress("DEPRECATION")
    private fun notificationBuilder(context: Context): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, ReminderScheduler.CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }
}
