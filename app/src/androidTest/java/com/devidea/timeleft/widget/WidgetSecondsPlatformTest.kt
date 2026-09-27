package com.devidea.timeleft.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.database.itemdata.ItemType
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class WidgetSecondsPlatformTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun unavailableHostKeepsTheExistingSnapshotEvenWithASavedSecondsPreference() {
        assumeTrue(!WidgetSecondsSupport.available())
        instrumentation.runOnMainSync {
            val item = com.devidea.timeleft.ItemGenerate(context).timeItem()
            val views = AppWidget().previewViews(context, WidgetDimensions(320, 200),
                WidgetConfiguration(WidgetSource.Today, showSeconds = true), item, listOf(item), "clay", false)
            assertNotEquals(0, views.layoutId)
            assertNotNull(views.apply(context, FrameLayout(context)))
        }
    }

    @Test fun existingPreferencesRemainSnapshotsAndSecondsArePerWidget() {
        val prefs = context.getSharedPreferences("widget_seconds_test", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().putString("1", "embedTime").putBoolean("1option", true).apply()
            assertFalse(WidgetConfiguration.read(prefs, 1).showSeconds)
            val chosen = WidgetConfiguration(WidgetSource.Today, showRemaining = false, showSeconds = true)
            chosen.write(prefs, 1)
            WidgetConfiguration(WidgetSource.Month).write(prefs, 2)
            assertEquals(chosen, WidgetConfiguration.read(prefs, 1))
            assertFalse(WidgetConfiguration.read(prefs, 2).showSeconds)
            assertEquals("embedTime", prefs.getString("1", null))
            assertFalse(prefs.getBoolean("1option", true))
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun productionDocumentsUseSupportedOperationsAcrossStatesSizesAndThemes() {
        assumeTrue(Build.VERSION.SDK_INT >= 35 && WidgetSecondsSupport.available())
        val now = System.currentTimeMillis() / 1000 * 1000
        instrumentation.runOnMainSync {
            for (locale in listOf(Locale.ENGLISH, Locale.KOREAN)) {
                for (scale in listOf(1f, 2f)) {
                    val configured = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                        setLocale(locale); fontScale = scale
                    })
                    for (design in listOf("time-focus", "time-board")) for (dark in listOf(false, true)) {
                        val palette = WidgetPalette.create("clay", dark, design)
                        val item = AdapterItem(id = 1, title = "긴 시간 구간 / Long schedule title", type = ItemType.Time,
                            startLabel = "10:00 AM", endLabel = "11:00 AM", startsAtMillis = now, endsAtMillis = now + 3_600_000)
                        val plan = WidgetSecondsPlan.create(WidgetConfiguration(WidgetSource.Custom, 1), item, now)!!
                        for (size in listOf(WidgetDimensions(250, 160), WidgetDimensions(320, 200), WidgetDimensions(400, 320))) {
                            for (moment in listOf<Long?>(null, now - 1_000, now, now + 3_600_000)) {
                                val views = WidgetSecondsRenderer.create(configured, size, item, plan, palette, true, moment)
                                assertEquals(0, views.layoutId)
                                // Inflates with the framework player, not AndroidX's embedded player.
                                assertNotNull(views.apply(configured, FrameLayout(configured)))
                            }
                        }
                    }
                }
            }
        }
    }
}
