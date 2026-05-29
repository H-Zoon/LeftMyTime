package com.devidea.timeleft.ui.editor

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.theme.Spacing
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun TimeRangeDial(
    startTime: LocalTime,
    endTime: LocalTime,
    onRangeChange: (LocalTime, LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = DateTimeFormatter.ofPattern(stringResource(R.string.pattern_display_time))
    val startText = startTime.format(formatter)
    val endText = endTime.format(formatter)
    val durationText = rangeDurationText(startTime, endTime)
    val accessibilityText = stringResource(
        R.string.editor_time_range_accessibility,
        startText,
        endText,
        durationText
    )
    val latestStart = rememberUpdatedState(startTime)
    val latestEnd = rememberUpdatedState(endTime)
    val latestOnRangeChange = rememberUpdatedState(onRangeChange)
    var draggingHandle by remember { mutableStateOf<RangeHandle?>(null) }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val tick = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface

    val metrics = with(LocalDensity.current) {
        remember(this) {
            DialMetrics(
                ringWidthPx = 16.dp.toPx(),
                outerInsetPx = 24.dp.toPx(),
                tickMajorInsetPx = 3.dp.toPx(),
                tickMinorInsetPx = 7.dp.toPx(),
                tickMajorWidthPx = 2.dp.toPx(),
                tickMinorWidthPx = 1.dp.toPx(),
                labelInsetPx = 29.dp.toPx(),
                labelTextSizePx = 11.sp.toPx(),
                handleHaloPx = 16.dp.toPx(),
                handleOuterPx = 10.dp.toPx(),
                handleInnerPx = 8.dp.toPx(),
                handleTouchPx = 28.dp.toPx(),
            )
        }
    }
    val labelPaint = remember(tick, metrics.labelTextSizePx) {
        Paint().apply {
            color = tick.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = metrics.labelTextSizePx
            isAntiAlias = true
        }
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = stringResource(R.string.editor_time_range),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$startText - $endText",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = durationText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Canvas(
                modifier = Modifier
                    .size(252.dp)
                    .semantics { contentDescription = accessibilityText }
                    .pointerInput(metrics) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val handle = handleAt(
                                position = down.position,
                                size = size,
                                startTime = latestStart.value,
                                endTime = latestEnd.value,
                                outerInsetPx = metrics.outerInsetPx,
                                touchRadiusPx = metrics.handleTouchPx
                            ) ?: return@awaitEachGesture
                            down.consume()
                            draggingHandle = handle
                            updateRange(
                                handle = handle,
                                position = down.position,
                                size = size,
                                startTime = latestStart.value,
                                endTime = latestEnd.value,
                                onRangeChange = latestOnRangeChange.value
                            )
                            drag(down.id) { change ->
                                change.consume()
                                updateRange(
                                    handle = handle,
                                    position = change.position,
                                    size = size,
                                    startTime = latestStart.value,
                                    endTime = latestEnd.value,
                                    onRangeChange = latestOnRangeChange.value
                                )
                            }
                            draggingHandle = null
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = min(size.width, size.height) / 2f - metrics.outerInsetPx
                val startMinutes = minuteOfDay(startTime)
                val endMinutes = minuteOfDay(endTime)

                repeat(HOURS_PER_DAY) { hour ->
                    val angleMinutes = hour * MINUTES_PER_HOUR
                    val isMajor = hour % 3 == 0
                    val outer = pointOnDial(angleMinutes, center, radius + metrics.ringWidthPx)
                    val inner = pointOnDial(
                        angleMinutes,
                        center,
                        radius + if (isMajor) metrics.tickMajorInsetPx else metrics.tickMinorInsetPx
                    )
                    drawLine(
                        color = tick.copy(alpha = if (isMajor) 0.9f else 0.42f),
                        start = inner,
                        end = outer,
                        strokeWidth = if (isMajor) metrics.tickMajorWidthPx else metrics.tickMinorWidthPx,
                        cap = StrokeCap.Round
                    )
                }
                DIAL_LABELS.forEach { (hour, label) ->
                    val point = pointOnDial(
                        hour * MINUTES_PER_HOUR,
                        center,
                        radius - metrics.labelInsetPx
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        point.x,
                        point.y + metrics.labelTextSizePx / 3f,
                        labelPaint
                    )
                }
                drawCircle(
                    color = track,
                    radius = radius,
                    center = center,
                    style = Stroke(width = metrics.ringWidthPx)
                )
                drawArc(
                    color = primary,
                    startAngle = dialAngle(startMinutes),
                    sweepAngle = ((endMinutes - startMinutes).coerceAtLeast(0) /
                        MINUTES_PER_DAY.toFloat()) * DEGREES_IN_CIRCLE,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = metrics.ringWidthPx, cap = StrokeCap.Round)
                )
                drawHandle(pointOnDial(startMinutes, center, radius), secondary, surface, metrics, draggingHandle == RangeHandle.Start)
                drawHandle(pointOnDial(endMinutes, center, radius), primary, surface, metrics, draggingHandle == RangeHandle.End)
            }
            Text(
                text = stringResource(R.string.editor_time_range_drag_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun rangeDurationText(startTime: LocalTime, endTime: LocalTime): String {
    val minutes = Duration.between(startTime, endTime).toMinutes().coerceAtLeast(0)
    val hours = minutes / MINUTES_PER_HOUR
    val remainderMinutes = minutes % MINUTES_PER_HOUR
    return when {
        hours > 0 && remainderMinutes > 0 ->
            stringResource(R.string.editor_time_range_hours_minutes, hours, remainderMinutes)
        hours > 0 -> stringResource(R.string.editor_time_range_hours, hours)
        else -> stringResource(R.string.editor_time_range_minutes, remainderMinutes)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(
    center: Offset,
    color: Color,
    surface: Color,
    metrics: DialMetrics,
    active: Boolean,
) {
    if (active) {
        drawCircle(color = color.copy(alpha = 0.16f), radius = metrics.handleHaloPx, center = center)
    }
    drawCircle(color = surface, radius = metrics.handleOuterPx, center = center)
    drawCircle(color = color, radius = metrics.handleInnerPx, center = center)
}

private fun handleAt(
    position: Offset,
    size: IntSize,
    startTime: LocalTime,
    endTime: LocalTime,
    outerInsetPx: Float,
    touchRadiusPx: Float,
): RangeHandle? {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = min(size.width, size.height) / 2f - outerInsetPx
    val startPos = pointOnDial(minuteOfDay(startTime), center, radius)
    val endPos = pointOnDial(minuteOfDay(endTime), center, radius)
    val startDist = (position - startPos).getDistance()
    val endDist = (position - endPos).getDistance()
    val nearestDist = min(startDist, endDist)
    if (nearestDist > touchRadiusPx) return null
    return if (startDist <= endDist) RangeHandle.Start else RangeHandle.End
}

private fun updateRange(
    handle: RangeHandle,
    position: Offset,
    size: IntSize,
    startTime: LocalTime,
    endTime: LocalTime,
    onRangeChange: (LocalTime, LocalTime) -> Unit,
) {
    val candidate = minutesFromPosition(position, size)
    val startMinutes = minuteOfDay(startTime)
    val endMinutes = minuteOfDay(endTime)
    when (handle) {
        RangeHandle.Start -> {
            val updated = candidate.coerceIn(0, (endMinutes - MINIMUM_RANGE_MINUTES).coerceAtLeast(0))
            if (updated != startMinutes) onRangeChange(timeOfDay(updated), endTime)
        }
        RangeHandle.End -> {
            val minimum = (startMinutes + MINIMUM_RANGE_MINUTES).coerceAtMost(LAST_MINUTE)
            val updated = candidate.coerceIn(minimum, LAST_MINUTE)
            if (updated != endMinutes) onRangeChange(startTime, timeOfDay(updated))
        }
    }
}

private fun minutesFromPosition(position: Offset, size: IntSize): Int {
    val center = Offset(size.width / 2f, size.height / 2f)
    val angle = atan2(position.y - center.y, position.x - center.x) + PI / 2
    val normalized = if (angle < 0) angle + PI * 2 else angle
    val rawMinutes = normalized / (PI * 2) * MINUTES_PER_DAY
    return ((rawMinutes / MINUTE_STEP).roundToInt() * MINUTE_STEP)
        .coerceIn(0, LAST_MINUTE)
}

private fun pointOnDial(minutes: Int, center: Offset, radius: Float): Offset {
    val radians = (minutes.toFloat() / MINUTES_PER_DAY * PI * 2 - PI / 2).toFloat()
    return Offset(
        x = center.x + cos(radians) * radius,
        y = center.y + sin(radians) * radius
    )
}

private fun dialAngle(minutes: Int): Float =
    minutes.toFloat() / MINUTES_PER_DAY * DEGREES_IN_CIRCLE - 90f

private fun minuteOfDay(time: LocalTime): Int = time.hour * MINUTES_PER_HOUR + time.minute

private fun timeOfDay(minutes: Int): LocalTime =
    LocalTime.of(minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)

private enum class RangeHandle {
    Start,
    End,
}

private data class DialMetrics(
    val ringWidthPx: Float,
    val outerInsetPx: Float,
    val tickMajorInsetPx: Float,
    val tickMinorInsetPx: Float,
    val tickMajorWidthPx: Float,
    val tickMinorWidthPx: Float,
    val labelInsetPx: Float,
    val labelTextSizePx: Float,
    val handleHaloPx: Float,
    val handleOuterPx: Float,
    val handleInnerPx: Float,
    val handleTouchPx: Float,
)

private val DIAL_LABELS = listOf(0 to "00", 6 to "06", 12 to "12", 18 to "18")

private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR
private const val MINUTE_STEP = 5
private const val MINIMUM_RANGE_MINUTES = 1
private const val LAST_MINUTE = MINUTES_PER_DAY - 1
private const val DEGREES_IN_CIRCLE = 360f
