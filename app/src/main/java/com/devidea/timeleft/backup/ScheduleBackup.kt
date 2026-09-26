package com.devidea.timeleft.backup

import com.devidea.timeleft.focus.FocusSession
import com.devidea.timeleft.focus.isFocusSession
import com.devidea.timeleft.calendar.isTimedOccurrence
import androidx.room.withTransaction
import com.devidea.timeleft.database.AppDatabase
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import javax.inject.Inject
import javax.inject.Singleton

/** v2 adds absolute calendar occurrences. Older readers reject it instead of repeating them daily. */
object ScheduleBackupCodec {
    const val MAX_BYTES = 8 * 1024 * 1024
    private const val MAX_ITEMS = 10_000
    private val dateFormat = DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(ResolverStyle.STRICT)
    private val timeFormat = DateTimeFormatter.ofPattern("H:m").withResolverStyle(ResolverStyle.STRICT)

    fun encode(items: List<ItemEntity>, clock: com.devidea.timeleft.focus.FocusClockReading? = null): ByteArray {
        require(items.size <= MAX_ITEMS)
        val values = JSONArray()
        val capturedAt = System.currentTimeMillis()
        items.forEach { raw ->
            // A copied timer must not run on a second device without an explicit resume.
            val adjusted = clock?.let { FocusSession.withClock(raw, capturedAt, it) } ?: raw
            val item = if (raw.isFocusSession) FocusSession.pause(adjusted, capturedAt) else raw
            values.put(JSONObject().apply {
                put("stableId", item.stableId); put("modifiedAt", item.modifiedAt)
                put("type", item.type.name); put("title", item.title)
                put("startValue", item.startValue); put("endValue", item.endValue)
                put("updateFlag", item.updateFlag.name); put("updateRate", item.updateRate)
                put("category", item.category); put("colorKey", item.colorKey); put("iconKey", item.iconKey)
                put("reminderOffsetDays", item.reminderOffsetDays); put("weekdays", item.weekdays); put("endNextDay", item.endNextDay)
                put("focusState", item.focusState); put("focusDurationMillis", item.focusDurationMillis)
                put("focusStartedAt", item.focusStartedAt); put("focusResumedAt", item.focusResumedAt)
                put("focusEndsAt", item.focusEndsAt); put("focusRemainingMillis", item.focusRemainingMillis)
                put("focusElapsedMillis", item.focusElapsedMillis); put("focusStoppedAt", item.focusStoppedAt)
                put("isTemplate", item.isTemplate); put("isPinned", item.isPinned)
                put("pinnedUntilMillis", item.pinnedUntilMillis); put("manualOrder", item.manualOrder)
                put("occurrenceStartMillis", item.occurrenceStartMillis); put("occurrenceEndMillis", item.occurrenceEndMillis)
                put("calendarSourceKey", item.calendarSourceKey)
            })
        }
        return JSONObject().put("application", "LeftMyTime").put("formatVersion", 2)
            .put("exportedAt", System.currentTimeMillis()).put("items", values)
            .toString(2).toByteArray(Charsets.UTF_8).also { require(it.size <= MAX_BYTES) }
    }

    fun decode(input: InputStream): List<ItemEntity> {
        // Bound memory even when a provider does not report a file size.
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_BYTES)
            output.write(buffer, 0, count)
        }
        val document = JSONObject(output.toString(Charsets.UTF_8.name()))
        require(document.getString("application") == "LeftMyTime")
        require(document.getInt("formatVersion") in 1..2)
        val values = document.getJSONArray("items")
        require(values.length() <= MAX_ITEMS)
        val items = (0 until values.length()).map { index ->
            val json = values.getJSONObject(index)
            fun text(key: String, limit: Int = 16_384) = json.getString(key).also { require(it.length <= limit) }
            fun optionalLong(key: String): Long? = if (json.has(key) && !json.isNull(key)) json.getLong(key) else null
            ItemEntity(type = ItemType.valueOf(text("type", 32)), title = text("title"),
                startValue = text("startValue", 40), endValue = text("endValue", 40),
                updateFlag = RecurrenceMode.valueOf(text("updateFlag", 32)), updateRate = json.getInt("updateRate"),
                category = text("category"), colorKey = text("colorKey", 128), iconKey = text("iconKey", 128),
                reminderOffsetDays = json.getInt("reminderOffsetDays"), stableId = text("stableId", 128),
                modifiedAt = json.getLong("modifiedAt"), weekdays = json.optInt("weekdays", 127), endNextDay = json.optBoolean("endNextDay", false),
                focusState = json.optString("focusState", ""), focusDurationMillis = optionalLong("focusDurationMillis"),
                focusStartedAt = optionalLong("focusStartedAt"), focusResumedAt = optionalLong("focusResumedAt"),
                focusEndsAt = optionalLong("focusEndsAt"), focusRemainingMillis = optionalLong("focusRemainingMillis"),
                focusElapsedMillis = json.optLong("focusElapsedMillis", 0), focusStoppedAt = optionalLong("focusStoppedAt"),
                isTemplate = json.optBoolean("isTemplate", false), isPinned = json.optBoolean("isPinned", false),
                pinnedUntilMillis = optionalLong("pinnedUntilMillis"), manualOrder = json.optLong("manualOrder", 0),
                occurrenceStartMillis = optionalLong("occurrenceStartMillis"), occurrenceEndMillis = optionalLong("occurrenceEndMillis"),
                calendarSourceKey = json.optString("calendarSourceKey", "")).also(::validate)
        }
        require(items.map { it.stableId }.distinct().size == items.size)
        return items
    }

    private fun validate(item: ItemEntity) {
        require(item.stableId.matches(Regex("[a-zA-Z0-9-]{1,128}")))
        require(item.title.isNotBlank() && item.modifiedAt >= 0)
        require(item.reminderOffsetDays in -1..365)
        require(item.calendarSourceKey.isEmpty() || item.calendarSourceKey.matches(Regex("[a-f0-9]{64}")))
        if (item.isTimedOccurrence) {
            require(!item.isFocusSession && !item.isTemplate && item.type == ItemType.Time && item.updateFlag == RecurrenceMode.None)
            val start = java.time.Instant.ofEpochMilli(requireNotNull(item.occurrenceStartMillis))
            val end = java.time.Instant.ofEpochMilli(requireNotNull(item.occurrenceEndMillis))
            require(end.isAfter(start))
            require(start.atZone(java.time.ZoneOffset.UTC).year in 1..9999 && end.atZone(java.time.ZoneOffset.UTC).year in 1..9999)
            LocalTime.parse(item.startValue, timeFormat); LocalTime.parse(item.endValue, timeFormat)
            return
        }
        if (item.isFocusSession) {
            val duration = requireNotNull(item.focusDurationMillis)
            require(!item.isTemplate)
            require(item.type == ItemType.Time && duration in 60_000L..86_400_000L)
            require(item.focusState in setOf(FocusSession.PAUSED, FocusSession.COMPLETED, FocusSession.ABORTED))
            require((item.focusStartedAt ?: -1) >= 0 && item.focusElapsedMillis in 0..duration)
            if (item.focusState == FocusSession.PAUSED) {
                require(item.focusRemainingMillis != null && item.focusRemainingMillis in 1..duration)
                require(item.focusElapsedMillis + item.focusRemainingMillis == duration)
            } else {
                // A manual wall-clock correction can make the finish date precede the start.
                // Running duration is stored independently and remains bounded above.
                require((item.focusStoppedAt ?: -1) >= 0 && item.focusRemainingMillis == 0L)
                if (item.focusState == FocusSession.COMPLETED) require(item.focusElapsedMillis == duration)
            }
            require(item.focusResumedAt == null)
            return
        }
        require(item.focusState.isEmpty())
        when (item.type) {
            ItemType.Time -> {
                val start = LocalTime.parse(item.startValue, timeFormat)
                val end = LocalTime.parse(item.endValue, timeFormat)
                require(item.weekdays in 1..127)
                require(if (item.endNextDay) !end.isAfter(start) else end.isAfter(start))
                require(item.updateFlag == RecurrenceMode.TimeRange || item.updateFlag == RecurrenceMode.None)
            }
            ItemType.Date -> {
                val start = LocalDate.parse(item.startValue, dateFormat)
                val end = LocalDate.parse(item.endValue, dateFormat)
                require(!end.isBefore(start))
                require(start.year in 1..9999 && end.year in 1..9999)
                when (item.updateFlag) {
                    RecurrenceMode.Day -> require(item.updateRate in 1..365_000)
                    RecurrenceMode.Month -> require(item.updateRate in 1..31)
                    RecurrenceMode.None -> Unit
                    RecurrenceMode.TimeRange -> error("Invalid date recurrence")
                }
            }
        }
    }
}

enum class RestoreMode { KeepExisting, UseBackup }
data class RestorePreview(val imported: List<ItemEntity>, val existing: List<ItemEntity>, val reviewedAt: Long = System.currentTimeMillis()) {
    private val byIdentity = existing.associateBy { it.stableId }
    val additions = imported.count { it.stableId !in byIdentity }
    val unchanged = imported.count { candidate -> byIdentity[candidate.stableId]?.let { sameSchedule(it, candidate) } == true }
    val conflicts = imported.size - additions - unchanged

    private fun mergedItems(mode: RestoreMode): List<ItemEntity> = byIdentity.toMutableMap().apply {
        imported.forEach { candidate ->
            val old = get(candidate.stableId)
            if (old == null || mode == RestoreMode.UseBackup) put(candidate.stableId, candidate.copy(id = old?.id ?: 0))
        }
    }.values.toList()

    fun pinCandidates(mode: RestoreMode, at: Long = reviewedAt): List<ItemEntity> = mergedItems(mode).filter {
        it.deletedAt == null && !it.isTemplate && it.isPinned &&
            (it.pinnedUntilMillis?.let { deadline -> deadline > at }
                ?: (it.isFocusSession && it.focusState == FocusSession.PAUSED)) &&
            (!it.isFocusSession || it.focusState in setOf(FocusSession.RUNNING, FocusSession.PAUSED))
    }

    fun preferredPin(mode: RestoreMode): String? {
        val candidates = pinCandidates(mode)
        return candidates.firstOrNull { byIdentity[it.stableId]?.isPinned == true }?.stableId
            ?: candidates.firstOrNull()?.stableId
    }

    /** Resolve the one shared home slot separately from per-schedule content conflicts. */
    fun resolvedItems(mode: RestoreMode, selectedPin: String?): List<ItemEntity> = mergedItems(mode).map {
        if (it.deletedAt == null && it.isPinned && it.stableId != selectedPin)
            it.copy(isPinned = false, pinnedUntilMillis = null)
        else it
    }
}

internal fun sameSchedule(a: ItemEntity, b: ItemEntity): Boolean =
    a.copy(id = 0, modifiedAt = 0, focusResumedRealtime = null, focusBootCount = null) ==
        b.copy(id = 0, modifiedAt = 0, focusResumedRealtime = null, focusBootCount = null)

class RestorePreviewChanged : IllegalStateException()

@Singleton
class ScheduleBackupRepository @Inject constructor(private val database: AppDatabase,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context) {
    suspend fun export(): ByteArray = ScheduleBackupCodec.encode(database.itemDao().getAllRecords().filter { it.deletedAt == null },
        com.devidea.timeleft.focus.readFocusClock(context))
    suspend fun preview(imported: List<ItemEntity>) = RestorePreview(imported, database.itemDao().getAllRecords())

    suspend fun restore(preview: RestorePreview, mode: RestoreMode, selectedPin: String? = preview.preferredPin(mode)): Int = database.withTransaction {
        val dao = database.itemDao()
        val current = dao.getAllRecords()
        // Do not silently overwrite an edit made after the review screen was prepared.
        if (current != preview.existing) throw RestorePreviewChanged()
        if (selectedPin != null && preview.pinCandidates(mode, System.currentTimeMillis()).none { it.stableId == selectedPin })
            throw RestorePreviewChanged()
        val byIdentity = current.associateBy { it.stableId }
        var changed = 0
        preview.resolvedItems(mode, selectedPin).forEach { candidate ->
            val old = byIdentity[candidate.stableId]
            when {
                old == null -> { dao.saveItem(candidate.copy(id = 0, modifiedAt = System.currentTimeMillis())); changed++ }
                sameSchedule(old, candidate) -> Unit
                else -> {
                    // A restored snapshot must not inherit a pending notification from this device.
                    database.focusCompletionDao().removeForItem(old.id)
                    dao.updateItem(candidate.copy(id = old.id, modifiedAt = maxOf(System.currentTimeMillis(), old.modifiedAt + 1)))
                    changed++
                }
            }
        }
        changed
    }
}
