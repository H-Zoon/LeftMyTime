package com.devidea.timeleft.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.devidea.timeleft.ItemVisuals
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderScheduler {
    const val CHANNEL_ID = "countdown_reminders"
    const val EXTRA_ITEM_ID = "com.devidea.timeleft.extra.REMINDER_ITEM_ID"
    const val EXTRA_TITLE = "com.devidea.timeleft.extra.REMINDER_TITLE"
    const val EXTRA_END_VALUE = "com.devidea.timeleft.extra.REMINDER_END_VALUE"
    const val EXTRA_OFFSET_DAYS = "com.devidea.timeleft.extra.REMINDER_OFFSET_DAYS"
    const val EXTRA_ITEM_TYPE = "com.devidea.timeleft.extra.REMINDER_ITEM_TYPE"

    private const val ACTION_REMINDER = "com.devidea.timeleft.action.REMINDER"
    private const val REQUEST_CODE_BASE = 20_000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    fun schedule(context: Context, item: ItemEntity) {
        cancel(context, item.id)
        if (item.reminderOffsetDays == ItemVisuals.REMINDER_DISABLED) return
        if (!context.canPostReminderNotifications()) return

        val triggerMillis = reminderTimeMillis(item) ?: return
        if (triggerMillis <= System.currentTimeMillis()) return

        createChannel(context)
        setReminderAlarm(context, item.id, reminderIntent(context, item), triggerMillis)
    }

    fun scheduleNextTimeReminder(
        context: Context,
        itemId: Int,
        title: String,
        endValue: String,
        offsetMinutes: Int,
    ) {
        if (offsetMinutes == ItemVisuals.REMINDER_DISABLED) return
        if (!context.canPostReminderNotifications()) return

        val triggerMillis = timeReminderTimeMillis(endValue, offsetMinutes) ?: return
        setReminderAlarm(
            context,
            itemId,
            reminderIntent(context, itemId, title, ItemType.Time, endValue, offsetMinutes),
            triggerMillis
        )
    }

    private fun setReminderAlarm(
        context: Context,
        itemId: Int,
        intent: Intent,
        triggerMillis: Long,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(itemId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun rescheduleAll(context: Context, items: List<ItemEntity>) {
        createChannel(context)
        items.forEach { schedule(context, it) }
    }

    fun cancel(context: Context, itemId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(itemId),
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMINDER),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun reminderTimeMillis(item: ItemEntity): Long? = when (item.type) {
        ItemType.Date -> dateReminderTimeMillis(item)
        ItemType.Time -> timeReminderTimeMillis(item.endValue, item.reminderOffsetDays)
    }

    private fun dateReminderTimeMillis(item: ItemEntity): Long? {
        val endDate = runCatching {
            LocalDate.parse(item.endValue, STORAGE_DATE_FORMATTER)
        }.getOrNull() ?: return null
        val triggerDate = endDate.minusDays(item.reminderOffsetDays.toLong())
        return LocalDateTime.of(triggerDate, LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun timeReminderTimeMillis(endValue: String, offsetMinutes: Int): Long? {
        val endTime = runCatching {
            LocalTime.parse(endValue, STORAGE_TIME_FORMATTER)
        }.getOrNull() ?: return null
        val now = LocalDateTime.now()
        var triggerDateTime = LocalDateTime.of(now.toLocalDate(), endTime)
            .minusMinutes(offsetMinutes.toLong())
        if (!triggerDateTime.isAfter(now)) {
            triggerDateTime = triggerDateTime.plusDays(1)
        }
        return triggerDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private val STORAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d")
    private val STORAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:m")

    private fun reminderIntent(context: Context, item: ItemEntity): Intent =
        reminderIntent(
            context,
            item.id,
            item.title,
            item.type,
            item.endValue,
            item.reminderOffsetDays
        )

    private fun reminderIntent(
        context: Context,
        itemId: Int,
        title: String,
        type: ItemType,
        endValue: String,
        offset: Int,
    ): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_ITEM_TYPE, type.name)
            putExtra(EXTRA_END_VALUE, endValue)
            putExtra(EXTRA_OFFSET_DAYS, offset)
        }

    private fun requestCode(itemId: Int): Int = REQUEST_CODE_BASE + itemId
}
