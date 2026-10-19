package com.devidea.timeleft.widget

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class WidgetCountdownTest {
    private val utc = ZoneId.of("UTC")
    private val elapsedNow = 5_000_000L

    /** Mirrors Chronometer.updateText: countdown seconds are truncated toward zero. */
    private fun shownSeconds(base: Long, elapsed: Long): Long = (base - elapsed) / 1000

    private fun create(plan: WidgetSecondsPlan, now: Long, zone: ZoneId = utc, running: Boolean = true) =
        WidgetCountdown.create(plan, now, elapsedNow, zone, running)

    @Test fun `active range counts down to the end second`() {
        val plan = WidgetSecondsPlan(false, startSecond = 1_000, endSecond = 4_600)
        val countdown = requireNotNull(create(plan, 2_000_400L))
        assertEquals(4_600_000L, countdown.targetMillis)
        assertFalse(countdown.waiting)
        assertEquals(2_600L, countdown.remainingSeconds) // 2599.6 s left, shown as the next whole second
        assertEquals(elapsedNow + 2_599_600L + WidgetCountdown.CEIL_SHIFT_MILLIS, countdown.baseElapsedMillis)
    }

    @Test fun `waiting range counts down to the start and says until start`() {
        val plan = WidgetSecondsPlan(false, startSecond = 1_000, endSecond = 4_600)
        val countdown = requireNotNull(create(plan, 940_000L))
        assertEquals(1_000_000L, countdown.targetMillis)
        assertTrue(countdown.waiting)
        assertEquals(60L, countdown.remainingSeconds)
        assertEquals(com.devidea.timeleft.R.string.widget_seconds_until_start, countdown.statusRes)
        assertEquals(com.devidea.timeleft.R.string.widget_seconds_left,
            requireNotNull(create(plan, 1_000_000L)).statusRes)
    }

    @Test fun `an ended range has no countdown so the static ended state is shown`() {
        val plan = WidgetSecondsPlan(false, startSecond = 1_000, endSecond = 4_600)
        assertNull(create(plan, 4_600_000L))
        assertNull(create(plan, 9_000_000L))
    }

    @Test fun `Chronometer shows the next whole second until the target and never goes negative before the grace ends`() {
        val plan = WidgetSecondsPlan(false, startSecond = 0, endSecond = 100)
        val now = 10_250L
        val countdown = requireNotNull(create(plan, now))
        fun shownAt(wall: Long) = shownSeconds(countdown.baseElapsedMillis, elapsedNow + (wall - now))
        assertEquals(90L, shownAt(now)) // 89.75 s left
        assertEquals(1L, shownAt(99_999L))
        assertEquals(1L, shownAt(99_001L))
        assertEquals(0L, shownAt(100_000L)) // the target itself
        assertEquals(0L, shownAt(101_998L)) // still 00:00 ...
        assertEquals(-1L, shownAt(101_999L)) // ... the first negative text comes ~2 s after the target
    }

    @Test fun `today ends with its last second and rolls over with the date change`() {
        val plan = WidgetSecondsPlan(today = true)
        val morning = ZonedDateTime.of(2026, 10, 19, 9, 30, 15, 500_000_000, utc).toInstant().toEpochMilli()
        val countdown = requireNotNull(create(plan, morning))
        assertEquals(ZonedDateTime.of(2026, 10, 19, 23, 59, 59, 0, utc).toInstant().toEpochMilli(), countdown.targetMillis)
        assertFalse(countdown.waiting)
        val lastSecond = ZonedDateTime.of(2026, 10, 19, 23, 59, 59, 0, utc).toInstant().toEpochMilli()
        assertNull(create(plan, lastSecond))
    }

    @Test fun `today follows real elapsed time on a 23 hour daylight saving day`() {
        val zone = ZoneId.of("America/New_York")
        val plan = WidgetSecondsPlan(today = true)
        val start = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, zone).toInstant().toEpochMilli() // spring forward
        val countdown = requireNotNull(create(plan, start, zone))
        assertEquals(23 * 3600L - 1, (countdown.targetMillis - start) / 1000)
        assertEquals(23 * 3600L - 1, countdown.remainingSeconds)
    }

    @Test fun `the widest text follows the range of the next render`() {
        fun widest(seconds: Long) = WidgetCountdown(0, 0, true, false, seconds).widestText
        assertEquals("00:00", widest(59))
        assertEquals("00:00", widest(3_599))
        assertEquals("1:00:00", widest(3_600))
        assertEquals("25:00:00", widest(90_000))
        assertEquals("168:00:00", widest(7 * 86_400L))
    }

    @Test fun `the exact alarm is armed a second early unless that moment has already passed`() {
        assertEquals(99_000L, WidgetCountdown.alarmAtMillis(100_000L, 10_000L))
        assertEquals(99_000L, WidgetCountdown.alarmAtMillis(100_000L, 98_999L))
        assertEquals(100_000L, WidgetCountdown.alarmAtMillis(100_000L, 99_000L)) // the early moment itself is not ahead
        assertEquals(100_000L, WidgetCountdown.alarmAtMillis(100_000L, 99_750L))
        assertEquals(100_000L, WidgetCountdown.alarmAtMillis(100_000L, 100_500L)) // never earlier than now
    }

    /** Measured on the Galaxy A33: alarm to widget posted took about 1.8 s cold and is assumed 0.5 s warm. */
    private val coldAlarmToPostMillis = 1_800L
    private val warmAlarmToPostMillis = 500L
    /** The Chronometer first shows a negative text this long after the target. */
    private val negativeAfterMillis = 1_999L

    @Test fun `a cold start posts the ended state well inside the Chronometer grace`() {
        val plan = WidgetSecondsPlan(false, startSecond = 0, endSecond = 100)
        val first = requireNotNull(create(plan, 10_250L))
        val alarmAt = WidgetCountdown.alarmAtMillis(first.targetMillis, 10_250L)
        assertEquals(99_000L, alarmAt)
        val posted = alarmAt + coldAlarmToPostMillis
        assertNull("The widget must already be the ended state", create(plan, posted))
        val margin = negativeAfterMillis - (posted - first.targetMillis)
        assertTrue("Only $margin ms of grace left", margin >= 1_000L)
    }

    @Test fun `a warm start renders once more at the target and never swaps before the end`() {
        val plan = WidgetSecondsPlan(false, startSecond = 0, endSecond = 100)
        val first = requireNotNull(create(plan, 10_250L))
        val firstAlarm = WidgetCountdown.alarmAtMillis(first.targetMillis, 10_250L)
        val earlyPost = firstAlarm + warmAlarmToPostMillis
        // Still running: the early alarm only refreshed the countdown, which re-arms at the target.
        val again = requireNotNull(create(plan, earlyPost))
        assertEquals(first.targetMillis, again.targetMillis)
        val secondAlarm = WidgetCountdown.alarmAtMillis(again.targetMillis, earlyPost)
        assertEquals(first.targetMillis, secondAlarm)
        val posted = secondAlarm + warmAlarmToPostMillis
        assertNull(create(plan, posted))
        assertTrue(negativeAfterMillis - (posted - first.targetMillis) >= 1_000L)
    }

    @Test fun `without the lead a cold start would have used almost the whole grace`() {
        // The measurement behind ALARM_LEAD_MILLIS: arming at the target left about 0.2 s.
        assertEquals(199L, negativeAfterMillis - coldAlarmToPostMillis)
    }

    @Test fun `a frozen preview does not start the Chronometer`() {
        val plan = WidgetSecondsPlan(false, 1_000, 4_600)
        assertFalse(requireNotNull(create(plan, 2_000_000L, running = false)).running)
        assertTrue(requireNotNull(create(plan, 2_000_000L)).running)
    }

    @Test fun `an upcoming plan far from its start counts the whole gap`() {
        val plan = WidgetSecondsPlan(false, 1_000, 4_600)
        val reference = Instant.ofEpochSecond(500).toEpochMilli()
        val countdown = requireNotNull(create(plan, reference))
        assertTrue(countdown.waiting)
        assertEquals(500L, countdown.remainingSeconds)
    }
}
