package com.devidea.timeleft.widget

import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.calc.timeRangeSnapshot
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal object NextCountdownSelector {
    fun select(entities: List<ItemEntity>, now: LocalTime = LocalTime.now(), today: LocalDate = LocalDate.now()): ItemEntity? =
        entities.map { it to rank(it, now, today) }
            .filterNot { it.second.expired }
            .minWithOrNull(compareBy<Pair<ItemEntity, CountdownRank>> { it.second.priority }
                .thenBy { it.second.seconds }.thenBy { it.first.id })?.first
            ?: entities.firstOrNull()

    private fun rank(entity: ItemEntity, now: LocalTime, today: LocalDate): CountdownRank = when (entity.type) {
        ItemType.Time -> {
            val snapshot = timeRangeSnapshot(LocalTime.parse(entity.startValue, TIME), LocalTime.parse(entity.endValue, TIME), now)
            val active = snapshot.phase == TimeRangePhase.Active
            CountdownRank(if (active) 0 else 1, if (active) snapshot.secondsLeft else snapshot.secondsUntilStart, false)
        }
        ItemType.Date -> {
            val days = ChronoUnit.DAYS.between(today, LocalDate.parse(entity.endValue, DATE))
            CountdownRank(1, days * 86400, days < 0)
        }
    }

    private data class CountdownRank(val priority: Int, val seconds: Long, val expired: Boolean)
    private val TIME = DateTimeFormatter.ofPattern("H:m")
    private val DATE = DateTimeFormatter.ofPattern("yyyy-M-d")
}
