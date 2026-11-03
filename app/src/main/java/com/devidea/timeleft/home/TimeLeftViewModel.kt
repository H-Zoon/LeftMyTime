package com.devidea.timeleft.home

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.ui.TimeDetailFacts
import com.devidea.timeleft.R
import com.devidea.timeleft.calc.TimeRangePhase
import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.ui.TimeDisplayFactory
import com.devidea.timeleft.database.ItemEntity
import com.devidea.timeleft.database.ItemType
import com.devidea.timeleft.calc.currentOccurrence
import com.devidea.timeleft.notification.ReminderScheduler
import com.devidea.timeleft.database.TimeLeftRepository
import com.devidea.timeleft.telemetry.AppPerformance
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeLoadState(
    val items: List<TimeDisplayItem> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
    val invalidCount: Int = 0,
    val actionFailed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimeLeftViewModel @Inject constructor(
    private val repository: TimeLeftRepository,
    private val displayFactory: TimeDisplayFactory,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val secondTicker: Flow<Unit> = intervalFlow(TICK_INTERVAL_MS)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            replay = 1
        )

    private val dateTicker: Flow<Unit> = secondTicker.map { LocalDate.now() }.distinctUntilChanged().map { Unit }
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

    val topItems: StateFlow<List<TimeDisplayItem>> = combine(secondTicker, calendarItems) { _, calendar ->
        buildTopItems(calendar)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = buildTopItems(calendarItems.value)
        )

    private data class RepositoryState(
        val items: List<ItemEntity> = emptyList(),
        val loading: Boolean = true,
        val failed: Boolean = false,
    )
    private val retry = MutableStateFlow(0)
    private val actionFailed = MutableStateFlow(false)
    private val source = repository.items.map { RepositoryState(items = it, loading = false) }
        .onStart { emit(RepositoryState()) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(RepositoryState(loading = false, failed = true))
        }
    val homeState: StateFlow<HomeLoadState> = retry.flatMapLatest {
        flow {
            val dateCache = HomeDateCache(::buildItem)
            var lastSource: RepositoryState? = null
            val trace = AppPerformance.startHomeDataLoad()
            var unfinishedOutcome = AppPerformance.Outcome.Cancelled
            try {
                emitAll(combine(source, secondTicker, actionFailed) { state, _, failedAction ->
                    val today = LocalDate.now()
                    val nowMillis = System.currentTimeMillis()
                    if (lastSource !== state) {
                        dateCache.retainDatesIn(state.items)
                        lastSource = state
                    }
                    val items = state.items.map { raw ->
                        if (raw.type == ItemType.Date) dateCache.item(raw, today, nowMillis) else buildItem(raw)
                    }
                    HomeLoadState(items, state.loading, state.failed, items.count { it.dataError }, failedAction).also { ready ->
                        if (!ready.loading) trace.finish(when {
                            ready.failed -> AppPerformance.Outcome.Error
                            ready.invalidCount > 0 -> AppPerformance.Outcome.Fallback
                            else -> AppPerformance.Outcome.Success
                        })
                    }
                })
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                unfinishedOutcome = AppPerformance.Outcome.Error
                throw error
            } finally {
                // Closing again is a no-op once the first non-loading state has been prepared.
                trace.finish(unfinishedOutcome)
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeLoadState())

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

    private fun buildItem(entity: ItemEntity): TimeDisplayItem = try {
        displayFactory.fromSchedule(entity).also {
            require(it.detailFacts?.validRange != false)
        }
    } catch (exception: Exception) {
        if (exception is CancellationException) throw exception
        TimeDisplayItem(id = entity.id, title = entity.title, category = entity.category,
            colorKey = entity.colorKey, iconKey = entity.iconKey, dataError = true,
            countdownText = context.getString(R.string.home_item_invalid),
            dueText = context.getString(R.string.home_item_invalid_hint),
            detailFacts = TimeDetailFacts(0f, 0, 0, TimeRangePhase.Finished, validRange = false))
    }

    private fun buildCalendarItems(): CalendarItems =
        CalendarItems(
            monthItem = displayFactory.month(),
            yearItem = displayFactory.year()
        )

    private fun buildTopItems(calendarItems: CalendarItems): List<TimeDisplayItem> =
        listOf(displayFactory.today(), calendarItems.monthItem, calendarItems.yearItem)

    private fun intervalFlow(intervalMillis: Long): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(intervalMillis)
        }
    }

    private data class CalendarItems(
        val monthItem: TimeDisplayItem,
        val yearItem: TimeDisplayItem,
    )

    companion object {
        private const val TICK_INTERVAL_MS = 1_000L
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Owned by one home-state collector. Time ranges keep their per-second projection. */
internal class HomeDateCache(private val buildItem: (ItemEntity) -> TimeDisplayItem) {
    private data class Entry(
        val raw: ItemEntity,
        val day: LocalDate,
        val pinExpired: Boolean,
        val display: TimeDisplayItem,
    )

    private val entries = mutableMapOf<Int, Entry>()

    fun retainDatesIn(rows: List<ItemEntity>) {
        if (entries.isNotEmpty()) {
            entries.keys.retainAll(rows.asSequence().filter { it.type == ItemType.Date }.map { it.id }.toHashSet())
        }
    }

    fun item(raw: ItemEntity, today: LocalDate, nowMillis: Long): TimeDisplayItem {
        val pinExpired = raw.isPinned && raw.pinnedUntilMillis?.let { it <= nowMillis } == true
        val cached = entries[raw.id]
        if (cached != null && cached.raw == raw && cached.day == today && cached.pinExpired == pinExpired) {
            return cached.display
        }

        val display = buildItem(raw.currentOccurrence(today))
        entries[raw.id] = Entry(raw, today, pinExpired, display)
        return display
    }
}
