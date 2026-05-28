package com.devidea.timeleft.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.InterfaceItem
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.notification.ReminderScheduler
import com.devidea.timeleft.repository.TimeLeftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class TimeLeftViewModel @Inject constructor(
    private val repository: TimeLeftRepository,
    private val itemGenerate: InterfaceItem,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val secondTicker: Flow<Unit> = intervalFlow(TICK_INTERVAL_MS)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            replay = 1
        )

    private val expiryTicker: Flow<Unit> = intervalFlow(EXPIRY_CHECK_INTERVAL_MS)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            replay = 1
        )

    private val dateTicker: Flow<Unit> = dateChangeFlow()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            replay = 1
        )

    private val calendarItems: StateFlow<CalendarItems> = dateTicker
        .map { buildCalendarItems() }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = buildCalendarItems()
        )

    val topItems: StateFlow<List<AdapterItem>> = combine(secondTicker, calendarItems) { _, calendar ->
        buildTopItems(calendar)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = buildTopItems(calendarItems.value)
        )

    // `distinctUntilChanged` gates this so `rescheduleAll` only fires when the entity list
    // actually changes content — i.e. on CRUD or when a recurring Date item rolls over to
    // its next cycle (which mutates startValue/endValue). The per-minute expiryTicker
    // alone does not trigger reschedules.
    private val advancedItems: StateFlow<List<ItemEntity>> = repository.items
        .combine(expiryTicker) { entities, _ ->
            repository.advanceExpiredRecurrences(entities)
        }
        .distinctUntilChanged()
        .onEach { items -> ReminderScheduler.rescheduleAll(context, items) }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList()
        )

    private val dateItems: StateFlow<Map<Int, AdapterItem>> = advancedItems
        .combine(dateTicker) { entities, _ ->
            buildDateItems(entities)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyMap()
        )

    private val timeItems: StateFlow<Map<Int, AdapterItem>> = advancedItems
        .combine(secondTicker) { entities, _ ->
            buildTimeItems(entities)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyMap()
        )

    val customItems: StateFlow<List<AdapterItem>> =
        combine(advancedItems, dateItems, timeItems, ::buildCustomItems)
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = emptyList()
            )

    fun deleteItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(id)
        }
    }

    private fun buildCalendarItems(): CalendarItems =
        CalendarItems(
            monthItem = itemGenerate.monthItem(),
            yearItem = itemGenerate.yearItem()
        )

    private fun buildTopItems(calendarItems: CalendarItems): List<AdapterItem> =
        listOf(itemGenerate.timeItem(), calendarItems.monthItem, calendarItems.yearItem)

    private fun buildDateItems(entities: List<ItemEntity>): Map<Int, AdapterItem> =
        entities.asSequence()
            .filter { it.type == ItemType.Date }
            .associate { it.id to itemGenerate.customMonthItem(it) }

    private fun buildTimeItems(entities: List<ItemEntity>): Map<Int, AdapterItem> =
        entities.asSequence()
            .filter { it.type == ItemType.Time }
            .associate { it.id to itemGenerate.customTimeItem(it) }

    private fun buildCustomItems(
        entities: List<ItemEntity>,
        dateItems: Map<Int, AdapterItem>,
        timeItems: Map<Int, AdapterItem>,
    ): List<AdapterItem> = entities.map { entity ->
        when (entity.type) {
            ItemType.Time -> timeItems[entity.id] ?: itemGenerate.customTimeItem(entity)
            ItemType.Date -> dateItems[entity.id] ?: itemGenerate.customMonthItem(entity)
        }
    }

    private fun intervalFlow(intervalMillis: Long): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(intervalMillis)
        }
    }

    private fun dateChangeFlow(): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(millisUntilNextDate())
        }
    }

    private fun millisUntilNextDate(): Long {
        val now = ZonedDateTime.now()
        val nextDate = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        return Duration.between(now, nextDate).toMillis().coerceAtLeast(MIN_DATE_TICK_DELAY_MS)
    }

    private data class CalendarItems(
        val monthItem: AdapterItem,
        val yearItem: AdapterItem,
    )

    companion object {
        private const val TICK_INTERVAL_MS = 1_000L
        private const val EXPIRY_CHECK_INTERVAL_MS = 60_000L
        private const val MIN_DATE_TICK_DELAY_MS = 1_000L
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
