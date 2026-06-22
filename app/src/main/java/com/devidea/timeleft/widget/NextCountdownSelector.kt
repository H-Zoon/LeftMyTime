package com.devidea.timeleft.widget

import com.devidea.timeleft.calc.CustomTimeProgress
import com.devidea.timeleft.calc.TimeProgressCalculator
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal object NextCountdownSelector {

    fun select(
        entities: List<ItemEntity>,
        now: LocalTime = LocalTime.now(),
        today: LocalDate = LocalDate.now(),
    ): ItemEntity? {
        var selectedEntity: ItemEntity? = null
        var selectedSortKey = Long.MAX_VALUE

        entities.forEach { entity ->
            val rank = rank(entity, now, today)
            if (!rank.isExpired && (selectedEntity == null || rank.remainingSortKey < selectedSortKey)) {
                selectedEntity = entity
                selectedSortKey = rank.remainingSortKey
            }
        }

        return selectedEntity ?: entities.firstOrNull()
    }

    private fun rank(
        entity: ItemEntity,
        now: LocalTime,
        today: LocalDate,
    ): CountdownRank = when (entity.type) {
        ItemType.Time -> rankTime(entity, now)
        ItemType.Date -> rankDate(entity, today)
    }

    private fun rankTime(entity: ItemEntity, now: LocalTime): CountdownRank {
        val startTime = LocalTime.parse(entity.startValue, STORAGE_TIME_FORMATTER)
        val endTime = LocalTime.parse(entity.endValue, STORAGE_TIME_FORMATTER)
        return when (val result = TimeProgressCalculator.customTimeProgress(startTime, endTime, now)) {
            is CustomTimeProgress.Active -> CountdownRank(
                remainingSortKey = result.durationLeft.seconds,
                isExpired = false
            )
            CustomTimeProgress.Idle -> CountdownRank(
                remainingSortKey = Long.MAX_VALUE,
                isExpired = false
            )
        }
    }

    private fun rankDate(entity: ItemEntity, today: LocalDate): CountdownRank {
        val endDate = LocalDate.parse(entity.endValue, STORAGE_DATE_FORMATTER)
        val daysLeft = ChronoUnit.DAYS.between(today, endDate)
        return CountdownRank(
            remainingSortKey = if (daysLeft >= 0) daysLeft * SECONDS_PER_DAY else Long.MAX_VALUE,
            isExpired = daysLeft < 0
        )
    }

    private data class CountdownRank(
        val remainingSortKey: Long,
        val isExpired: Boolean,
    )

    private const val SECONDS_PER_DAY = 86_400L
    private val STORAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:m")
    private val STORAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d")
}
