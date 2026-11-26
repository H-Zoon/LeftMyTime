package com.devidea.timeleft.videorecords

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

private val thumbnailRequests = Semaphore(2)

@Composable
internal fun ClipThumbnail(
    file: File,
    modifier: Modifier,
    cache: LruCache<File, Bitmap>? = null,
    showPlay: Boolean = false,
) {
    val bitmap by produceState(cache?.get(file), file) {
        if (value != null) return@produceState
        value = thumbnailRequests.withPermit {
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    if (Build.VERSION.SDK_INT >= 27) {
                        retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 384, 216)
                    } else {
                        retriever.getFrameAtTime(0)?.let { Bitmap.createScaledBitmap(it, 384, 216, true) }
                    }
                } catch (_: Exception) {
                    null
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }
        value?.let { cache?.put(file, it) }
    }
    val image = bitmap
    Box(
        modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) Image(image.asImageBitmap(), contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (showPlay) ContentScale.Crop else ContentScale.Fit)
        if (showPlay || image == null) {
            Icon(
                if (showPlay) Icons.Default.PlayArrow else Icons.Outlined.VideoLibrary,
                contentDescription = null,
                tint = if (image != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (image != null) Modifier.clip(MaterialTheme.shapes.small)
                    .background(Color.Black.copy(alpha = .56f)).padding(Spacing.xs) else Modifier,
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun ClipPlayerDialog(videos: List<RecordedVideo>, start: RecordedVideo, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var failed by remember(videos, start.file) { mutableStateOf(false) }
    var aspect by remember(videos, start.file) { mutableFloatStateOf(16f / 9f) }
    val maxVideoHeight = LocalConfiguration.current.screenHeightDp.dp * .62f
    val player = remember(videos, start.file, context) {
        val sources = ProgressiveMediaSource.Factory(DataSource.Factory { FileDataSource() })
        ExoPlayer.Builder(context).setMediaSourceFactory(sources).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setHandleAudioBecomingNoisy(true)
            val index = videos.indexOfFirst { it.file == start.file }
            setMediaItems(videos.map { MediaItem.fromUri(Uri.fromFile(it.file)) }, index, 0)
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player, lifecycle) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = (videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height)
                        .coerceIn(9f / 16f, 21f / 9f)
                }
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        player.addListener(listener)
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.video_record_play), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.video_record_close_player))
                    }
                }
                AndroidView(
                    factory = { PlayerView(it).apply { setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING) } },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxWidth().heightIn(max = maxVideoHeight).aspectRatio(aspect),
                )
                if (failed) Text(
                    stringResource(R.string.video_record_playback_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
