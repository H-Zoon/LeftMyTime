package com.devidea.timeleft.videorecords

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.StringRes
import com.devidea.timeleft.R
import com.devidea.timeleft.database.ItemDao
import com.devidea.timeleft.database.ItemEntity
import com.devidea.timeleft.database.ItemType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal data class RecordedVideo(val file: File, val recordedAt: Instant, val durationMillis: Long)
internal class VideoRecordProblem(@param:StringRes val errorRes: Int) : IOException()

@Singleton
class VideoRecordStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val items: ItemDao,
) {
    private val root = File(context.noBackupFilesDir, "video-records")
    private val drafts = File(root, "drafts")
    private val access = Mutex()

    internal suspend fun list(scheduleId: Int): List<RecordedVideo> = withContext(Dispatchers.IO) {
        access.withLock { clipsIn(scheduleFolder(requireDateSchedule(scheduleId).stableId)) }
    }

    internal suspend fun prepare(video: Uri, cameraFile: File?): RecordedVideo = withContext(Dispatchers.IO) {
        // The recording clock belongs to this device even when a saved video is selected.
        val takenAt = Instant.now()
        val copyingContext = coroutineContext
        val name = UUID.randomUUID().toString()
        var temporary: File? = null
        try {
            ensureDirectory(drafts)
            val copying = File(drafts, "$name.part.mp4")
            temporary = copying
            val input = context.contentResolver.openInputStream(video)
                ?: throw VideoRecordProblem(R.string.video_record_invalid)
            input.use { source ->
                copying.outputStream().use { sink ->
                    val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        copyingContext.ensureActive()
                        val count = source.read(bytes)
                        if (count < 0) break
                        total += count
                        if (total > MAX_BYTES) throw VideoRecordProblem(R.string.video_record_invalid)
                        sink.write(bytes, 0, count)
                    }
                }
            }
            val duration = readDuration(copying)
            val ready = File(drafts, "${takenAt.toEpochMilli()}_${duration}_$name.mp4")
            // Rename inside the same volume. Readers only see completed MP4 files.
            Files.move(copying.toPath(), ready.toPath(), StandardCopyOption.ATOMIC_MOVE)
            RecordedVideo(ready, takenAt, duration)
        } finally {
            temporary?.delete()
            cameraFile?.delete()
        }
    }

    internal suspend fun restoreDraft(name: String): RecordedVideo? = withContext(Dispatchers.IO) {
        if (videoName.matches(name)) describe(File(drafts, name)) else null
    }

    internal suspend fun discard(draft: RecordedVideo) = withContext(Dispatchers.IO) {
        if (draft.file.parentFile == drafts) Files.deleteIfExists(draft.file.toPath())
        Unit
    }

    internal suspend fun save(scheduleId: Int, draft: RecordedVideo): List<RecordedVideo> = withContext(Dispatchers.IO) {
        access.withLock {
            require(draft.file.parentFile == drafts && videoName.matches(draft.file.name))
            val folder = scheduleFolder(requireDateSchedule(scheduleId).stableId)
            ensureDirectory(folder)
            val committed = File(folder, draft.file.name)
            if (draft.file.exists()) {
                Files.move(draft.file.toPath(), committed.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } else if (!committed.isFile) {
                throw VideoRecordProblem(R.string.video_record_missing)
            }
            clipsIn(folder)
        }
    }

    internal suspend fun delete(scheduleId: Int, clip: RecordedVideo): List<RecordedVideo> = withContext(Dispatchers.IO) {
        access.withLock {
            val folder = scheduleFolder(requireDateSchedule(scheduleId).stableId)
            require(clip.file.parentFile == folder && videoName.matches(clip.file.name))
            Files.deleteIfExists(clip.file.toPath())
            clipsIn(folder)
        }
    }

    internal suspend fun deleteSchedule(scheduleId: Int, deleteItem: suspend () -> Unit) {
        access.withLock {
            val stableId = items.getSelectItem(scheduleId).stableId
            deleteItem()
            // A failed cleanup can be retried using the existing schedule tombstone.
            withContext(Dispatchers.IO + NonCancellable) {
                if (stableId.isNotBlank()) runCatching { scheduleFolder(stableId).deleteRecursively() }
            }
        }
    }

    internal suspend fun pruneDiscardedFiles() = withContext(Dispatchers.IO) {
        if (!root.isDirectory) return@withContext
        access.withLock {
            val expiredBefore = System.currentTimeMillis() - DRAFT_LIFETIME_MILLIS
            drafts.listFiles()?.filter { it.isFile && it.lastModified() < expiredBefore }?.forEach { it.delete() }
            items.getAllRecords().asSequence()
                .filter { it.deletedAt != null && it.stableId.isNotBlank() }
                .forEach { scheduleFolder(it.stableId).deleteRecursively() }
        }
    }

    private suspend fun requireDateSchedule(id: Int): ItemEntity {
        val item = items.getSelectItem(id)
        if (item.type != ItemType.Date || item.isTemplate || item.stableId.isBlank()) {
            throw VideoRecordProblem(R.string.video_record_schedule_missing)
        }
        return item
    }

    private fun scheduleFolder(stableId: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(stableId.toByteArray())
        return File(root, digest.joinToString("") { "%02x".format(it) })
    }

    private fun clipsIn(folder: File): List<RecordedVideo> {
        if (!folder.exists()) return emptyList()
        val files = folder.listFiles() ?: throw IOException("Unable to read video directory")
        return files.mapNotNull(::describe).sortedWith(compareBy<RecordedVideo> { it.recordedAt }.thenBy { it.file.name })
    }

    private fun describe(file: File): RecordedVideo? {
        val parts = videoName.matchEntire(file.name)?.groupValues ?: return null
        if (!file.isFile) return null
        val epoch = parts[1].toLongOrNull() ?: return null
        val duration = parts[2].toLongOrNull() ?: return null
        return RecordedVideo(file, Instant.ofEpochMilli(epoch), duration)
    }

    private fun ensureDirectory(folder: File) {
        if (!folder.mkdirs() && !folder.isDirectory) throw IOException("Unable to create video directory")
    }

    private fun readDuration(file: File): Long {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (!hasVideo || mime != "video/mp4" || duration == null || duration !in 1..MAX_DURATION_MILLIS) {
                throw VideoRecordProblem(R.string.video_record_invalid)
            }
            return duration
        } catch (_: RuntimeException) {
            throw VideoRecordProblem(R.string.video_record_invalid)
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        internal const val MAX_CAPTURE_SECONDS = 15
        internal const val MAX_BYTES = 32L * 1024L * 1024L
        private const val MAX_DURATION_MILLIS = MAX_CAPTURE_SECONDS * 1000L
        private const val DRAFT_LIFETIME_MILLIS = 24L * 60L * 60L * 1000L
        private val videoName = Regex("([0-9]{1,19})_([0-9]{1,5})_[0-9a-f-]{36}\\.mp4")
    }
}
