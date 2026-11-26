package com.devidea.timeleft.sharedclips

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.devidea.timeleft.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal data class SharedAlbum(val id: String, val title: String, val day: LocalDate, val isOwner: Boolean)
internal data class SharedClip(val path: String, val uploadedAt: Instant, val isMine: Boolean)
internal data class AlbumPlayback(
    val album: SharedAlbum,
    val clips: List<SharedClip>,
    val streamBase: String,
    val headers: Map<String, String>,
)

internal class InvalidClip : IOException()
internal class InvalidInvitation : IOException()
internal class AnonymousSignInDisabled : IOException()

internal class SharedClipsStore private constructor(private val context: Context) {
    companion object {
        private var instance: SharedClipsStore? = null

        @Synchronized
        fun get(context: Context): SharedClipsStore =
            instance ?: SharedClipsStore(context.applicationContext).also { instance = it }
    }

    private val base = BuildConfig.SHARED_CLIPS_URL.trimEnd('/')
    private val key = BuildConfig.SHARED_CLIPS_KEY
    private val preferences = context.getSharedPreferences(
        "shared_countdown_clips_${UUID.nameUUIDFromBytes(base.toByteArray())}", Context.MODE_PRIVATE,
    )
    private var session: Session? = null

    private data class Session(val token: String, val user: String, val expiresAt: Long)
    private class RequestFailed(status: Int) : IOException("HTTP $status")

    suspend fun albums(): List<SharedAlbum> = withContext(Dispatchers.IO) { readAlbums() }

    suspend fun find(localId: Int, date: LocalDate): AlbumPlayback? = withContext(Dispatchers.IO) {
        val id = preferences.getString(binding(localId, date), null) ?: return@withContext null
        readAlbums(id).singleOrNull()?.let(::playback)
    }

    suspend fun create(localId: Int, title: String, date: LocalDate): AlbumPlayback = withContext(Dispatchers.IO) {
        val binding = binding(localId, date)
        val id = preferences.getString(binding, null) ?: UUID.randomUUID().toString().also {
            check(preferences.edit().putString(binding, it).commit())
        }
        rpc("create_countdown_album", JSONObject().put("album", id).put("label", title).put("day", date.toString()))
        playback(readAlbums(id).single())
    }

    suspend fun join(code: String): String = withContext(Dispatchers.IO) {
        rpc("join_countdown_album", JSONObject().put("invitation", code)) as String
    }

    suspend fun invite(album: String): String = withContext(Dispatchers.IO) {
        rpc("invite_to_countdown_album", JSONObject().put("album", album)) as String
    }

    suspend fun open(albumId: String): AlbumPlayback = withContext(Dispatchers.IO) {
        playback(readAlbums(albumId).single())
    }

    private fun binding(localId: Int, date: LocalDate): String = "countdown:$localId:$date"

    private fun playback(album: SharedAlbum): AlbumPlayback {
        val userSession = session()
        val clips = rpc("list_countdown_clips", JSONObject().put("album", album.id)) as JSONArray
        return AlbumPlayback(album, (0 until clips.length()).map { index ->
            val row = clips.getJSONObject(index)
            val path = row.getString("path")
            SharedClip(path, Instant.parse(row.getString("uploaded_at")), path.split('/')[1] == userSession.user)
        }, "$base/storage/v1/object/authenticated/countdown-clips/", headers(userSession))
    }

    suspend fun upload(album: String, source: Uri, clipId: String) = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("countdown-", ".mp4", context.cacheDir)
        try {
            copyClip(source, temp)
            val auth = session()
            val objectPath = "$album/${auth.user}/$clipId.mp4"
            try {
                request("POST", "/storage/v1/object/countdown-clips/$objectPath",
                    auth = auth, clip = temp)
            } catch (error: IOException) {
                // An upload can succeed even if the client loses the response.
                val objects = rpc("list_countdown_clips", JSONObject().put("album", album)) as JSONArray
                if ((0 until objects.length()).none { objects.getJSONObject(it).getString("path") == objectPath }) {
                    throw error
                }
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        request("DELETE", "/storage/v1/object/countdown-clips", auth = session(),
            body = JSONObject().put("prefixes", JSONArray().put(path)))
        Unit
    }

    private fun copyClip(source: Uri, temp: File) {
        // ISO-BMFF containers can share signatures. Require both the MP4 MIME type and usable video metadata.
        if (context.contentResolver.getType(source) != "video/mp4") throw InvalidClip()
        val input = context.contentResolver.openInputStream(source) ?: throw InvalidClip()
        input.use {
            temp.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytes = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    bytes += count
                    if (bytes > 6L * 1024 * 1024) throw InvalidClip()
                    output.write(buffer, 0, count)
                }
            }
        }
        val metadata = MediaMetadataRetriever()
        try {
            metadata.setDataSource(temp.absolutePath)
            val duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (duration == null || duration !in 1..15_000 ||
                metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != "yes") throw InvalidClip()
        } catch (_: RuntimeException) {
            throw InvalidClip()
        } finally {
            metadata.release()
        }
    }

    private fun readAlbums(id: String? = null): List<SharedAlbum> {
        val auth = session()
        val filter = if (id == null) "&order=target_date.desc" else "&id=eq.$id"
        val rows = request("GET", "/rest/v1/countdown_albums?select=id,title,target_date,owner_id$filter", auth)
            as JSONArray
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            SharedAlbum(row.getString("id"), row.getString("title"), LocalDate.parse(row.getString("target_date")),
                row.getString("owner_id") == auth.user)
        }
    }

    private fun rpc(name: String, args: JSONObject): Any? = request("POST", "/rest/v1/rpc/$name", session(), args)

    @Synchronized
    private fun session(): Session {
        val now = Instant.now().epochSecond
        session?.takeIf { it.expiresAt > now + 120 }?.let { return it }
        val refresh = preferences.getString("refresh_token", null)
        val data = if (refresh != null) {
            request("POST", "/auth/v1/token?grant_type=refresh_token",
                body = JSONObject().put("refresh_token", refresh))
        } else {
            request("POST", "/auth/v1/signup", body = JSONObject())
        } as JSONObject
        check(preferences.edit().putString("refresh_token", data.getString("refresh_token")).commit())
        return Session(data.getString("access_token"), data.getJSONObject("user").getString("id"),
            now + data.getLong("expires_in")).also { session = it }
    }

    private fun headers(auth: Session): Map<String, String> =
        mapOf("apikey" to key, "Authorization" to "Bearer ${auth.token}")

    private fun request(
        method: String,
        route: String,
        auth: Session? = null,
        body: JSONObject? = null,
        clip: File? = null,
    ): Any? {
        val connection = URL(base + route).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("apikey", key)
            if (auth != null) connection.setRequestProperty("Authorization", "Bearer ${auth.token}")
            if (body != null || clip != null) {
                connection.doOutput = true
                if (clip != null) {
                    connection.setRequestProperty("Content-Type", "video/mp4")
                    connection.setFixedLengthStreamingMode(clip.length())
                    clip.inputStream().use { input -> connection.outputStream.use { input.copyTo(it) } }
                } else {
                    val payload = requireNotNull(body).toString().toByteArray()
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(payload) }
                }
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                if (status in 400..499) {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        ?.let { runCatching { JSONObject(it) }.getOrNull() }
                    if (route == "/auth/v1/signup" && error?.optString("error_code") == "anonymous_provider_disabled") {
                        throw AnonymousSignInDisabled()
                    }
                    if (route == "/rest/v1/rpc/join_countdown_album" && error?.optString("code") == "P0001") {
                        throw InvalidInvitation()
                    }
                }
                throw RequestFailed(status)
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return response.takeIf { it.isNotBlank() }?.let { JSONTokener(it).nextValue() }
        } finally {
            connection.disconnect()
        }
    }
}
