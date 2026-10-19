package com.devidea.timeleft.messaging

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.devidea.timeleft.R
import com.devidea.timeleft.activity.MainActivity
import com.devidea.timeleft.notification.isNotificationRuntimePermissionDenied
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger

internal enum class PushNotificationResult { Posted, Blocked, RewardOptedOut }

/** Remote messages use a channel and notification tag independent of local schedule reminders. */
object PushNotifications {
    const val CHANNEL_ID = "remote_messages"
    const val REWARD_CHANNEL_ID = "reward_notifications"
    private const val NOTIFICATION_TAG = "remote_messages"
    private const val OPEN_ACTION = "com.devidea.timeleft.action.OPEN_PUSH"
    private val nextNotificationId = AtomicInteger((System.currentTimeMillis() % Int.MAX_VALUE).toInt())

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_push),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        createRewardChannel(context)
    }

    internal fun createRewardChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                REWARD_CHANNEL_ID,
                context.getString(R.string.notification_channel_rewards),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notification_channel_rewards_description) },
        )
    }

    fun canPostNotifications(context: Context): Boolean {
        return canPostToChannel(context, CHANNEL_ID)
    }

    internal fun canPostRewardNotifications(context: Context): Boolean =
        canPostToChannel(context, REWARD_CHANNEL_ID)

    private fun canPostToChannel(context: Context, channelId: String): Boolean {
        if (context.isNotificationRuntimePermissionDenied()) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Includes SDK-posted background rewards; unrelated schedule/focus/general channels are kept. */
    internal fun cancelRewardNotifications(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        for (posted in manager.activeNotifications) {
            if (posted.notification.channelId == REWARD_CHANNEL_ID) {
                manager.cancel(posted.tag, posted.id)
            }
        }
    }

    /** Called only for messages delivered to the service; the SDK displays background notifications. */
    internal fun show(context: Context, message: RemoteMessage): PushNotificationResult {
        createChannel(context)
        val reward = isRewardMessage(message.data["notification_kind"], message.from, message.notification?.channelId)
        val channelId = if (reward) REWARD_CHANNEL_ID else CHANNEL_ID
        if (reward) {
            return RewardMessaging.postWithConsent {
                if (!canPostRewardNotifications(context)) PushNotificationResult.Blocked
                else post(context, message, channelId, REWARD_CHANNEL_ID)
            }
        }
        if (!canPostNotifications(context)) return PushNotificationResult.Blocked
        return post(context, message, channelId, NOTIFICATION_TAG)
    }

    private fun post(
        context: Context,
        message: RemoteMessage,
        channelId: String,
        tag: String,
    ): PushNotificationResult {
        val title = message.notification?.title?.takeIf { it.isNotBlank() }
            ?: message.data["title"]?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.push_notification_default_title)
        val body = message.notification?.body?.takeIf { it.isNotBlank() }
            ?: message.data["body"]?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.push_notification_default_body)
        val notificationId = message.messageId?.hashCode() ?: nextNotificationId.incrementAndGet()
        val target = Intent(context, MainActivity::class.java)
            .setAction(OPEN_ACTION)
            .setData(Uri.Builder().scheme("timeleft").authority("push")
                .appendPath(tag).appendPath(notificationId.toString()).build())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (PointHomePushOpen.isRequested(message.data[PointHomePushOpen.KIND_KEY], message.data[PointHomePushOpen.TARGET_KEY])) {
            target.putExtra(PointHomePushOpen.KIND_KEY, PointHomePushOpen.REWARD_KIND)
                .putExtra(PointHomePushOpen.TARGET_KEY, PointHomePushOpen.POINT_HOME_TARGET)
        }
        val click = PendingIntent.getActivity(
            context,
            notificationId,
            target,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_baseline_refresh_24)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(click)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(tag, notificationId, notification)
        return PushNotificationResult.Posted
    }
}
