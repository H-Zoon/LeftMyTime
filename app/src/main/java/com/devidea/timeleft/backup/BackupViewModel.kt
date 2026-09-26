package com.devidea.timeleft.backup

import android.appwidget.AppWidgetManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.R
import com.devidea.timeleft.notification.ReminderCoordinator
import com.devidea.timeleft.widget.AppWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupState(val busy: Boolean = false, val preview: RestorePreview? = null, val message: Int? = null, val restored: Int? = null)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backups: ScheduleBackupRepository,
    private val reminders: ReminderCoordinator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BackupState())
    val state = mutableState.asStateFlow()

    fun export(uri: Uri) = perform(R.string.backup_export_failed) {
        val content = backups.export()
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(content); it.flush() }
        mutableState.value = BackupState(message = R.string.backup_exported)
    }

    fun inspect(uri: Uri) = perform(R.string.backup_import_failed) {
        val imported = requireNotNull(context.contentResolver.openInputStream(uri)).use(ScheduleBackupCodec::decode)
        mutableState.value = BackupState(preview = backups.preview(imported))
    }

    fun cancelPreview() { if (!mutableState.value.busy) mutableState.value = BackupState() }

    fun restore(mode: RestoreMode, selectedPin: String?) {
        val preview = mutableState.value.preview ?: return
        perform(R.string.backup_restore_failed, keepPreview = true) {
            val count = try { backups.restore(preview, mode, selectedPin) }
            catch (_: RestorePreviewChanged) {
                mutableState.value = BackupState(preview = backups.preview(preview.imported), message = R.string.backup_preview_changed)
                return@perform
            }
            // Committed data remains valid if the platform refuses a subsequent alarm/widget refresh.
            val refreshed = runCatching {
                reminders.refresh(force = true)
                AppWidget.updateAllWidgets(context, AppWidgetManager.getInstance(context))
            }.isSuccess
            mutableState.value = BackupState(restored = count,
                message = if (refreshed) R.string.backup_restored else R.string.backup_restored_refresh_pending)
        }
    }

    private fun perform(failure: Int, keepPreview: Boolean = false, block: suspend () -> Unit) {
        if (mutableState.value.busy) return
        val preview = mutableState.value.preview.takeIf { keepPreview }
        mutableState.value = BackupState(busy = true, preview = preview)
        viewModelScope.launch(Dispatchers.IO) {
            try { block() }
            catch (exception: CancellationException) { throw exception }
            catch (_: Exception) { mutableState.value = BackupState(preview = preview, message = failure) }
        }
    }
}
