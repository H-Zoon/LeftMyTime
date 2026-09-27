package com.devidea.timeleft.widget

import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.database.itemdata.ItemType
import java.time.Instant
import java.time.ZoneId

/** A display plan, never a source of reminder delivery or persisted completion. */
internal data class WidgetSecondsPlan(
    val today: Boolean,
    val startSecond: Long = 0,
    val endSecond: Long = 86_399,
) {
    data class Frame(val phase: TimeRangePhase, val seconds: Long, val elapsedFraction: Float)

    fun frame(now: Instant, zone: ZoneId): Frame {
        if (today) {
            val second = now.atZone(zone).toLocalTime().toSecondOfDay().toLong()
            return Frame(if (second >= 86_399) TimeRangePhase.Finished else TimeRangePhase.Active,
                (86_399 - second).coerceAtLeast(0), second / 86_400f)
        }
        val second = now.epochSecond
        return when {
            second < startSecond -> Frame(TimeRangePhase.Upcoming, startSecond - second, 0f)
            second >= endSecond -> Frame(TimeRangePhase.Finished, 0, 1f)
            else -> Frame(TimeRangePhase.Active, endSecond - second,
                (second - startSecond).toFloat() / (endSecond - startSecond))
        }
    }

    fun nextBoundaryMillis(nowMillis: Long): Long? = when {
        today -> null // Local clock expressions roll into the next day without an app update.
        nowMillis < startSecond * 1000 -> startSecond * 1000
        nowMillis < endSecond * 1000 -> endSecond * 1000
        else -> null
    }

    companion object {
        // Keep integer subtraction and float formatting exact in the supported short-time scope.
        const val MAX_SECONDS = 7 * 86_400L

        fun create(configuration: WidgetConfiguration, item: AdapterItem?, nowMillis: Long): WidgetSecondsPlan? {
            if (item == null || item.dataError || item.isFocusSession || configuration.legacySummary) return null
            if (configuration.source == WidgetSource.Today) return WidgetSecondsPlan(today = true)
            if (configuration.source !in listOf(WidgetSource.Custom, WidgetSource.Next) || item.type != ItemType.Time) return null
            val start = item.startsAtMillis ?: return null
            val end = item.endsAtMillis ?: return null
            if (end <= start || start < 0 || end > Int.MAX_VALUE.toLong() * 1000) return null
            // Non-integral imported endpoints become due on the next tick, never before the endpoint.
            val startSecond = (start + 999) / 1000
            val endSecond = (end + 999) / 1000
            val nowSecond = nowMillis / 1000
            if (endSecond - startSecond !in 1..MAX_SECONDS ||
                startSecond - nowSecond !in -MAX_SECONDS..MAX_SECONDS ||
                endSecond - nowSecond !in -MAX_SECONDS..MAX_SECONDS) return null
            return WidgetSecondsPlan(false, startSecond, endSecond)
        }
    }
}
