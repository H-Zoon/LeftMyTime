package com.devidea.timeleft.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.InterfaceItem
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.repository.TimeLeftRepository
import com.devidea.timeleft.widget.forWidgetPreview
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
    val periods: List<AdapterItem>,
    val items: List<AdapterItem>,
    val capturedAtMillis: Long = System.currentTimeMillis(),
)
internal data class ThemePreviewState(
    val snapshot: ThemePreviewSnapshot? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
)

/** A one-shot, read-only snapshot. The normal home ViewModel also advances/saves recurrences. */
@HiltViewModel
internal class ThemePreviewViewModel @Inject constructor(
    private val repository: TimeLeftRepository,
    private val generator: InterfaceItem,
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
                        val currentOccurrence = entity.forWidgetPreview()
                        when (entity.type) {
                            ItemType.Time -> generator.customTimeItem(currentOccurrence)
                            ItemType.Date -> generator.customMonthItem(currentOccurrence)
                        }
                    }
                    ThemePreviewSnapshot(listOf(generator.timeItem(), generator.monthItem(), generator.yearItem()), items)
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
