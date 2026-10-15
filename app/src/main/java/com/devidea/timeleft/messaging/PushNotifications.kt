package com.devidea.timeleft.messaging

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.notification.isNotificationRuntimePermissionDenied
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger

/** Remote messages use a channel and notification tag independent of local schedule reminders. */
object PushNotifications {
    const val CHANNEL_ID = "remote_messages"
    private const val NOTIFICATION_TAG = "remote_messages"
    private const val OPEN_ACTION = "com.devidea.timeleft.action.OPEN_PUSH"
    private const val OPEN_REQUEST_CODE = 70_001
    private val nextNotificationId = AtomicInteger((System.currentTimeMillis() % Int.MAX_VALUE).toInt())

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_push),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun canPostNotifications(context: Context): Boolean {
        if (context.isNotificationRuntimePermissionDenied()) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Called only for messages delivered to the service; the SDK displays background notifications. */
    internal fun show(context: Context, message: RemoteMessage): Boolean {
        createChannel(context)
        if (!canPostNotifications(context)) return false

        val title = message.notification?.title?.takeIf { it.isNotBlank() }
            ?: message.data["title"]?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.push_notification_default_title)
        val body = message.notification?.body?.takeIf { it.isNotBlank() }
            ?: message.data["body"]?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.push_notification_default_body)
        val target = Intent(context, MainActivity::class.java)
            .setAction(OPEN_ACTION)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val click = PendingIntent.getActivity(
            context,
            OPEN_REQUEST_CODE,
            target,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_refresh_24)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(click)
            .setAutoCancel(true)
            .build()
        val notificationId = message.messageId?.hashCode() ?: nextNotificationId.incrementAndGet()
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_TAG, notificationId, notification)
        return true
    }
}
