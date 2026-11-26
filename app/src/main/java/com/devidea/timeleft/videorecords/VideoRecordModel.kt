package com.devidea.timeleft.videorecords

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoRecordModel @Inject constructor(
    private val store: VideoRecordStore,
    private val state: SavedStateHandle,
) : ViewModel() {
    private data class VideoInput(val scheduleId: Int, val uri: Uri, val cameraFile: File?)
    private var pendingInput: VideoInput? = null

    internal var clips by mutableStateOf<List<RecordedVideo>?>(null)
        private set
    internal var draft by mutableStateOf<RecordedVideo?>(null)
        private set
    internal var busy by mutableStateOf(false)
        private set
    internal var error by mutableStateOf<Int?>(null)
        private set

    internal fun load(scheduleId: Int) {
        if (clips != null) return
        perform(R.string.video_record_load_failed) {
            clips = store.list(scheduleId)
            draft = state.get<String>("video_draft")?.let { store.restoreDraft(it) }
        }
    }

    internal fun prepare(scheduleId: Int, uri: Uri, cameraFile: File?) {
        if (busy) {
            // Camera results can arrive while the diary is loading after process recreation.
            pendingInput = VideoInput(scheduleId, uri, cameraFile)
            return
        }
        perform(R.string.video_record_prepare_failed) {
            val staged = store.prepare(uri, cameraFile)
            val previous = draft
            draft = staged
            state["video_draft"] = staged.file.name
            if (clips == null) clips = store.list(scheduleId)
            if (previous != null) store.discard(previous)
        }
    }

    internal fun save(scheduleId: Int) {
        val staged = draft ?: return
        perform(R.string.video_record_save_failed) {
            clips = store.save(scheduleId, staged)
            clearDraft()
        }
    }

    internal fun discard() {
        val staged = draft ?: return
        perform(R.string.video_record_delete_failed) {
            store.discard(staged)
            clearDraft()
        }
    }

    internal fun delete(scheduleId: Int, clip: RecordedVideo) = perform(R.string.video_record_delete_failed) {
        clips = store.delete(scheduleId, clip)
    }

    internal fun showError(message: Int) {
        error = message
    }

    internal fun clearError() {
        error = null
    }

    private fun clearDraft() {
        draft = null
        state.remove<String>("video_draft")
    }

    private fun perform(fallback: Int, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        clearError()
        viewModelScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (problem: VideoRecordProblem) {
                error = problem.errorRes
            } catch (_: Exception) {
                error = fallback
            } finally {
                busy = false
                val input = pendingInput
                pendingInput = null
                if (input != null) prepare(input.scheduleId, input.uri, input.cameraFile)
            }
        }
    }
}
