package com.devidea.timeleft.videorecords

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.devidea.timeleft.R
import com.devidea.timeleft.config.UserPreferences
import com.devidea.timeleft.database.ItemType
import com.devidea.timeleft.ui.TimeDetailRange
import com.devidea.timeleft.ui.TimeDisplayItem
import com.devidea.timeleft.ui.theme.TimeLeftTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

internal val TimeDisplayItem.canRecordVideo: Boolean
    get() = id > 0 && type == ItemType.Date && !dataError && !isCalendarOccurrence &&
        detailFacts?.let { it.validRange && it.range is TimeDetailRange.Calendar } == true

class VideoCaptureFileProvider : FileProvider()

@AndroidEntryPoint
class VideoRecordActivity : AppCompatActivity() {
    private val model: VideoRecordModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(SCHEDULE, 0)
        val title = intent.getStringExtra(TITLE)
        val target = intent.getStringExtra(DAY)?.let(LocalDate::parse)
        if (id <= 0 || title == null || target == null) {
            finish()
            return
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setContent {
            var captureName by rememberSaveable { mutableStateOf<String?>(null) }
            val camera = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val capture = captureName?.let { File(cameraDirectory(), it) }
                captureName = null
                if (capture != null) {
                    revokeUriPermission(cameraUri(capture), CAMERA_GRANTS)
                    if (result.resultCode == Activity.RESULT_OK) {
                        val source = if (capture.length() > 0L) Uri.fromFile(capture) else result.data?.data
                        if (source != null) model.prepare(id, source, capture)
                        else {
                            capture.delete()
                            model.showError(R.string.video_record_prepare_failed)
                        }
                    } else capture.delete()
                }
            }
            val pick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { model.prepare(id, it, null) }
                }
            }
            val onBack = { if (!model.busy) finish() }
            BackHandler(onBack = onBack)
            LaunchedEffect(id) { model.load(id) }
            TimeLeftTheme(
                themeMode = prefs.getString(UserPreferences.KEY_THEME, null) ?: UserPreferences.THEME_AUTO,
                paletteKey = prefs.getString(UserPreferences.KEY_COLOR_THEME, null) ?: UserPreferences.COLOR_THEME_CLAY,
                designKey = prefs.getString(UserPreferences.KEY_DESIGN, null) ?: UserPreferences.DESIGN_TIME_FOCUS,
            ) {
                VideoRecordScreen(
                    title = title,
                    target = target,
                    clips = model.clips,
                    draft = model.draft,
                    busy = model.busy,
                    error = model.error,
                    onBack = onBack,
                    onRetry = { model.load(id) },
                    onClearError = model::clearError,
                    onRecord = {
                        val directory = cameraDirectory()
                        val targetFile = File(directory, "${UUID.randomUUID()}.mp4")
                        try {
                            if (!directory.isDirectory && !directory.mkdirs()) {
                                throw IOException()
                            }
                            val output = cameraUri(targetFile)
                            captureName = targetFile.name
                            camera.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                                putExtra(MediaStore.EXTRA_OUTPUT, output)
                                putExtra(MediaStore.EXTRA_DURATION_LIMIT, VideoRecordStore.MAX_CAPTURE_SECONDS)
                                putExtra(MediaStore.EXTRA_SIZE_LIMIT, VideoRecordStore.MAX_BYTES)
                                putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                                clipData = ClipData.newRawUri("", output)
                                addFlags(CAMERA_GRANTS)
                            })
                        } catch (_: ActivityNotFoundException) {
                            captureName = null
                            targetFile.delete()
                            model.showError(R.string.video_record_camera_unavailable)
                        } catch (_: Exception) {
                            captureName = null
                            targetFile.delete()
                            model.showError(R.string.video_record_prepare_failed)
                        }
                    },
                    onPick = {
                        try {
                            pick.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                type = "video/mp4"
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                            })
                        } catch (_: ActivityNotFoundException) {
                            model.showError(R.string.video_record_picker_unavailable)
                        }
                    },
                    onSave = { model.save(id) },
                    onDiscard = model::discard,
                    onDelete = { model.delete(id, it) },
                )
            }
        }
    }

    private fun cameraDirectory() = File(cacheDir, "video-record-camera")

    private fun cameraUri(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.video-records", file)

    companion object {
        private const val SCHEDULE = "schedule_id"
        private const val TITLE = "schedule_title"
        private const val DAY = "target_day"
        private const val CAMERA_GRANTS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        internal fun open(context: Context, item: TimeDisplayItem) {
            val day = (item.detailFacts?.range as? TimeDetailRange.Calendar)?.end ?: return
            context.startActivity(Intent(context, VideoRecordActivity::class.java)
                .putExtra(SCHEDULE, item.id).putExtra(TITLE, item.title).putExtra(DAY, day.toString()))
        }
    }
}
