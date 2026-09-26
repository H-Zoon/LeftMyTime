package com.devidea.timeleft.backup

import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestorePreviewTest {
    private val item = ItemEntity(8, ItemType.Date, "A", "2026-09-25", "2026-09-26", RecurrenceMode.None, 0,
        stableId = "identity", modifiedAt = 10)

    @Test fun sameItemOnDifferentDeviceIsNotDuplicated() {
        val preview = RestorePreview(listOf(item.copy(id = 0, modifiedAt = 50)), listOf(item))
        assertEquals(0, preview.additions)
        assertEquals(0, preview.conflicts)
        assertEquals(1, preview.unchanged)
    }

    @Test fun deletedAndEditedVersionsAreExplicitConflicts() {
        assertEquals(1, RestorePreview(listOf(item), listOf(item.copy(deletedAt = 60))).conflicts)
        assertEquals(1, RestorePreview(listOf(item), listOf(item.copy(title = "B"))).conflicts)
    }

    @Test fun equalTitleWithDifferentIdentityRemainsAnIndependentSchedule() {
        assertEquals(1, RestorePreview(listOf(item.copy(stableId = "other")), listOf(item)).additions)
    }

    @Test fun twoBackupsCannotSilentlyLeaveMultipleHomePins() {
        val local = item.copy(isPinned = true, pinnedUntilMillis = 1_000)
        val imported = item.copy(id = 0, stableId = "imported", title = "B", isPinned = true, pinnedUntilMillis = 1_000)
        val preview = RestorePreview(listOf(imported), listOf(local), reviewedAt = 100)
        assertEquals("identity", preview.preferredPin(RestoreMode.KeepExisting))
        assertEquals(2, preview.pinCandidates(RestoreMode.KeepExisting).size)
        val keepLocal = preview.resolvedItems(RestoreMode.KeepExisting, preview.preferredPin(RestoreMode.KeepExisting))
        assertEquals(listOf("identity"), keepLocal.filter { it.isPinned }.map { it.stableId })
        val useImported = preview.resolvedItems(RestoreMode.KeepExisting, "imported")
        assertEquals(listOf("imported"), useImported.filter { it.isPinned }.map { it.stableId })
        assertEquals(listOf("A", "B"), useImported.map { it.title })
        val automatic = preview.resolvedItems(RestoreMode.KeepExisting, null)
        assertEquals(2, automatic.size)
        assertTrue(automatic.none { it.isPinned || it.pinnedUntilMillis != null })
    }

    @Test fun contentChoiceStillProtectsEditsAndDeletedItems() {
        val edited = item.copy(title = "Local edit", isPinned = true, pinnedUntilMillis = 1_000)
        val deleted = item.copy(id = 9, stableId = "deleted", deletedAt = 90)
        val preview = RestorePreview(listOf(item.copy(title = "File"), deleted.copy(deletedAt = null)), listOf(edited, deleted), 100)
        val keeping = preview.resolvedItems(RestoreMode.KeepExisting, "identity")
        assertEquals("Local edit", keeping.first().title)
        assertEquals(90L, keeping.last().deletedAt)
        val replacing = preview.resolvedItems(RestoreMode.UseBackup, null)
        assertEquals("File", replacing.first().title)
        assertTrue(replacing.last().deletedAt == null)
    }

    @Test fun expiredPinsAreNotOfferedForRestore() {
        val preview = RestorePreview(listOf(item.copy(isPinned = true, pinnedUntilMillis = 100)), emptyList(), 100)
        assertTrue(preview.pinCandidates(RestoreMode.KeepExisting).isEmpty())
        assertFalse(preview.resolvedItems(RestoreMode.KeepExisting, null).single().isPinned)
    }
}
