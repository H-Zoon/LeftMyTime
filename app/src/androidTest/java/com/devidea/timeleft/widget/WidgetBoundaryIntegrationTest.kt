package com.devidea.timeleft.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.Lifecycle
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class WidgetBoundaryIntegrationTest {
    @Test fun delayedAndDuplicateBroadcastsReadTheNewSelectionAndCancelTheOldBoundary() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val host = AppWidgetHost(context, 0x534543)
        val manager = AppWidgetManager.getInstance(context)
        val prefs = EntryPointAccessors.fromApplication(context.applicationContext, AppWidget.AppWidgetEntryPoint::class.java).prefs()
        val boundaries = context.getSharedPreferences("widget_boundaries", Context.MODE_PRIVATE)
        val id = host.allocateAppWidgetId()
        try {
            instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
            try { assertTrue(manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, AppWidget::class.java))) }
            finally { instrumentation.uiAutomation.dropShellPermissionIdentity() }
            WidgetConfiguration(WidgetSource.Today, showSeconds = true).write(prefs, id)
            runBlocking { AppWidget.updateAllWidgetsAndAwait(context, manager) }
            val oldBoundary = System.currentTimeMillis() + 120_000
            WidgetBoundarySchedule.record(context, id, oldBoundary)
            assertEquals(oldBoundary, boundaries.getLong(id.toString(), 0))

            // Change selection before an old event is delivered. Its payload has no stale item data.
            WidgetConfiguration(WidgetSource.Month).write(prefs, id)
            val action = Intent(context, WidgetBoundaryReceiver::class.java).setAction(WidgetBoundarySchedule.ACTION)
            context.sendBroadcast(action)
            context.sendBroadcast(action)
            val deadline = SystemClock.elapsedRealtime() + 5_000
            while (boundaries.contains(id.toString()) && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(20)
            assertFalse("The obsolete alarm must be cancelled after the receiver finishes posting", boundaries.contains(id.toString()))
            assertEquals(WidgetSource.Month, WidgetConfiguration.read(prefs, id).source)
            assertFalse(WidgetConfiguration.read(prefs, id).showSeconds)

            // Tapping an expired document refreshes the CURRENT widget and returns to the launcher;
            // it must not expose another occurrence through the old item link.
            val expired = WidgetDetailsActivity.createIntent(context, id, WidgetSource.Next, -1)
                .putExtra(WidgetDetailsActivity.SECONDS_VALID_UNTIL, System.currentTimeMillis() - 1_000)
            ActivityScenario.launch<WidgetDetailsActivity>(expired).use { scenario ->
                val closeDeadline = SystemClock.elapsedRealtime() + 5_000
                while (scenario.state != Lifecycle.State.DESTROYED && SystemClock.elapsedRealtime() < closeDeadline) SystemClock.sleep(20)
                assertEquals(Lifecycle.State.DESTROYED, scenario.state)
                assertEquals(WidgetSource.Month, WidgetConfiguration.read(prefs, id).source)
            }
        } finally {
            host.deleteAppWidgetId(id)
            WidgetBoundarySchedule.record(context, id, null)
            prefs.edit().remove(id.toString()).remove("${id}option").remove(WidgetConfiguration.displayKey(id))
                .remove(WidgetConfiguration.secondsKey(id)).commit()
        }
        assertNull(manager.getAppWidgetInfo(id))
        assertFalse(boundaries.contains(id.toString()))
    }
}
