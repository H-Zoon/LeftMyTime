package com.devidea.timeleft.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.InterfaceItem
import com.devidea.timeleft.ItemGenerate
import com.devidea.timeleft.datadase.itemdata.ItemDao
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import com.devidea.timeleft.repository.TimeLeftRepository
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

class TimeLeftViewModel(private val repository: TimeLeftRepository) : ViewModel() {

    private val itemGenerate: InterfaceItem = ItemGenerate()

    private val ticker: Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(TICK_INTERVAL_MS)
        }
    }

    val timeValue: StateFlow<String> = ticker
        .map { LocalDateTime.now().format(TIME_FORMATTER) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LocalDateTime.now().format(TIME_FORMATTER)
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

    val customItems: StateFlow<List<AdapterItem>> = repository.items
        .combine(ticker) { entities, _ ->
            repository.advanceExpiredRecurrences(entities).map(::toAdapterItem)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = emptyList()
        )

    private fun toAdapterItem(entity: ItemEntity): AdapterItem =
        if (entity.type == "Time") itemGenerate.customTimeItem(entity)
        else itemGenerate.customMonthItem(entity)

    fun deleteItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(id)
        }
    }

    companion object {
        private const val TICK_INTERVAL_MS = 1_000L
        private const val STOP_TIMEOUT_MS = 5_000L
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("a h:m:ss")
    }
}

class TimeLeftViewModelFactory(
    private val itemDao: ItemDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimeLeftViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimeLeftViewModel(TimeLeftRepository(itemDao)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
