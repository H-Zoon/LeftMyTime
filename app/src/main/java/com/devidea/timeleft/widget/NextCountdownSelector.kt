package com.devidea.timeleft.widget

import com.devidea.timeleft.focus.isFocusSession
import com.devidea.timeleft.calendar.isTimedOccurrence
import com.devidea.timeleft.focus.FocusSession
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.calc.scheduledTimeWindow
import java.time.Duration
import java.time.ZoneId
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal object NextCountdownSelector {
    fun select(entities: List<ItemEntity>, now: LocalTime = LocalTime.now(), today: LocalDate = LocalDate.now(),
        clock: com.devidea.timeleft.focus.FocusClockReading? = null): ItemEntity? =
        entities.mapNotNull { raw -> runCatching {
            val entity = clock?.let { FocusSession.withClock(raw, today.atTime(now).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), it) } ?: raw
            entity to rank(entity, now, today)
        }.getOrNull() }
            .filterNot { it.second.expired }
            .minWithOrNull(compareBy<Pair<ItemEntity, CountdownRank>> { it.second.priority }
                .thenBy { it.second.seconds }.thenBy { it.first.id })?.first

    private fun rank(entity: ItemEntity, now: LocalTime, today: LocalDate): CountdownRank = when (entity.type) {
        ItemType.Time -> {
            val at = today.atTime(now).atZone(ZoneId.systemDefault())
            val window = if (entity.isFocusSession) null else scheduledTimeWindow(entity, at)
            if (entity.isFocusSession) {
                val remaining = FocusSession.remaining(entity, at.toInstant().toEpochMilli())
                CountdownRank(0, remaining / 1000, entity.focusState != FocusSession.RUNNING || remaining == 0L)
            } else if (window == null) CountdownRank(2, Long.MAX_VALUE, true) else {
                val active = window.phase == TimeRangePhase.Active
                CountdownRank(if (active) 0 else 1, Duration.between(at, if (active) window.end else window.start).seconds,
                    entity.isTimedOccurrence && window.phase == TimeRangePhase.Finished)
            }
        }
        ItemType.Date -> {
            val start = LocalDate.parse(entity.startValue, DATE)
            val end = LocalDate.parse(entity.endValue, DATE)
            require(!end.isBefore(start))
            val days = ChronoUnit.DAYS.between(today, end)
            CountdownRank(1, days * 86400, days < 0)
        }
    }

    private data class CountdownRank(val priority: Int, val seconds: Long, val expired: Boolean)
    private val TIME = DateTimeFormatter.ofPattern("H:m")
    private val DATE = DateTimeFormatter.ofPattern("yyyy-M-d")
}
