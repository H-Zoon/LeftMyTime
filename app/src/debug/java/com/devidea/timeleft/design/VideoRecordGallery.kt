package com.devidea.timeleft.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.devidea.timeleft.R
import com.devidea.timeleft.videorecords.RecordedVideo
import com.devidea.timeleft.videorecords.VideoRecordScreen
import java.io.File
import java.time.Instant
import java.time.LocalDate

@Composable
internal fun VideoRecordGallery(screen: String) {
    val korean = LocalConfiguration.current.locales[0].language == "ko"
    val day = LocalDate.of(2026, 11, 28)
    val title = if (screen.endsWith("-large")) {
        if (korean) "친구들과 오랜만에 함께 가는 제주 가을 여행" else "An autumn weekend in Jeju with old friends"
    } else {
        if (korean) "제주 여행" else "A weekend in Jeju"
    }
    val cacheDir = LocalContext.current.cacheDir
    val fixtures = remember(cacheDir) {
        listOf(
            RecordedVideo(File(cacheDir, "gallery-first.mp4"), Instant.parse("2026-11-24T09:35:00Z"), 2_100L),
            RecordedVideo(File(cacheDir, "gallery-second.mp4"), Instant.parse("2026-11-24T14:10:00Z"), 4_800L),
            RecordedVideo(File(cacheDir, "gallery-third.mp4"), Instant.parse("2026-11-26T09:15:00Z"), 3_000L),
        )
    }
    var clips by remember(screen) {
        mutableStateOf(
            when (screen) {
                "video-record-error" -> null
                "video-record-empty", "video-record-draft" -> emptyList()
                else -> fixtures
            },
        )
    }
    var draft by remember(screen) { mutableStateOf(fixtures.first().takeIf { screen == "video-record-draft" }) }
    VideoRecordScreen(
        title = title, target = day, clips = clips, draft = draft, busy = false,
        error = R.string.video_record_load_failed.takeIf { screen == "video-record-error" },
        today = LocalDate.of(2026, 11, 26),
        onRecord = { draft = fixtures.first() },
        onPick = { draft = fixtures.first() },
        onSave = { clips = clips.orEmpty() + listOfNotNull(draft); draft = null },
        onDiscard = { draft = null },
        onDelete = { clip -> clips = clips.orEmpty().filterNot { it == clip } },
    )
}
