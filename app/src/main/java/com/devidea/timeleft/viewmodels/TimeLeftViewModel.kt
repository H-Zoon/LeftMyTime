package com.devidea.timeleft.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.InterfaceItem
import com.devidea.timeleft.R
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TimeLeftViewModel @Inject constructor(
    private val repository: TimeLeftRepository,
    private val itemGenerate: InterfaceItem,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(context.getString(R.string.pattern_header_time))

    private val ticker: Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(TICK_INTERVAL_MS)
        }
    }

    private val expiryTicker: Flow<Unit> = flow {
        emit(Unit)
        while (currentCoroutineContext().isActive) {
            delay(EXPIRY_CHECK_INTERVAL_MS)
            emit(Unit)
        }
    }

    val timeValue: StateFlow<String> = ticker
        .map { LocalDateTime.now().format(timeFormatter) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LocalDateTime.now().format(timeFormatter)
        )

    val topTimeItem: StateFlow<AdapterItem?> = ticker
        .map { itemGenerate.timeItem() }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = null
        )

    val topItems: StateFlow<List<AdapterItem>> = ticker
        .map {
            listOf(
                itemGenerate.timeItem(),
                itemGenerate.monthItem(),
                itemGenerate.yearItem()
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = listOf(
                itemGenerate.timeItem(),
                itemGenerate.monthItem(),
                itemGenerate.yearItem()
            )
        )

    private val advancedItems: Flow<List<ItemEntity>> = repository.items
        .combine(expiryTicker) { entities, _ ->
            repository.advanceExpiredRecurrences(entities).also {
                ReminderScheduler.rescheduleAll(context, it)
            }
        }
        .flowOn(Dispatchers.IO)

    val customItems: StateFlow<List<AdapterItem>> = advancedItems
        .combine(ticker) { entities, _ ->
            entities.map(::toAdapterItem)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList()
        )

    private fun toAdapterItem(entity: ItemEntity): AdapterItem =
        when (entity.type) {
            ItemType.Time -> itemGenerate.customTimeItem(entity)
            ItemType.Date -> itemGenerate.customMonthItem(entity)
        }

    fun deleteItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(id)
        }
    }

    companion object {
        private const val TICK_INTERVAL_MS = 1_000L
        private const val EXPIRY_CHECK_INTERVAL_MS = 60_000L
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
