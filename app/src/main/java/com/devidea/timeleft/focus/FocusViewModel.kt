package com.devidea.timeleft.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.R
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.repository.TimeLeftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(val items: List<ItemEntity> = emptyList(), val now: Long = System.currentTimeMillis(),
    val loading: Boolean = true, val failed: Boolean = false, val busy: Boolean = false, val error: Int? = null)

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FocusViewModel @Inject constructor(private val repository: TimeLeftRepository, private val focus: FocusCoordinator,
    private val telemetry: com.devidea.timeleft.telemetry.AppTelemetry,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<Int?>(null)
    private val clock = flow {
        while (currentCoroutineContext().isActive) { emit(System.currentTimeMillis()); delay(1000) }
    }
    private val reload = MutableStateFlow(0)
    private val rows = reload.flatMapLatest {
        repository.items.map { FocusUiState(items = it.filter { item -> item.isFocusSession }, loading = false) }
            .onStart { emit(FocusUiState()) }
            .catch { if (it is CancellationException) throw it; emit(FocusUiState(loading = false, failed = true)) }
    }
    val state = combine(rows, clock, busy, error) { data, now, changing, failure ->
        val clock = readFocusClock(context)
        data.copy(items = data.items.map { FocusSession.withClock(it, now, clock) }, now = now, busy = changing, error = failure)
    }.onEach { value ->
        if (value.items.any { it.focusState == FocusSession.RUNNING && (it.focusEndsAt ?: Long.MAX_VALUE) <= value.now }) {
            try { focus.refresh() }
            catch (exception: CancellationException) { throw exception }
            catch (_: Exception) { error.value = R.string.focus_action_failed }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusUiState())

    fun start(minutes: Int, onCreated: (Int) -> Unit) = perform {
        val id = focus.start(minutes)
        telemetry.record(com.devidea.timeleft.telemetry.UsageEvent.FocusStarted)
        onCreated(id)
    }
    fun change(id: Int, action: String) = perform { focus.change(id, action) }
    fun retry() { error.value = null; reload.value++ }

    private fun perform(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; error.value = null
        viewModelScope.launch {
            try { action() }
            catch (exception: CancellationException) { throw exception }
            catch (_: AnotherFocusRunning) { error.value = R.string.focus_another_running }
            catch (_: Exception) { error.value = R.string.focus_action_failed }
            finally { busy.value = false }
        }
    }
}
