package com.devidea.timeleft.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A persisted recovery path; 15 minutes is a requested interval, not a delivery deadline. */
class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i("WidgetRefresh", "Periodic refresh started")
        try {
            val refreshed = AppWidget.updateAllWidgetsAndAwait(applicationContext, AppWidgetManager.getInstance(applicationContext))
            Log.i("WidgetRefresh", "Periodic refresh finished: retry=${!refreshed}")
            if (refreshed) Result.success() else Result.retry()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e("WidgetRefresh", "Periodic refresh failed: ${error.javaClass.simpleName}")
            Result.retry()
        }
    }
}

internal object WidgetRefreshSchedule {
    const val WORK_NAME = "widget_periodic_refresh"
    private val mutex = Mutex()

    /** Persist before rendering so even a failed first render leaves a recovery request. */
    suspend fun reconcile(context: Context) = mutex.withLock {
        val manager = AppWidgetManager.getInstance(context)
        val work = WorkManager.getInstance(context)
        if (AppWidget.installedWidgetIds(context, manager).isEmpty()) {
            work.cancelUniqueWork(WORK_NAME).await()
        } else {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            // Repeated renders/resumes must not move the next periodic run into the future.
            work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request).await()
        }
    }

    /** Scheduling trouble must not suppress a manual or provider-triggered render. */
    suspend fun reconcileSafely(context: Context) {
        try { reconcile(context) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            Log.e("WidgetRefresh", "Periodic scheduling failed: ${error.javaClass.simpleName}")
        }
    }
}
