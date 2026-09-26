package com.devidea.timeleft.focus

import com.devidea.timeleft.database.itemdata.ItemEntity
import com.devidea.timeleft.database.itemdata.ItemType
import com.devidea.timeleft.database.itemdata.RecurrenceMode
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

val ItemEntity.isFocusSession: Boolean get() = focusDurationMillis != null

/** Device-local reading. Never export the boot ID or elapsed-realtime anchor. */
data class FocusClockReading(val elapsedRealtime: Long, val bootCount: Int)

/** Elapsed time means timer-running time, not a claim that the user actually worked. */
object FocusSession {
    const val RUNNING = "running"
    const val PAUSED = "paused"
    const val COMPLETED = "completed"
    const val ABORTED = "aborted"

    fun create(title: String, minutes: Int, now: Long, zone: ZoneId, clock: FocusClockReading? = null): ItemEntity {
        require(minutes in 1..1440)
        val duration = minutes * 60_000L
        val from = Instant.ofEpochMilli(now).atZone(zone)
        val to = from.plusMinutes(minutes.toLong())
        return ItemEntity(type = ItemType.Time, title = title,
            startValue = "${from.hour}:${from.minute}", endValue = "${to.hour}:${to.minute}",
            updateFlag = RecurrenceMode.None, updateRate = 0, reminderOffsetDays = 0,
            stableId = UUID.randomUUID().toString(), modifiedAt = now, manualOrder = now,
            focusState = RUNNING, focusDurationMillis = duration, focusStartedAt = now,
            focusResumedAt = now, focusEndsAt = now + duration, focusRemainingMillis = duration,
            focusResumedRealtime = clock?.elapsedRealtime, focusBootCount = clock?.bootCount)
    }

    /** Rebase display dates after a wall-clock change without changing the timer's length.
     * After reboot/legacy migration, use the saved deadline once (powered-off time counts).
     * A virtual negative realtime anchor can represent a segment begun before this boot.
     */
    fun withClock(item: ItemEntity, now: Long, clock: FocusClockReading): ItemEntity {
        if (item.focusState != RUNNING) return item
        val resumed = item.focusResumedAt ?: return item
        val segment = item.focusRemainingMillis ?: return item
        val sameBoot = item.focusBootCount == clock.bootCount && item.focusResumedRealtime != null &&
            clock.elapsedRealtime >= item.focusResumedRealtime
        val since = if (sameBoot) (clock.elapsedRealtime - item.focusResumedRealtime!!).coerceAtLeast(0)
            else (now - resumed).coerceAtLeast(0)
        val rebased = now - since
        if (sameBoot && kotlin.math.abs(rebased - resumed) < 1_000L) return item
        return item.copy(focusResumedAt = rebased, focusEndsAt = rebased + segment,
            focusResumedRealtime = if (sameBoot) item.focusResumedRealtime else clock.elapsedRealtime - since,
            focusBootCount = clock.bootCount,
            pinnedUntilMillis = if (item.isPinned) rebased + segment else item.pinnedUntilMillis)
    }

    fun elapsed(item: ItemEntity, now: Long): Long {
        val planned = item.focusDurationMillis ?: return 0
        val running = if (item.focusState == RUNNING) {
            (minOf(now, item.focusEndsAt ?: now) - (item.focusResumedAt ?: now)).coerceAtLeast(0)
        } else 0
        return (item.focusElapsedMillis + running).coerceIn(0, planned)
    }

    fun remaining(item: ItemEntity, now: Long): Long = when (item.focusState) {
        RUNNING -> ((item.focusEndsAt ?: now) - now).coerceIn(0, item.focusDurationMillis ?: 0)
        PAUSED -> (item.focusRemainingMillis ?: 0).coerceAtLeast(0)
        else -> 0
    }

    fun settle(item: ItemEntity, now: Long): ItemEntity {
        if (item.focusState != RUNNING || item.focusEndsAt == null || now < item.focusEndsAt) return item
        return item.copy(isPinned = false, pinnedUntilMillis = null, focusState = COMPLETED, focusElapsedMillis = elapsed(item, item.focusEndsAt),
            focusRemainingMillis = 0, focusResumedAt = null, focusStoppedAt = item.focusEndsAt,
            focusResumedRealtime = null, focusBootCount = null,
            modifiedAt = maxOf(now, item.modifiedAt + 1))
    }

    fun pause(item: ItemEntity, now: Long): ItemEntity {
        val current = settle(item, now)
        if (current.focusState != RUNNING) return current
        return current.copy(pinnedUntilMillis = null, focusState = PAUSED, focusElapsedMillis = elapsed(current, now),
            focusRemainingMillis = remaining(current, now), focusResumedAt = null, focusEndsAt = null,
            focusResumedRealtime = null, focusBootCount = null,
            modifiedAt = maxOf(now, current.modifiedAt + 1))
    }

    fun resume(item: ItemEntity, now: Long, clock: FocusClockReading? = null): ItemEntity {
        require(item.focusState == PAUSED && (item.focusRemainingMillis ?: 0) > 0)
        return item.copy(pinnedUntilMillis = if (item.isPinned) now + item.focusRemainingMillis!! else null, focusState = RUNNING, focusResumedAt = now, focusEndsAt = now + item.focusRemainingMillis!!,
            focusResumedRealtime = clock?.elapsedRealtime, focusBootCount = clock?.bootCount,
            modifiedAt = maxOf(now, item.modifiedAt + 1))
    }

    fun stop(item: ItemEntity, now: Long): ItemEntity {
        val current = settle(item, now)
        if (current.focusState !in setOf(RUNNING, PAUSED)) return current
        return current.copy(isPinned = false, pinnedUntilMillis = null, focusState = ABORTED, focusElapsedMillis = elapsed(current, now),
            focusRemainingMillis = 0, focusResumedAt = null, focusStoppedAt = now,
            focusResumedRealtime = null, focusBootCount = null,
            modifiedAt = maxOf(now, current.modifiedAt + 1))
    }
}
