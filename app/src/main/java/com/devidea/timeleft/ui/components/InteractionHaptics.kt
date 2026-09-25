package com.devidea.timeleft.ui.components

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.devidea.timeleft.ui.theme.TimeRulerTokens

/** Platform feedback respects device/user settings; never bypass them or request vibration permission. */
internal class InteractionHaptics(private val view: View) {
    private var lastTick = 0L
    fun tick() {
        val now = SystemClock.uptimeMillis()
        if (now - lastTick >= TimeRulerTokens.HapticIntervalMs) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            lastTick = now
        }
    }
}

@Composable
internal fun rememberInteractionHaptics(): InteractionHaptics {
    val view = LocalView.current
    return remember(view) { InteractionHaptics(view) }
}
