package com.devidea.timeleft.backup

import com.devidea.timeleft.calendar.CalendarOccurrence
import com.devidea.timeleft.calendar.isTimedOccurrence
import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ScheduleBackupCodecTest {
    @Test fun v2RoundTripKeepsAbsoluteOccurrenceAndDuplicateIdentity() {
        val item = CalendarOccurrence("a".repeat(64), "Across midnight",
            Instant.parse("2026-09-26T14:00:00Z").toEpochMilli(), Instant.parse("2026-09-27T02:00:00Z").toEpochMilli(), false)
            .toItem(ZoneId.of("Asia/Seoul")).copy(stableId = "stable", modifiedAt = 123)
        val bytes = ScheduleBackupCodec.encode(listOf(item))
        assertEquals(2, JSONObject(String(bytes)).getInt("formatVersion"))
        val restored = ScheduleBackupCodec.decode(bytes.inputStream()).single()
        assertTrue(restored.isTimedOccurrence)
        assertTrue(sameSchedule(item, restored))
        assertEquals(RecurrenceMode.None, restored.updateFlag)
    }

    @Test fun v1WithoutNewFieldsRemainsReadable() {
        val legacy = ItemEntity(type = ItemType.Time, title = "Daily", startValue = "9:0", endValue = "18:0",
            updateFlag = RecurrenceMode.TimeRange, updateRate = 0, stableId = "legacy", modifiedAt = 10)
        val document = JSONObject(String(ScheduleBackupCodec.encode(listOf(legacy)))).put("formatVersion", 1)
        document.getJSONArray("items").getJSONObject(0).apply {
            remove("occurrenceStartMillis"); remove("occurrenceEndMillis"); remove("calendarSourceKey")
        }
        val restored = ScheduleBackupCodec.decode(document.toString().byteInputStream()).single()
        assertFalse(restored.isTimedOccurrence)
        assertTrue(sameSchedule(legacy, restored))
    }

    @Test fun incompleteAbsoluteBoundsRejectTheWholeFile() {
        val legacy = ItemEntity(type = ItemType.Time, title = "Daily", startValue = "9:0", endValue = "18:0",
            updateFlag = RecurrenceMode.None, updateRate = 0, stableId = "legacy", modifiedAt = 10)
        val document = JSONObject(String(ScheduleBackupCodec.encode(listOf(legacy))))
        document.getJSONArray("items").getJSONObject(0).put("occurrenceStartMillis", 123456)
        assertTrue(runCatching { ScheduleBackupCodec.decode(document.toString().byteInputStream()) }.isFailure)
    }
}
