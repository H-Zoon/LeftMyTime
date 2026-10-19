package com.devidea.timeleft.widget

import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import java.time.Instant
import java.time.ZoneId

/**
 * Seconds for launchers that cannot draw draw-instructions. The number is a host-ticked
 * Chronometer and needs no app process; the ruler stays a snapshot of the last update, and an
 * exact alarm at [targetMillis] replaces the widget before the Chronometer could go negative.
 */
internal data class WidgetCountdown(
    /** Wall-clock second the countdown reaches zero: the start while waiting, otherwise the end. */
    val targetMillis: Long,
    /** Chronometer base on the SystemClock.elapsedRealtime() clock, already shifted by [CEIL_SHIFT_MILLIS]. */
    val baseElapsedMillis: Long,
    val running: Boolean,
    val waiting: Boolean,
    val remainingSeconds: Long,
) {
    val statusRes: Int get() = if (waiting) R.string.widget_seconds_until_start else R.string.widget_seconds_left

    /** Widest text until the next render: seconds only ever count down. Digits are tabular. */
    val widestText: String get() = if (remainingSeconds >= 3600) "${remainingSeconds / 3600}:00:00" else "00:00"

    companion object {
        /**
         * Chronometer truncates toward zero. The shift shows 00:01 until the target, 00:00 from it,
         * and the first negative text only about two seconds later, the grace for the exact alarm.
         */
        const val CEIL_SHIFT_MILLIS = 999L

        /**
         * Measured on a Galaxy A33: a cold-started app received the exact alarm about 1.1 s late and had
         * posted the widget 1.7 to 1.8 s late, nearly all of the roughly 2 s the Chronometer stays at 00:00.
         * The alarm is therefore armed this early. A render that still finds the countdown running (a warm
         * app) arms it again for the target itself, so the ended state never appears before the end.
         */
        const val ALARM_LEAD_MILLIS = 1_000L

        /** When to arm the exact alarm for [targetMillis] when rendering at [nowMillis]. */
        fun alarmAtMillis(targetMillis: Long, nowMillis: Long): Long {
            val early = targetMillis - ALARM_LEAD_MILLIS
            return if (early > nowMillis) early else targetMillis
        }

        /** A finished plan has no countdown: the static widget shows the ended state. */
        fun create(
            plan: WidgetSecondsPlan,
            referenceMillis: Long,
            elapsedRealtimeMillis: Long,
            zone: ZoneId,
            running: Boolean = true,
        ): WidgetCountdown? {
            val frame = plan.frame(Instant.ofEpochMilli(referenceMillis), zone)
            val target = when (frame.phase) {
                TimeRangePhase.Finished -> return null
                TimeRangePhase.Upcoming -> plan.startSecond * 1000
                // Today ends with its last second, as in the draw-instruction document.
                TimeRangePhase.Active -> if (plan.today) endOfTodayMillis(referenceMillis, zone) else plan.endSecond * 1000
            }
            val remaining = target - referenceMillis
            if (remaining <= 0) return null
            return WidgetCountdown(
                targetMillis = target,
                baseElapsedMillis = elapsedRealtimeMillis + remaining + CEIL_SHIFT_MILLIS,
                running = running,
                waiting = frame.phase == TimeRangePhase.Upcoming,
                remainingSeconds = (remaining + 999) / 1000,
            )
        }

        /** 23:59:59 local time, correct on 23 and 25 hour days. */
        fun endOfTodayMillis(referenceMillis: Long, zone: ZoneId): Long =
            Instant.ofEpochMilli(referenceMillis).atZone(zone).toLocalDate().plusDays(1)
                .atStartOfDay(zone).toInstant().toEpochMilli() - 1000
    }
}

/** The ruler is a snapshot, so a countdown widget refreshes it on the same period as static ones. */
internal const val WIDGET_SNAPSHOT_REFRESH_MILLIS = 15 * 60_000L
