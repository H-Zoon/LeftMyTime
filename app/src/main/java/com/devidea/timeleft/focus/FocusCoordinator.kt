package com.devidea.timeleft.focus

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.room.withTransaction
import com.devidea.timeleft.R
import com.devidea.timeleft.database.AppDatabase
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.notification.ReminderScheduler
import com.devidea.timeleft.notification.canPostReminderNotifications
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

class AnotherFocusRunning : IllegalStateException()

@Singleton
class FocusCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val completionOutbox: FocusCompletionOutbox,
) {
    private val mutex = Mutex()

    suspend fun start(minutes: Int): Int = mutex.withLock {
        settleAndNotify()
        val now = System.currentTimeMillis()
        val id = database.withTransaction {
            val dao = database.itemDao()
            if (dao.getItems().any { it.focusState == FocusSession.RUNNING }) throw AnotherFocusRunning()
            dao.saveItem(FocusSession.create(context.getString(R.string.focus_default_title), minutes, now, ZoneId.systemDefault(), readFocusClock(context))).toInt()
        }
        scheduleNext()
        id
    }

    suspend fun change(id: Int, action: String) = mutex.withLock {
        settleAndNotify()
        database.withTransaction {
            val dao = database.itemDao()
            val now = System.currentTimeMillis()
            val current = dao.getSelectItem(id)
            require(current.isFocusSession)
            if (action == FocusSession.RUNNING && dao.getItems().any { it.id != id && it.focusState == FocusSession.RUNNING })
                throw AnotherFocusRunning()
            val updated = when (action) {
                FocusSession.PAUSED -> FocusSession.pause(current, now)
                FocusSession.RUNNING -> if (current.focusState == FocusSession.PAUSED) FocusSession.resume(current, now, readFocusClock(context)) else current
                FocusSession.ABORTED -> FocusSession.stop(current, now)
                else -> current
            }
            if (current != updated) dao.updateItem(updated)
        }
        scheduleNext()
    }

    suspend fun refresh() = mutex.withLock {
        settleAndNotify()
    }

    private suspend fun settleAndNotify() {
        completionOutbox.settle(System.currentTimeMillis(), readFocusClock(context))
        // Arm recovery before posting. Persisted notices are also recovered on app/boot refresh.
        runCatching { scheduleNext() }.onFailure {
            if (it is CancellationException) throw it
            Log.w("FocusCompletion", "Recovery alarm unavailable: ${it.javaClass.simpleName}")
        }
        try { deliverPendingCompletions() }
        finally { scheduleNext() }
    }

    private suspend fun deliverPendingCompletions() {
        val outbox = database.focusCompletionDao()
        for (notice in outbox.pending()) {
            val item = database.itemDao().getItems().firstOrNull { it.id == notice.itemId }
            if (item == null || !notice.matches(item) || item.reminderOffsetDays < 0 || !context.canPostReminderNotifications()) {
                // Respect a disabled reminder; never replay a backlog when permission returns.
                outbox.acknowledge(notice.itemId, notice.completedAt)
                continue
            }
            val now = System.currentTimeMillis()
            // A backward clock correction must not postpone a local retry indefinitely.
            if (notice.nextAttemptAt - now in 1..FocusCompletionNotice.MAX_RETRY_MILLIS) continue
            // Record the next attempt before crossing the non-transactional Android API boundary.
            outbox.update(notice.attempted(now))
            try {
                notifyFinished(item)
                outbox.acknowledge(notice.itemId, notice.completedAt)
            } catch (exception: CancellationException) { throw exception }
            catch (exception: Exception) {
                Log.w("FocusCompletion", "Completion delivery deferred: ${exception.javaClass.simpleName}")
            }
        }
    }

    private suspend fun scheduleNext() {
        val manager = context.getSystemService(AlarmManager::class.java)
        val alarm = PendingIntent.getBroadcast(context, 50_001, Intent(context, FocusReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.cancel(alarm)
        val now = System.currentTimeMillis()
        val clock = readFocusClock(context)
        val running = database.itemDao().getItems().filter { it.focusState == FocusSession.RUNNING }
            .map { FocusSession.remaining(FocusSession.withClock(it, now, clock), now) }.minOrNull()
        val retry = database.focusCompletionDao().pending().minOfOrNull {
            (it.nextAttemptAt - now).coerceIn(1_000L, FocusCompletionNotice.MAX_RETRY_MILLIS)
        }
        val remaining = listOfNotNull(running, retry).minOrNull() ?: return
        val end = clock.elapsedRealtime + remaining
        try {
            if (running == remaining && canUseExactAlarms(context)) manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, end, alarm)
            else manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, end, alarm)
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, end, alarm)
        }
    }

    private fun notifyFinished(item: ItemEntity) {
        if (!context.canPostReminderNotifications() || item.reminderOffsetDays < 0) return
        val click = PendingIntent.getActivity(context, item.id, FocusActivity.intent(context, item.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        ReminderScheduler.createChannel(context)
        context.getSystemService(NotificationManager::class.java).notify(item.id,
            Notification.Builder(context, ReminderScheduler.CHANNEL_ID).setSmallIcon(R.drawable.ic_baseline_refresh_24)
                .setContentTitle(context.getString(R.string.focus_completed))
                .setContentText(item.title).setContentIntent(click).setAutoCancel(true).setOnlyAlertOnce(true).build())
    }
}

fun canUseExactAlarms(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
    context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
