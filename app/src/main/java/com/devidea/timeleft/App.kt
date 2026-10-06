package com.devidea.timeleft

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.devidea.timeleft.focus.FocusCoordinator
import com.devidea.timeleft.notification.ReminderCoordinator
import com.devidea.timeleft.notification.ReminderScheduler
import com.devidea.timeleft.repository.TimeLeftRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject lateinit var focus: FocusCoordinator
    @Inject lateinit var reminders: ReminderCoordinator
    @Inject lateinit var repository: TimeLeftRepository
    @Inject lateinit var telemetry: com.devidea.timeleft.telemetry.AppTelemetry
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        telemetry.initialize()
        ReminderScheduler.createChannel(this)
        // Repair missing work on any process start, independently of reminder/database reads.
        applicationScope.launch { com.devidea.timeleft.widget.WidgetRefreshSchedule.reconcileSafely(this@App) }
        applicationScope.launch {
            repository.items.retryWhen { cause, attempt ->
                if (cause is CancellationException) throw cause
                delay((1_000L * (attempt + 1)).coerceAtMost(60_000L))
                true
            }.collect { refreshReminders() }
        }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            // Includes returning from system notification settings or a detail/editor screen.
            override fun onActivityResumed(activity: Activity) { applicationScope.launch { refreshReminders() } }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private suspend fun refreshReminders() {
        try { focus.refresh(); repository.expirePins(); reminders.refresh() }
        catch (exception: CancellationException) { throw exception }
        catch (exception: Exception) { Log.e("ReminderRefresh", "Refresh failed: ${exception.javaClass.simpleName}") }
    }
}
