package com.devidea.timeleft.sharedclips

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devidea.timeleft.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

internal data class AlbumEvent(val id: Int, val title: String, val date: LocalDate)

internal sealed interface SharedClipsPage {
    data class Directory(val albums: List<SharedAlbum>) : SharedClipsPage
    data class Create(val event: AlbumEvent) : SharedClipsPage
    data class Album(val playback: AlbumPlayback) : SharedClipsPage
}

internal class SharedClipsModel(application: Application) : AndroidViewModel(application) {
    private val store = SharedClipsStore.get(application)
    private var event: AlbumEvent? = null
    private var directory: SharedClipsPage.Directory? = null

    var page by mutableStateOf<SharedClipsPage?>(null)
        private set
    var selection by mutableStateOf<Uri?>(null)
        private set
    var invitation by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<Int?>(null)
        private set

    private var uploadId: String? = null

    fun start(event: AlbumEvent?) {
        this.event = event
        if (page == null && error == null) refresh()
    }

    fun refresh() = perform {
        val current = page
        val target = event
        page = when {
            current is SharedClipsPage.Album -> SharedClipsPage.Album(store.open(current.playback.album.id))
            target != null -> {
                val existing = store.find(target.id, target.date)
                if (existing == null) SharedClipsPage.Create(target) else SharedClipsPage.Album(existing)
            }
            else -> SharedClipsPage.Directory(store.albums()).also { directory = it }
        }
    }

    fun open(album: SharedAlbum) = perform {
        page = SharedClipsPage.Album(store.open(album.id))
        invitation = null
    }

    fun create(event: AlbumEvent) = perform {
        page = SharedClipsPage.Album(store.create(event.id, event.title, event.date))
    }

    fun join(code: String) = perform {
        val albumId = store.join(code)
        // Keep a route back to the joined albums even if opening the album fails.
        directory = SharedClipsPage.Directory(store.albums()).also { page = it }
        page = SharedClipsPage.Album(store.open(albumId))
        invitation = null
    }

    fun invite() {
        val album = (page as? SharedClipsPage.Album)?.playback?.album ?: return
        perform { invitation = store.invite(album.id) }
    }

    fun select(video: Uri) {
        if (page !is SharedClipsPage.Album) return
        selection = video
        uploadId = UUID.randomUUID().toString()
        clearError()
    }

    fun clearSelection() {
        selection = null
        uploadId = null
        clearError()
    }

    fun upload() {
        val selected = selection ?: return
        val id = uploadId ?: return
        val current = (page as? SharedClipsPage.Album)?.playback ?: return
        perform {
            store.upload(current.album.id, selected, id)
            clearSelection()
            page = SharedClipsPage.Album(store.open(current.album.id))
        }
    }

    fun delete(clip: SharedClip) {
        val current = (page as? SharedClipsPage.Album)?.playback ?: return
        perform {
            store.delete(clip.path)
            page = SharedClipsPage.Album(store.open(current.album.id))
        }
    }

    fun goUp(): Boolean {
        if (busy) return true
        val previous = directory ?: return false
        if (page !is SharedClipsPage.Album) return false
        page = previous
        invitation = null
        clearSelection()
        return true
    }

    fun clearError() {
        error = null
    }

    private fun perform(work: suspend () -> Unit) {
        if (busy) return
        busy = true
        clearError()
        viewModelScope.launch {
            try {
                work()
            } catch (error: CancellationException) {
                throw error
            } catch (_: AnonymousSignInDisabled) {
                error = R.string.shared_clips_auth_disabled
            } catch (_: InvalidInvitation) {
                error = R.string.shared_clips_join_error
            } catch (_: InvalidClip) {
                error = R.string.shared_clips_invalid
            } catch (_: Exception) {
                error = R.string.shared_clips_network_error
            } finally {
                busy = false
            }
        }
    }
}
