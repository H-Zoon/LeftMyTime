package com.devidea.timeleft.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalendarImportState(
    val permission: Boolean = false, val calendars: List<DeviceCalendar> = emptyList(),
    val calendarId: Long? = null, val days: Int = 30,
    val preview: CalendarImportPreview? = null, val selected: Set<String> = emptySet(),
    val loading: Boolean = false, val saving: Boolean = false, val error: Int? = null, val imported: Int? = null,
)

@HiltViewModel
class CalendarImportViewModel @Inject constructor(private val repository: CalendarImportRepository) : ViewModel() {
    private val mutable = MutableStateFlow(CalendarImportState())
    val state = mutable.asStateFlow()
    private var load: Job? = null

    fun checkPermission() {
        if (mutable.value.saving) return
        if (!repository.hasPermission()) {
            load?.cancel()
            mutable.value = CalendarImportState(days = mutable.value.days)
        } else if (!mutable.value.permission || (!mutable.value.loading && mutable.value.preview == null && mutable.value.imported == null)) refresh()
    }

    fun refresh(calendarId: Long? = mutable.value.calendarId, days: Int = mutable.value.days) {
        if (mutable.value.saving) return
        load?.cancel()
        mutable.value = mutable.value.copy(permission = repository.hasPermission(), loading = true,
            days = days, calendarId = calendarId, error = null, preview = null, selected = emptySet(), imported = null)
        load = viewModelScope.launch(Dispatchers.IO) {
            try {
                val calendars = repository.calendars()
                val id = calendars.firstOrNull { it.id == calendarId }?.id ?: calendars.firstOrNull()?.id
                val preview = id?.let { repository.preview(it, days) }
                currentCoroutineContext().ensureActive()
                mutable.value = mutable.value.copy(permission = true, calendars = calendars, calendarId = id, preview = preview, loading = false)
            } catch (error: CancellationException) { throw error }
            catch (_: SecurityException) { currentCoroutineContext().ensureActive(); mutable.value = CalendarImportState(days = days) }
            catch (_: Exception) { currentCoroutineContext().ensureActive(); mutable.value = mutable.value.copy(loading = false, error = R.string.calendar_load_error) }
        }
    }

    fun toggle(key: String) {
        val current = mutable.value
        if (current.loading || current.saving || current.imported != null || current.preview == null ||
            key in current.preview.alreadyImported || current.preview.entries.none { it.sourceKey == key }) return
        val selected = current.selected.toMutableSet()
        if (!selected.remove(key) && selected.size < CalendarImportRepository.MAX_SELECTION) selected.add(key)
        mutable.value = current.copy(selected = selected)
    }

    fun save() {
        val current = mutable.value
        val preview = current.preview ?: return
        if (current.loading || current.saving || current.selected.isEmpty() || current.imported != null) return
        mutable.value = current.copy(saving = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = repository.import(preview, current.selected)
                mutable.value = mutable.value.copy(saving = false, imported = count, selected = emptySet(),
                    preview = preview.copy(alreadyImported = preview.alreadyImported + current.selected))
            } catch (error: CancellationException) { throw error }
            catch (_: SecurityException) { mutable.value = CalendarImportState(days = current.days) }
            catch (_: CalendarPreviewChanged) { mutable.value = mutable.value.copy(saving = false, selected = emptySet(), preview = null, error = R.string.calendar_changed) }
            catch (_: Exception) { mutable.value = mutable.value.copy(saving = false, error = R.string.calendar_save_error) }
        }
    }
}
