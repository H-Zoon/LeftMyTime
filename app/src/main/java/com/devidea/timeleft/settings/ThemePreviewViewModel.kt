package com.devidea.timeleft.settings

import com.devidea.timeleft.calc.currentOccurrence
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.ui.TimeDisplayFactory
import com.devidea.timeleft.database.TimeLeftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal data class ThemePreviewSnapshot(
    val periods: List<TimeDisplayItem>,
    val items: List<TimeDisplayItem>,
    val capturedAtMillis: Long = System.currentTimeMillis(),
)
internal data class ThemePreviewState(
    val snapshot: ThemePreviewSnapshot? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
)

/** A one-shot, read-only snapshot. Recurrences are projected for display at preview time. */
@HiltViewModel
internal class ThemePreviewViewModel @Inject constructor(
    private val repository: TimeLeftRepository,
    private val generator: TimeDisplayFactory,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ThemePreviewState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init { refresh() }

    fun refresh() {
        loadJob?.cancel()
        mutableState.value = ThemePreviewState()
        loadJob = viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    val items = repository.allItems().map { entity ->
                        generator.fromSchedule(entity.currentOccurrence())
                    }
                    ThemePreviewSnapshot(generator.standardPeriods(), items)
                }
                mutableState.value = ThemePreviewState(snapshot, loading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = ThemePreviewState(loading = false, failed = true)
            }
        }
    }
}
