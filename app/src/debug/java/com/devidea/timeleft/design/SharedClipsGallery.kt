package com.devidea.timeleft.design

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.devidea.timeleft.R
import com.devidea.timeleft.sharedclips.AlbumEvent
import com.devidea.timeleft.sharedclips.AlbumPlayback
import com.devidea.timeleft.sharedclips.SharedAlbum
import com.devidea.timeleft.sharedclips.SharedClip
import com.devidea.timeleft.sharedclips.SharedClipsPage
import com.devidea.timeleft.sharedclips.SharedClipsScreen
import java.io.File
import java.time.Instant
import java.time.LocalDate

@Composable
internal fun SharedClipsGallery(screen: String) {
    val korean = LocalConfiguration.current.locales[0].language == "ko"
    val day = LocalDate.of(2026, 11, 28)
    val title = if (korean) "제주 여행" else "A weekend in Jeju"
    val longTitle = if (korean) "친구들과 오랜만에 함께 가는 제주 가을 여행" else "An autumn weekend in Jeju with old friends"
    val album = SharedAlbum("gallery", if (screen.endsWith("-large")) longTitle else title, day, isOwner = true)
    val cacheDir = LocalContext.current.cacheDir
    val clips = remember(cacheDir) {
        listOf(
            SharedClip(Uri.fromFile(File(cacheDir, "gallery-first.mp4")).toString(), Instant.parse("2026-11-24T09:35:00Z"), true),
            SharedClip(Uri.fromFile(File(cacheDir, "gallery-second.mp4")).toString(), Instant.parse("2026-11-24T14:10:00Z"), false),
            SharedClip(Uri.fromFile(File(cacheDir, "gallery-third.mp4")).toString(), Instant.parse("2026-11-26T09:15:00Z"), false),
        )
    }
    val populated = SharedClipsPage.Album(AlbumPlayback(album, clips, "", emptyMap()))
    var page by remember(screen) {
        mutableStateOf<SharedClipsPage?>(
            when (screen) {
                "album-error" -> null
                "album-directory" -> SharedClipsPage.Directory(listOf(album))
                "album-directory-empty" -> SharedClipsPage.Directory(emptyList())
                "album-create" -> SharedClipsPage.Create(AlbumEvent(1, album.title, day))
                "album-empty" -> SharedClipsPage.Album(AlbumPlayback(album, emptyList(), "", emptyMap()))
                else -> populated
            },
        )
    }
    var code by remember { mutableStateOf<String?>(null) }
    SharedClipsScreen(
        page = page,
        busy = false,
        error = R.string.shared_clips_auth_disabled.takeIf { screen == "album-error" },
        today = LocalDate.of(2026, 11, 26),
        invitation = code,
        onBack = {},
        onOpen = { page = populated },
        onCreate = { page = populated },
        onJoin = { page = populated },
        onIssueInvitation = { code = "7d72d07a-d56d-4685-aa91-2a07925f7307" },
    )
}
