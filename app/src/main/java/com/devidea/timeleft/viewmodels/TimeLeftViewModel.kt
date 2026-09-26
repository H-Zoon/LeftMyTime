package com.devidea.timeleft.viewmodels

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.TimeDetailFacts
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.InterfaceItem
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.calc.currentOccurrence
import com.devidea.timeleft.notification.ReminderScheduler
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.widget.AppWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
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
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeLoadState(
    val items: List<AdapterItem> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
    val invalidCount: Int = 0,
    val actionFailed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
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

    private val dateTicker: Flow<Unit> = secondTicker.map { java.time.LocalDate.now() }.distinctUntilChanged().map { Unit }
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

    private data class RepositoryState(val items: List<ItemEntity> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)
    private val retry = MutableStateFlow(0)
    private val actionFailed = MutableStateFlow(false)
    private val source = retry.flatMapLatest {
        repository.items.map { RepositoryState(items = it, loading = false) }
            .onStart { emit(RepositoryState()) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(RepositoryState(loading = false, failed = true))
            }
    }
    // One bad record remains editable in All schedules; it cannot terminate the rest of the flow.
    private val dateCache = mutableMapOf<Int, Pair<Pair<ItemEntity, java.time.LocalDate>, AdapterItem>>()
    val homeState: StateFlow<HomeLoadState> = combine(source, secondTicker, actionFailed) { state, _, failedAction ->
        val today = java.time.LocalDate.now()
        dateCache.keys.retainAll(state.items.map { it.id }.toSet())
        val items = state.items.map { raw ->
            val entity = raw.currentOccurrence(today)
            if (entity.type == ItemType.Date) {
                val key = entity to today
                dateCache[entity.id]?.takeIf { it.first == key }?.second ?: buildItem(entity).also {
                    dateCache[entity.id] = key to it
                }
            } else buildItem(entity)
        }
        HomeLoadState(items, state.loading, state.failed, items.count { it.dataError }, failedAction)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeLoadState())

    val customItems: StateFlow<List<AdapterItem>> = homeState.map { it.items }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun retryLoading() { actionFailed.value = false; retry.value++ }

    fun deleteItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.delete(id)
                ReminderScheduler.cancel(context, id)
                AppWidget.updateAllWidgets(context, AppWidgetManager.getInstance(context))
            } catch (exception: CancellationException) { throw exception }
            catch (_: Exception) { actionFailed.value = true }
        }
    }

    private fun buildItem(entity: ItemEntity): AdapterItem = try {
        (if (entity.type == ItemType.Time) itemGenerate.customTimeItem(entity) else itemGenerate.customMonthItem(entity)).also {
            require(it.detailFacts?.validRange != false)
        }
    } catch (exception: Exception) {
        if (exception is CancellationException) throw exception
        AdapterItem(id = entity.id, title = entity.title, category = entity.category,
            colorKey = entity.colorKey, iconKey = entity.iconKey, dataError = true,
            countdownText = context.getString(R.string.home_item_invalid),
            dueText = context.getString(R.string.home_item_invalid_hint),
            detailFacts = TimeDetailFacts(0f, 0, 0, TimeRangePhase.Finished, validRange = false))
    }

    private fun buildCalendarItems(): CalendarItems =
        CalendarItems(
            monthItem = itemGenerate.monthItem(),
            yearItem = itemGenerate.yearItem()
        )

    private fun buildTopItems(calendarItems: CalendarItems): List<AdapterItem> =
        listOf(itemGenerate.timeItem(), calendarItems.monthItem, calendarItems.yearItem)

    private fun intervalFlow(intervalMillis: Long): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(intervalMillis)
        }
    }

    private data class CalendarItems(
        val monthItem: AdapterItem,
        val yearItem: AdapterItem,
    )

    companion object {
        private const val TICK_INTERVAL_MS = 1_000L
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
