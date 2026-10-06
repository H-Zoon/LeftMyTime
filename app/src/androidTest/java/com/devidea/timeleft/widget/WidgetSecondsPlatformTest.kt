package com.devidea.timeleft.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.database.itemdata.ItemType
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

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

    @Test fun releasedDocumentsDrawAcrossStatesSizesAndThemes() {
        assumeTrue(Build.VERSION.SDK_INT >= 35 && WidgetSecondsSupport.available())
        checkDocuments(requireNotNull(WidgetSecondsSupport.capability().productionProfile))
    }

    @Test fun releasedTodayPreviewUsesTheProductionSecondsPath() {
        assumeTrue(Build.VERSION.SDK_INT >= 35 && WidgetSecondsSupport.available())
        instrumentation.runOnMainSync {
            val item = com.devidea.timeleft.ItemGenerate(context).timeItem()
            val size = WidgetDimensions(400, 320)
            val views = AppWidget().previewViews(context, size,
                WidgetConfiguration(WidgetSource.Today, showSeconds = true), item, listOf(item), "clay", false)
            assertDraws(views, size, context)
        }
    }

    @Test fun candidateDocumentsDrawOnTheirActualLowerVersionHost() {
        val capability = WidgetSecondsSupport.capability()
        assumeTrue(Build.VERSION.SDK_INT >= 35 && capability.productionProfile == null && capability.validationProfile != null)
        checkDocuments(requireNotNull(capability.validationProfile))
    }

    @Test fun fullProductDocumentsHaveTheLegacyOrMapHeaderRequiredByTheirHost() {
        assumeTrue(Build.VERSION.SDK_INT >= 35)
        val now = System.currentTimeMillis()
        instrumentation.runOnMainSync {
            val configured = context.createConfigurationContext(Configuration(context.resources.configuration).apply { fontScale = 1f })
            val item = AdapterItem(title = "Seconds QA", type = ItemType.Time,
                startsAtMillis = now, endsAtMillis = now + 60_000)
            val size = WidgetDimensions(400, 320)
            val palette = WidgetPalette.create("clay", false, "time-focus")
            for (source in listOf(WidgetSource.Today, WidgetSource.Custom)) {
                val plan = WidgetSecondsPlan.create(WidgetConfiguration(source), item, now)!!
                for (profile in WidgetSecondsProfile.entries) {
                    val bytes = WidgetSecondsRenderer.createDocument(configured, size, item, plan, palette, true, null, profile)
                    assertHeader(bytes, profile, size, configured)
                }
            }
        }
    }

    @RequiresApi(35)
    private fun checkDocuments(profile: WidgetSecondsProfile) {
        val now = System.currentTimeMillis() / 1000 * 1000
        val midnight = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
                        for (source in listOf(WidgetSource.Today, WidgetSource.Custom)) {
                            val plan = WidgetSecondsPlan.create(WidgetConfiguration(source, 1), item, now)!!
                            val moments = if (source == WidgetSource.Today) listOf(null, now, midnight - 1_000, midnight)
                                else listOf(null, now - 1_000, now, now + 3_600_000)
                            for (size in listOf(WidgetDimensions(250, 160), WidgetDimensions(320, 200), WidgetDimensions(400, 320))) {
                                for (moment in moments) {
                                    val bytes = WidgetSecondsRenderer.createDocument(configured, size, item, plan, palette, true, moment, profile)
                                    assertHeader(bytes, profile, size, configured)
                                    val views = RemoteViews(RemoteViews.DrawInstructions.Builder(listOf(bytes)).build())
                                    assertDraws(views, size, configured)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun assertHeader(bytes: ByteArray, profile: WidgetSecondsProfile, size: WidgetDimensions, context: Context) {
        DataInputStream(ByteArrayInputStream(bytes)).use { header ->
            assertEquals("Header operation", 0, header.readUnsignedByte())
            when (profile) {
                WidgetSecondsProfile.V6 -> {
                    // A V7 property-map header is silently rejected by a V6 framework player.
                    assertEquals(1, header.readInt())
                    assertEquals(0, header.readInt())
                    assertEquals(0, header.readInt())
                    assertEquals((size.width * context.resources.displayMetrics.density).roundToInt(), header.readInt())
                    assertEquals((size.height * context.resources.displayMetrics.density).roundToInt(), header.readInt())
                    assertEquals(0L, header.readLong())
                }
                WidgetSecondsProfile.V7 -> {
                    assertEquals(0x048C0001, header.readInt())
                    assertEquals(1, header.readInt())
                    assertEquals(0, header.readInt())
                    assertTrue("Header properties", header.readInt() > 0)
                }
            }
        }
    }

    private fun assertDraws(views: RemoteViews, size: WidgetDimensions, context: Context) {
        assertEquals(0, views.layoutId)
        val width = (size.width * context.resources.displayMetrics.density).roundToInt()
        val height = (size.height * context.resources.displayMetrics.density).roundToInt()
        val parent = FrameLayout(context)
        // Exercise the framework player; merely returning a View can hide rejected documents.
        parent.addView(views.apply(context, parent))
        parent.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        parent.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            parent.draw(Canvas(bitmap))
            assertTrue("Framework player must render visible content",
                (height / 4 until height * 3 / 4 step 8).any { y ->
                    (width / 4 until width * 3 / 4 step 8).any { x -> Color.alpha(bitmap.getPixel(x, y)) > 0 }
                })
        } finally {
            bitmap.recycle()
            parent.removeAllViews()
        }
        // First-frame drawing is not proof of ticking, TalkBack, or launcher performance.
    }
}
