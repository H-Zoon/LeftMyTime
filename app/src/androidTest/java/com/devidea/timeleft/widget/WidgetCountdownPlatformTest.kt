package com.devidea.timeleft.widget

import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId

/** The Chronometer widget is plain XML, so it renders on every launcher and API level. */
@RunWith(AndroidJUnit4::class)
class WidgetCountdownPlatformTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    private fun render(item: AdapterItem, plan: WidgetSecondsPlan, now: Long, size: WidgetDimensions, board: Boolean = false): View {
        val countdown = requireNotNull(WidgetCountdown.create(plan, now, SystemClock.elapsedRealtime(), ZoneId.systemDefault()))
        val palette = WidgetPalette.create("clay", false, if (board) "time-board" else "time-focus")
        val views = createSingleWidgetViews(context, size, WidgetConfiguration(WidgetSource.Custom, item.id, showSeconds = true),
            item, emptyList(), palette, R.string.widget_no_upcoming, true, null, countdown)
        lateinit var root: View
        instrumentation.runOnMainSync { root = views.apply(context, FrameLayout(context)) }
        return root
    }

    private fun timed(now: Long, startOffset: Long, endOffset: Long) = AdapterItem(id = 7, title = "Seconds QA", type = ItemType.Time,
        startsAtMillis = now + startOffset, endsAtMillis = now + endOffset, startLabel = "09:00", endLabel = "10:00")

    @Test fun countdownReplacesThePercentTextWithACountingDownChronometer() {
        val now = System.currentTimeMillis()
        val item = timed(now, -60_000, 3_600_000)
        val plan = requireNotNull(WidgetSecondsPlan.create(WidgetConfiguration(WidgetSource.Custom, 7, showSeconds = true), item, now))
        for (board in listOf(false, true)) {
            val root = render(item, plan, now, WidgetDimensions(340, 260), board)
            val chronometer = root.findViewById<Chronometer>(R.id.widgetChronometer)
            assertTrue(chronometer.isCountDown)
            assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widgetSecondsGroup).visibility)
            assertEquals(View.GONE, root.findViewById<View>(R.id.percent).visibility)
            val status = listOf(R.id.widgetSecondsStatusInline, R.id.widgetSecondsStatusBelow).map { root.findViewById<TextView>(it) }
                .single { it.visibility == View.VISIBLE }
            assertEquals(context.getString(R.string.widget_seconds_left), status.text.toString())
            assertTrue("The countdown must show a clock, not a negative or empty text: ${chronometer.text}",
                Regex("""\d{1,3}:\d{2}(:\d{2})?""").matches(chronometer.text))
        }
    }

    @Test fun aStaticRenderReappliedOverACountdownHidesTheOldChronometer() {
        // Launchers reapply new RemoteViews onto the existing hierarchy; stale state would keep counting past zero.
        val now = System.currentTimeMillis()
        val item = timed(now, -60_000, 3_600_000)
        val plan = requireNotNull(WidgetSecondsPlan.create(WidgetConfiguration(WidgetSource.Custom, 7, showSeconds = true), item, now))
        val size = WidgetDimensions(340, 260)
        val root = render(item, plan, now, size)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widgetSecondsGroup).visibility)
        val staticViews = createSingleWidgetViews(context, size, WidgetConfiguration(WidgetSource.Custom, 7),
            item, emptyList(), WidgetPalette.create("clay", false, "time-focus"), R.string.widget_no_upcoming, true)
        instrumentation.runOnMainSync { staticViews.reapply(context, root) }
        assertEquals(View.GONE, root.findViewById<View>(R.id.widgetSecondsGroup).visibility)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.percent).visibility)
    }

    @Test fun aWaitingRangeCountsDownToItsStart() {
        val now = System.currentTimeMillis()
        val item = timed(now, 90_000, 3_600_000)
        val plan = requireNotNull(WidgetSecondsPlan.create(WidgetConfiguration(WidgetSource.Custom, 7, showSeconds = true), item, now))
        val root = render(item, plan, now, WidgetDimensions(340, 260))
        val status = listOf(R.id.widgetSecondsStatusInline, R.id.widgetSecondsStatusBelow).map { root.findViewById<TextView>(it) }
            .single { it.visibility == View.VISIBLE }
        assertEquals(context.getString(R.string.widget_seconds_until_start), status.text.toString())
    }

    @Test fun aWidgetThatCannotFitTheNumberKeepsTheEnlargeHint() {
        val now = System.currentTimeMillis()
        val item = timed(now, -60_000, 3_600_000)
        val plan = requireNotNull(WidgetSecondsPlan.create(WidgetConfiguration(WidgetSource.Custom, 7, showSeconds = true), item, now))
        val countdown = requireNotNull(WidgetCountdown.create(plan, now, SystemClock.elapsedRealtime(), ZoneId.systemDefault()))
        val views = createSingleWidgetViews(context, WidgetDimensions(120, 80), WidgetConfiguration(WidgetSource.Custom, 7, showSeconds = true),
            item, emptyList(), WidgetPalette.create("clay", false), R.string.widget_no_upcoming, true, null, countdown)
        assertEquals(R.layout.app_widget_resize, views.layoutId)
    }
}
