package com.devidea.timeleft.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.devidea.timeleft.R
import com.devidea.timeleft.formatPercent
import com.devidea.timeleft.roundPercent
import com.devidea.timeleft.ui.theme.Spacing
import com.devidea.timeleft.ui.theme.TimeRulerTokens

@Composable
internal fun TimeRuler(
    percentElapsed: Float,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    currentLabel: String = "",
    glowEnabled: Boolean = false,
) {
    val safePercent = percentElapsed.takeIf { it.isFinite() }?.coerceIn(0f, 100f) ?: 0f
    val elapsed = safePercent / 100f
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    val glowAlpha = if (MaterialTheme.colorScheme.background.luminance() < .5f) TimeRulerTokens.GlowDarkAlpha else TimeRulerTokens.GlowLightAlpha
    val displayedElapsed = roundPercent(safePercent).let { if (glowEnabled) it.coerceAtMost(99.9f) else it }
    val remaining = formatPercent(100f - displayedElapsed, LocalContext.current.resources.configuration.locales[0], alwaysOneDecimal = true)
    val description = if (startLabel.isBlank() && endLabel.isBlank()) stringResource(R.string.time_ruler_remaining_description, remaining)
        else stringResource(R.string.time_ruler_description, startLabel, endLabel, remaining)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Canvas(Modifier.fillMaxWidth().height(TimeRulerTokens.Height.dp).clearAndSetSemantics { contentDescription = description }) {
            if (glowEnabled && percentElapsed.isFinite()) drawIntoCanvas {
                drawTimeRulerGlow(it.nativeCanvas, size.width, size.height, elapsed, accent.toArgb(), glowAlpha)
            }
            val count = TimeRulerTokens.tickCount(size.width / density)
            val step = size.width / count
            repeat(count) { index ->
                val x = step * (index + .5f)
                val height = if (index % 5 == 0) TimeRulerTokens.MajorHeight.dp.toPx() else TimeRulerTokens.MinorHeight.dp.toPx()
                drawLine(
                    color = if ((index + .5f) / count < elapsed) track else accent,
                    start = Offset(x, size.height), end = Offset(x, size.height - height),
                    strokeWidth = TimeRulerTokens.StrokeWidth.dp.toPx(), cap = StrokeCap.Butt
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(startLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(endLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (currentLabel.isNotBlank()) {
            Text(stringResource(R.string.time_now, currentLabel), style = MaterialTheme.typography.bodySmall, color = accent)
        }
    }
}
