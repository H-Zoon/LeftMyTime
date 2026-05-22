package com.devidea.timeleft.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ReminderScheduler.createChannel(context)

        val itemId = intent.getIntExtra(ReminderScheduler.EXTRA_ITEM_ID, 0)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val offsetDays = intent.getIntExtra(ReminderScheduler.EXTRA_OFFSET_DAYS, 0)
        val notificationTitle = context.getString(R.string.notification_reminder_title, title)
        val notificationText = if (offsetDays == 0) {
            context.getString(R.string.notification_reminder_due_today, title)
        } else {
            context.getString(R.string.notification_reminder_due_in_days, title, offsetDays)
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
    }

    @Suppress("DEPRECATION")
    private fun notificationBuilder(context: Context): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, ReminderScheduler.CHANNEL_ID)
        } else {
            Notification.Builder(context)
        }
}
