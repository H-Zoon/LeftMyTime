package com.devidea.timeleft.widget

import android.content.Context
import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.TimeDisplayFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class WidgetPreviewPlatformTest {
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun context(language: String, scale: Float): Context = base.createConfigurationContext(
        Configuration(base.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
            fontScale = scale
        },
    )

    @Test fun currentSizeIsPreservedWhenTheContentFits() {
        val context = context("ko", 1f)
        val size = WidgetDimensions(320, 240)
        val preview = widgetContentPreview(size, WidgetSource.Custom) {
            WidgetPreview(it, android.widget.RemoteViews(context.packageName, R.layout.app_widget_single))
        }
        assertEquals(size, preview.dimensions)
        assertTrue(preview.fits)
    }

    @Test fun longTitlesAndLargeTextProduceContentPreviewsInBothLayouts() {
        for (language in listOf("ko", "en")) {
            for (scale in listOf(1f, 1.5f, 2f)) {
                val context = context(language, scale)
                val periods = TimeDisplayFactory(context).standardPeriods()
                val item = periods.first().copy(title = "A long schedule title / 긴 일정 제목 ".repeat(4))
                for (design in listOf("time-focus", "time-board")) {
                    for (source in listOf(WidgetSource.Custom, WidgetSource.Overview)) {
                        val preview = widgetContentPreview(WidgetDimensions.minimumFor(source), source) { size ->
                            val palette = WidgetPalette.create("clay", false, design)
                            val views = if (source == WidgetSource.Overview) {
                                createOverviewViews(context, size, periods, palette)
                            } else {
                                createSingleWidgetViews(context, size, WidgetConfiguration(source), item, periods,
                                    palette, R.string.widget_no_upcoming, true)
                            }
                            WidgetPreview(size, views)
                        }
                        assertTrue("$language/$scale/$design/$source", preview.fits)
                        assertNotEquals(R.layout.app_widget_resize, preview.views.layoutId)
                    }
                }
            }
        }
    }

    @Test fun missingOverviewDataStopsGrowingInsteadOfLoopingForever() {
        val context = context("ko", 1f)
        var attempts = 0
        val palette = WidgetPalette.create("clay", false)
        val preview = widgetContentPreview(WidgetDimensions.OverviewMinimum, WidgetSource.Overview) { size ->
            attempts++
            check(attempts <= 16) { "Preview growth did not stop" }
            WidgetPreview(size, createOverviewViews(context, size, emptyList(), palette))
        }
        assertFalse(preview.fits)
        assertEquals(R.layout.app_widget_resize, preview.views.layoutId)
    }

    @Test
    @SdkSuppress(minSdkVersion = 35)
    fun drawInstructionPreviewUsesItsMeasuredFitInsteadOfTheLayoutId() {
        val context = context("en", 2f)
        val now = System.currentTimeMillis()
        val item = TimeDisplayFactory(context).today()
        val configuration = WidgetConfiguration(WidgetSource.Today, showSeconds = true)
        val plan = requireNotNull(WidgetSecondsPlan.create(configuration, item, now))
        val palette = WidgetPalette.create("clay", true)
        val measured = mutableListOf<Boolean>()
        val preview = widgetContentPreview(WidgetDimensions.Minimum, WidgetSource.Today) { size ->
            var fits = false
            val views = WidgetSecondsRenderer.create(context, size, item, plan, palette, true, now,
                WidgetSecondsProfile.V6, onSizeMeasured = {
                    fits = it
                    measured.add(it)
                })
            WidgetPreview(size, views, fits)
        }
        assertTrue(preview.fits)
        assertTrue(measured.contains(false))
        assertTrue(measured.last())
    }
}
