package com.devidea.timeleft

import com.devidea.timeleft.calc.TimeRangePhase
import java.time.Duration

/** Display-only facts from the original range; never reconstructed from rounded percentages. */
data class TimeDetailFacts(
    val percentElapsed: Float,
    val elapsed: Long,
    val total: Long,
    val phase: TimeRangePhase,
    val inDays: Boolean = false,
    val secondsLeft: Long? = null,
    val secondsUntilStart: Long? = null,
    val validRange: Boolean = true,
    val includesToday: Boolean = false,
    val range: TimeDetailRange? = null,
) {
    val glowActive: Boolean
        get() = validRange && phase == TimeRangePhase.Active && percentElapsed.isFinite() && percentElapsed < 100f
}

/** Only the seconds display rounds up. Stored values, ordering and existing minute labels stay intact. */
internal fun Duration.secondsForDisplay(): Long =
    if (isNegative || isZero) 0L else seconds + if (nano > 0) 1L else 0L
