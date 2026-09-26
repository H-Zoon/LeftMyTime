package com.devidea.timeleft.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Instances
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.devidea.timeleft.R
import com.devidea.timeleft.database.AppDatabase
import com.devidea.timeleft.repository.TimeLeftRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceCalendar(val id: Long, val name: String)
data class CalendarImportPreview(val calendarId: Long, val firstDay: LocalDate, val days: Int,
    val entries: List<CalendarOccurrence>, val alreadyImported: Set<String>, val truncated: Boolean)
class CalendarPreviewChanged : IllegalStateException()

@Singleton
class CalendarImportRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prefs: SharedPreferences,
    private val database: AppDatabase,
    private val schedules: TimeLeftRepository,
) {
    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    private fun requirePermission() { if (!hasPermission()) throw SecurityException() }
    private val namespace: String by lazy {
        prefs.getString("calendar_import_namespace", null) ?: UUID.randomUUID().toString().also {
            check(prefs.edit().putString("calendar_import_namespace", it).commit())
        }
    }

    fun calendars(): List<DeviceCalendar> {
        requirePermission()
        return context.contentResolver.query(Calendars.CONTENT_URI,
            arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME), "${Calendars.VISIBLE} = 1", null, "${Calendars.CALENDAR_DISPLAY_NAME} ASC")
            ?.use { cursor -> buildList {
                while (cursor.moveToNext()) add(DeviceCalendar(cursor.getLong(0), cursor.getString(1).orEmpty().ifBlank { context.getString(R.string.calendar_unnamed) }))
            } } ?: error("Calendar provider unavailable")
    }

    suspend fun preview(calendarId: Long, days: Int, firstDay: LocalDate = LocalDate.now()): CalendarImportPreview {
        requirePermission()
        require(days in setOf(7, 30, 90))
        val zone = ZoneId.systemDefault()
        val until = firstDay.plusDays(days.toLong())
        val fromMillis = minOf(firstDay.atStartOfDay(zone).toInstant().toEpochMilli(), firstDay.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        val untilMillis = maxOf(until.atStartOfDay(zone).toInstant().toEpochMilli(), until.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        val uri = Instances.CONTENT_URI.buildUpon().also { ContentUris.appendId(it, fromMillis); ContentUris.appendId(it, untilMillis) }.build()
        val projection = arrayOf(Instances.EVENT_ID, Instances.TITLE, Instances.BEGIN, Instances.END, Instances.ALL_DAY,
            Instances.RRULE, Instances.RDATE, Instances.ORIGINAL_ID, Instances.ORIGINAL_INSTANCE_TIME,
            Instances.DTSTART, Instances.DTEND, Instances.DURATION, Instances.EXDATE, Instances.EVENT_TIMEZONE)
        val result = context.contentResolver.query(uri, projection,
            "${Instances.CALENDAR_ID} = ? AND (${Instances.STATUS} IS NULL OR ${Instances.STATUS} != ?)",
            arrayOf(calendarId.toString(), CalendarContract.Events.STATUS_CANCELED.toString()), "${Instances.BEGIN} ASC, ${Instances.EVENT_ID} ASC")
            ?.use { cursor -> buildList<CalendarOccurrence> {
                while (cursor.moveToNext() && size <= MAX_VISIBLE) {
                    val recurring = !cursor.getString(5).isNullOrBlank() || !cursor.getString(6).isNullOrBlank()
                    // Some providers temporarily keep an older expansion after Events has changed.
                    // Single events/exceptions have authoritative absolute DTSTART/DTEND values.
                    val begin = if (!recurring && !cursor.isNull(9)) cursor.getLong(9) else cursor.getLong(2)
                    val end = if (!recurring && !cursor.isNull(10)) cursor.getLong(10) else cursor.getLong(3)
                    if (end <= begin) continue
                    val allDay = cursor.getInt(4) == 1
                    val eventZone = if (allDay) ZoneOffset.UTC else zone
                    val lower = firstDay.atStartOfDay(eventZone).toInstant().toEpochMilli()
                    val upper = until.atStartOfDay(eventZone).toInstant().toEpochMilli()
                    if (end <= lower || begin >= upper) continue
                    val originalId = if (cursor.isNull(7)) null else cursor.getLong(7)
                    val originalTime = if (cursor.isNull(8)) null else cursor.getLong(8)
                    val key = calendarSourceKey(namespace, calendarId, originalId ?: cursor.getLong(0),
                        originalTime ?: begin.takeIf { recurring })
                    val revision = (5..13).joinToString("\u0000") { if (cursor.isNull(it)) "" else cursor.getString(it) }
                    add(CalendarOccurrence(key, cursor.getString(1).orEmpty().ifBlank { context.getString(R.string.calendar_untitled) }, begin, end, allDay, revision))
                }
            } } ?: error("Calendar provider unavailable")
        val existing = database.itemDao().getItems().map { it.calendarSourceKey }.filter { it.isNotEmpty() }.toSet()
        return CalendarImportPreview(calendarId, firstDay, days, result.take(MAX_VISIBLE).distinctBy { it.sourceKey }, existing, result.size > MAX_VISIBLE)
    }

    suspend fun import(preview: CalendarImportPreview, selection: Set<String>): Int {
        require(selection.isNotEmpty() && selection.size <= MAX_SELECTION)
        val chosen = preview.entries.filter { it.sourceKey in selection }
        require(chosen.size == selection.size)
        // Recheck provider data before committing; a moved/deleted event needs another review.
        val fresh = preview(preview.calendarId, preview.days, preview.firstDay).entries.associateBy { it.sourceKey }
        if (chosen.any { fresh[it.sourceKey] != it }) throw CalendarPreviewChanged()
        return database.withTransaction {
            requirePermission()
            val existing = database.itemDao().getItems().map { it.calendarSourceKey }.toMutableSet()
            var count = 0
            chosen.forEach { occurrence ->
                if (existing.add(occurrence.sourceKey)) {
                    schedules.save(occurrence.toItem(ZoneId.systemDefault()))
                    count++
                }
            }
            count
        }
    }

    companion object { const val MAX_VISIBLE = 500; const val MAX_SELECTION = 100 }
}
