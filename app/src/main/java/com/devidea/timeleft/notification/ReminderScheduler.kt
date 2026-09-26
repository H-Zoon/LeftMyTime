package com.devidea.timeleft.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.preferences.UserPreferences
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {
    const val CHANNEL_ID = "countdown_reminders"
    const val EXTRA_ITEM_ID = "com.devidea.timeleft.extra.REMINDER_ITEM_ID"
    internal const val EXTRA_TRIGGER = "reminder_trigger"
    internal const val EXTRA_SIGNATURE = "reminder_signature"
    private const val ACTION_REMINDER = "com.devidea.timeleft.action.REMINDER"
    private const val REQUEST_CODE_BASE = 20_000
    private const val LEDGER = "reminder_delivery"

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /** Repeated app resumes keep an already queued occurrence, including a late inexact alarm. */
    @Synchronized
    fun schedule(context: Context, item: ItemEntity, force: Boolean = false) {
        if (item.reminderOffsetDays < 0 || !context.canPostReminderNotifications()) {
            cancel(context, item.id)
            return
        }
        val ledger = context.getSharedPreferences(LEDGER, Context.MODE_PRIVATE)
        val signature = signature(context, item)
        val queued = ledger.getLong("trigger_${item.id}", 0)
        if (!force && queued > 0 && ledger.getString("signature_${item.id}", null) == signature &&
            existingIntent(context, item.id) != null) return
        val plan = ReminderPlanner.next(item, Instant.now(), ZoneId.systemDefault(), dateReminderTime(context))
        cancel(context, item.id)
        if (plan == null) return
        createChannel(context)
        val intent = baseIntent(context).putExtra(EXTRA_ITEM_ID, item.id)
            .putExtra(EXTRA_TRIGGER, plan.triggerMillis).putExtra(EXTRA_SIGNATURE, signature)
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE_BASE + item.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // This channel promises a reminder, not a precise alarm. Exact focus-end delivery is separate.
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerMillis, pending)
        ledger.edit().putLong("trigger_${item.id}", plan.triggerMillis)
            .putString("signature_${item.id}", signature).commit()
    }

    /** Reject stale edited/deleted/duplicate alarms before posting any notification. */
    @Synchronized
    fun claim(context: Context, item: ItemEntity, intent: Intent): Boolean {
        val trigger = intent.getLongExtra(EXTRA_TRIGGER, 0)
        val expected = intent.getStringExtra(EXTRA_SIGNATURE)
        val ledger = context.getSharedPreferences(LEDGER, Context.MODE_PRIVATE)
        if (trigger <= 0 || trigger > System.currentTimeMillis() || expected != signature(context, item) ||
            ledger.getLong("trigger_${item.id}", 0) != trigger ||
            ledger.getString("signature_${item.id}", null) != expected) return false
        // Persist the claim before delivery; duplicate broadcasts cannot notify twice.
        ledger.edit().remove("trigger_${item.id}").remove("signature_${item.id}").commit()
        return true
    }

    @Synchronized
    fun rescheduleAll(context: Context, items: List<ItemEntity>, force: Boolean = false) {
        val ledger = context.getSharedPreferences(LEDGER, Context.MODE_PRIVATE)
        val ids = items.mapTo(mutableSetOf()) { it.id }
        ledger.all.keys.filter { it.startsWith("trigger_") }.mapNotNull { it.removePrefix("trigger_").toIntOrNull() }
            .filterNot { it in ids }.forEach { cancel(context, it) }
        items.forEach { schedule(context, it, force) }
    }

    @Synchronized
    fun cancel(context: Context, itemId: Int) {
        existingIntent(context, itemId)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
        context.getSharedPreferences(LEDGER, Context.MODE_PRIVATE).edit()
            .remove("trigger_$itemId").remove("signature_$itemId").commit()
    }

    private fun existingIntent(context: Context, id: Int): PendingIntent? = PendingIntent.getBroadcast(
        context, REQUEST_CODE_BASE + id, baseIntent(context), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
    private fun baseIntent(context: Context) = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMINDER)

    private fun dateReminderTime(context: Context): LocalTime = runCatching {
        LocalTime.parse(PreferenceManager.getDefaultSharedPreferences(context).getString(
            UserPreferences.KEY_DATE_REMINDER_TIME, UserPreferences.DEFAULT_DATE_REMINDER_TIME))
    }.getOrDefault(LocalTime.of(9, 0))

    private fun signature(context: Context, item: ItemEntity): String {
        val value = listOf(item.id, item.type, item.title, item.startValue, item.endValue,
            item.updateFlag, item.updateRate, item.occurrenceStartMillis, item.occurrenceEndMillis, item.reminderOffsetDays, item.weekdays, item.endNextDay, dateReminderTime(context), ZoneId.systemDefault())
            .joinToString("\u0000")
        return MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
