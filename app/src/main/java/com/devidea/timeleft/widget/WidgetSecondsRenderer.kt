@file:SuppressLint("RestrictedApi")

package com.devidea.timeleft.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.creation.profile.WidgetsProfileWriterV6
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.core.graphics.createBitmap
import androidx.appcompat.content.res.AppCompatResources
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.R
import com.devidea.timeleft.ui.components.drawTimeRulerGlow
import com.devidea.timeleft.ui.theme.TimeLayout
import com.devidea.timeleft.ui.theme.TimeRulerTokens
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pinned alpha20 authoring adapter. Some authoring APIs are library-restricted; the platform
 * DrawInstructions API is public. Keep this exception here, never in domain/UI code. No reflection
 * or hidden Android APIs. Revalidate payloads and pacing before upgrading the authoring dependency.
 */
@RequiresApi(35)
internal object WidgetSecondsRenderer {
    fun create(context: Context, size: WidgetDimensions, item: AdapterItem, plan: WidgetSecondsPlan,
        palette: WidgetPalette, showProgress: Boolean, snapshotMillis: Long? = null,
        profile: WidgetSecondsProfile = requireNotNull(WidgetSecondsSupport.capability().productionProfile)): RemoteViews =
        RemoteViews(RemoteViews.DrawInstructions.Builder(listOf(
            createDocument(context, size, item, plan, palette, showProgress, snapshotMillis, profile)
        )).build())

    /** Shared by the published widget and platform tests; explicit profiles enable debug candidates. */
    fun createDocument(context: Context, size: WidgetDimensions, item: AdapterItem, plan: WidgetSecondsPlan,
        palette: WidgetPalette, showProgress: Boolean, snapshotMillis: Long?, profile: WidgetSecondsProfile): ByteArray {
        val density = context.resources.displayMetrics.density
        val width = size.width * density
        val height = size.height * density
        val measure = WidgetTextMeasure(context)
        val padding = measure.px(R.dimen.widget_padding)
        val topPadding = measure.px(if (size.height < 200) R.dimen.widget_compact_vertical_padding else R.dimen.widget_padding)
        val gap = measure.px(R.dimen.widget_gap)
        val smallGap = measure.px(R.dimen.widget_small_gap)
        val touch = measure.px(R.dimen.widget_touch_target)
        val contentWidth = (width - padding * 2).coerceAtLeast(1f)
        val titleWidth = (contentWidth - touch - gap).coerceAtLeast(1f)
        val valueRes = if (size.width >= 340 && size.height >= 260) R.dimen.widget_large_value_size else R.dimen.widget_value_size
        val valueSize = measure.px(valueRes)
        val labelSize = measure.px(R.dimen.widget_label_size)
        val nowMillis = snapshotMillis ?: System.currentTimeMillis()
        val maximum = if (plan.today) 86_399L else max(plan.endSecond - plan.startSecond, plan.startSecond - nowMillis / 1000)
        val includeHours = maximum >= 3600
        val maxText = if (includeHours) "${max(0, maximum / 3600)}:00:00" else "00:00"
        val numberWidth = measure.width(maxText, valueRes)
        val statuses = listOf(R.string.widget_seconds_left, R.string.widget_seconds_until_start, R.string.widget_seconds_finished)
        val statusWidth = statuses.maxOf { measure.width(context.getString(it), R.dimen.widget_label_size) }
        val sameLine = numberWidth + gap + statusWidth <= contentWidth
        val numberPaint = textPaint(valueSize)
        val labelPaint = textPaint(labelSize)
        val valueHeight = -numberPaint.fontMetrics.ascent + numberPaint.fontMetrics.descent +
            if (sameLine) 0f else smallGap - labelPaint.fontMetrics.ascent + labelPaint.fontMetrics.descent
        val title = layout(item.title, measure.px(R.dimen.widget_title_size), titleWidth)
        val rangeWidth = (contentWidth - gap) / 2
        val start = layout(item.startLabel, labelSize, rangeWidth)
        val end = layout(item.endLabel, labelSize, rangeWidth, Layout.Alignment.ALIGN_OPPOSITE)
        val rangeHeight = max(start.height, end.height).toFloat()
        val rulerHeight = TimeRulerTokens.Height * density
        val headingHeight = max(touch, title.height.toFloat())
        val required = topPadding * 2 + headingHeight + gap + valueHeight + gap +
            (if (showProgress) rulerHeight + smallGap else 0f) + rangeHeight
        val writer = createWidgetSecondsWriter(
            CreationDisplayInfo(width.roundToInt(), height.roundToInt(), context.resources.displayMetrics.densityDpi),
            item.title, profile)
        // Render in actual host pixels. RootContentBehavior was removed from the widget profile;
        // scaling the whole document would also incorrectly shrink large accessibility text.
        writer.startRoot()
        writer.startBox(RecordingModifier().width(width).height(height))
        writer.startCanvas(RecordingModifier().width(width).height(height))
        writer.rcPaint.setColor(palette.background).commit()
        val radius = context.resources.getDimension(android.R.dimen.system_app_widget_background_radius)
        writer.drawRoundRect(0f, 0f, width, height, radius, radius)
        if (!size.meetsMinimum || required > height || numberWidth > contentWidth) {
            val hint = layout(context.getString(R.string.widget_seconds_size_hint), labelSize, contentWidth)
            // Same payload kind at every responsive size. Never mix XML and draw instructions.
            writer.save()
            writer.clipRect(padding, topPadding, width - padding, height - topPadding)
            drawLayout(writer, hint, padding, topPadding, palette.onSurface)
            writer.restore()
            writer.endCanvas()
            button(writer, R.id.widgetSecondsConfigure, writer.addText(context.getString(R.string.widget_seconds_size_hint)), width, height)
            writer.endBox(); writer.endRoot()
            return writer.buffer().copyOf(writer.bufferSize())
        }
        val board = palette.layout == TimeLayout.TimeBoard
        val valueTop = if (board) topPadding else topPadding + headingHeight + gap
        val titleTop = if (board) topPadding + valueHeight + gap else topPadding
        drawLayout(writer, title, padding, titleTop, palette.onSurface)
        val refreshLeft = width - padding - touch
        val iconSize = 24f * density // Existing 24dp refresh drawable inside its 48dp target.
        val icon = createBitmap(iconSize.roundToInt(), iconSize.roundToInt())
        AppCompatResources.getDrawable(context, R.drawable.ic_baseline_refresh_24)?.mutate()?.apply {
            setTint(palette.muted); setBounds(0, 0, icon.width, icon.height); draw(Canvas(icon))
        }
        val iconInset = (touch - iconSize) / 2
        writer.drawBitmap(icon, refreshLeft + iconInset, titleTop + iconInset,
            refreshLeft + iconInset + iconSize, titleTop + iconInset + iconSize, null)

        fun expr(vararg values: Float) = writer.floatExpression(*values)
        fun epochDelta(second: Long): Float = writer.asFloatId(writer.integerExpression(
            second, Rc.Time.INT_EPOCH_SECOND, Rc.IntegerExpression.L_SUB))
        val frozen = snapshotMillis?.let { plan.frame(java.time.Instant.ofEpochMilli(it), java.time.ZoneId.systemDefault()) }
        // Use the quantized local clock for Today, matching the app's civil-day calculation/DST.
        val elapsedToday = if (plan.today && frozen == null) expr(Rc.Time.TIME_IN_HR, 3600f, Rc.FloatExpression.MUL,
            Rc.Time.TIME_IN_SEC, Rc.FloatExpression.ADD) else (frozen?.elapsedFraction ?: 0f) * 86_400f
        val endDelta = if (frozen != null) {
            when (frozen.phase) {
                com.devidea.timeleft.calc.TimeRangePhase.Finished -> 0f
                else -> if (plan.today) frozen.seconds.toFloat() else (plan.endSecond - snapshotMillis / 1000).toFloat()
            }
        } else if (plan.today) expr(86_399f, elapsedToday, Rc.FloatExpression.SUB) else epochDelta(plan.endSecond)
        val startDelta = if (plan.today) -1f else if (snapshotMillis != null) (plan.startSecond - snapshotMillis / 1000).toFloat() else epochDelta(plan.startSecond)
        val afterStart = expr(startDelta, 0f, Rc.FloatExpression.MIN)
        val until = expr(startDelta, 0f, Rc.FloatExpression.MAX)
        val remaining = expr(endDelta, 0f, Rc.FloatExpression.MAX)
        val progress = if (plan.today) {
            if (frozen != null) frozen.elapsedFraction else expr(elapsedToday, 86_400f, Rc.FloatExpression.DIV)
        } else expr(afterStart, -1f, Rc.FloatExpression.MUL, (plan.endSecond - plan.startSecond).toFloat(), Rc.FloatExpression.DIV,
            0f, Rc.FloatExpression.MAX, 1f, Rc.FloatExpression.MIN)
        // Register one-second wakeups for absolute-epoch expressions, with no animation clock.
        if (snapshotMillis == null) expr(Rc.Time.TIME_IN_SEC, 0f, Rc.FloatExpression.MUL)

        val valueBaseline = valueTop - numberPaint.fontMetrics.ascent
        val valueX = if (board) padding + (contentWidth - numberWidth - if (sameLine) gap + statusWidth else 0f) / 2 else padding
        val statusX = if (sameLine) valueX + numberWidth + gap else valueX
        val statusBaseline = if (sameLine) valueBaseline else valueBaseline + numberPaint.fontMetrics.descent + smallGap - labelPaint.fontMetrics.ascent
        fun clockText(seconds: Float): Int {
            val minutes = expr(seconds, 60f, Rc.FloatExpression.DIV, Rc.FloatExpression.FLOOR, 60f, Rc.FloatExpression.MOD)
            val secondsPart = expr(seconds, 60f, Rc.FloatExpression.MOD, Rc.FloatExpression.FLOOR)
            fun digits(v: Float, count: Int) = writer.createTextFromFloat(v, count, 0, TextFromFloat.PAD_PRE_ZERO)
            val colon = writer.addText(":")
            var text = writer.textMerge(writer.textMerge(digits(minutes, 2), colon), digits(secondsPart, 2))
            if (includeHours) {
                val hours = expr(seconds, 3600f, Rc.FloatExpression.DIV, Rc.FloatExpression.FLOOR)
                text = writer.textMerge(writer.textMerge(writer.createTextFromFloat(hours, 3, 0, TextFromFloat.PAD_PRE_NONE), colon), text)
            }
            return text
        }
        val texts = listOf(clockText(until), clockText(remaining), clockText(0f))
        val statusTexts = listOf(R.string.widget_seconds_until_start, R.string.widget_seconds_left, R.string.widget_seconds_finished)
            .map { writer.addText(context.getString(it)) }
        val upcoming = expr(startDelta, 0f, Rc.FloatExpression.MAX, 1f, Rc.FloatExpression.MIN)
        val ended = expr(1f, endDelta, 0f, Rc.FloatExpression.MAX, 1f, Rc.FloatExpression.MIN, Rc.FloatExpression.SUB)
        val state = expr(1f, upcoming, Rc.FloatExpression.SUB, ended, Rc.FloatExpression.ADD)
        val text = writer.textLookup(writer.addStringList(*texts.toIntArray()), state)
        val status = writer.textLookup(writer.addStringList(*statusTexts.toIntArray()), state)
        run {
            writer.rcPaint.setColor(palette.onSurface).setTextSize(valueSize).setTypeface(RemoteComposeWriterAndroid.FONT_TYPE_SANS_SERIF, 400, false).commit()
            writer.drawTextRun(text, 0, -1, 0, -1, valueX, valueBaseline, false)
            writer.rcPaint.setColor(palette.muted).setTextSize(labelSize).commit()
            writer.drawTextRun(status, 0, -1, 0, -1, statusX, statusBaseline, false)
        }
        val rulerTop = topPadding + headingHeight + gap + valueHeight + gap
        if (showProgress) {
            val glow = createBitmap(contentWidth.roundToInt(), rulerHeight.roundToInt())
            drawTimeRulerGlow(Canvas(glow), contentWidth, rulerHeight, 0f, palette.primary, palette.glowAlpha)
            val cut = expr(progress, contentWidth, Rc.FloatExpression.MUL, padding, Rc.FloatExpression.ADD)
            writer.conditionalOperations(Rc.Condition.LTE, startDelta, 0f) {
                writer.conditionalOperations(Rc.Condition.GT, endDelta, 0f) {
                    writer.save(); writer.clipRect(cut, rulerTop, width - padding, rulerTop + rulerHeight)
                    writer.drawBitmap(glow, padding, rulerTop, width - padding, rulerTop + rulerHeight, null)
                    writer.restore()
                }
            }
            writer.rcPaint.setStrokeWidth(TimeRulerTokens.StrokeWidth * density).commit()
            val count = TimeRulerTokens.tickCount(contentWidth / density)
            repeat(count) { index ->
                val fraction = (index + .5f) / count
                val tickHeight = (if (index % 5 == 0) TimeRulerTokens.MajorHeight else TimeRulerTokens.MinorHeight) * density
                val x = padding + contentWidth * fraction
                fun tick(color: Int) {
                    writer.rcPaint.setColor(color).commit()
                    writer.drawLine(x, rulerTop + rulerHeight, x, rulerTop + rulerHeight - tickHeight)
                }
                writer.conditionalOperations(Rc.Condition.GT, progress, fraction) { tick(palette.track) }
                writer.conditionalOperations(Rc.Condition.LTE, progress, fraction) { tick(palette.primary) }
            }
        }
        val rangeTop = rulerTop + if (showProgress) rulerHeight + smallGap else 0f
        drawLayout(writer, start, padding, rangeTop, palette.muted)
        drawLayout(writer, end, padding + rangeWidth + gap, rangeTop, palette.muted)
        val description = writer.textMerge(writer.textMerge(writer.addText(item.title + ", "), text),
            writer.textMerge(writer.addText(" "), status))
        writer.endCanvas()
        // Real semantic layout components are required by the framework accessibility bridge.
        // Keep the two targets disjoint; the current description is resolved on accessibility focus,
        // with no live-region announcement on every tick. The detail action checks its expiry again.
        writer.startRow(RecordingModifier().width(width).height(height), 0, 0)
        button(writer, R.id.widgetRoot, description, refreshLeft, height)
        writer.startColumn(RecordingModifier().width(touch).height(height), 0, 0)
        writer.startBox(RecordingModifier().width(touch).height(titleTop)); writer.endBox()
        button(writer, R.id.refresh, writer.addText(context.getString(R.string.widget_refresh)), touch, touch)
        writer.endColumn(); writer.endRow()
        writer.endBox(); writer.endRoot()
        return writer.buffer().copyOf(writer.bufferSize())
    }

    private fun button(writer: RemoteComposeWriterAndroid, action: Int, description: Int,
        width: Float, height: Float) {
        val semantics = RecordingModifier.Element { it.addSemanticsModifier(description, 0, 0, 0, 1, true, true) }
        writer.startBox(RecordingModifier().width(width).height(height)
            .onClick(HostAction(action)).then(semantics))
        writer.endBox()
    }

    private fun textPaint(size: Float) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; typeface = Typeface.create("sans-serif", Typeface.NORMAL); fontFeatureSettings = "tnum"
    }
    private fun layout(text: String, size: Float, width: Float, alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, textPaint(size), width.toInt().coerceAtLeast(1))
            .setIncludePad(false).setAlignment(alignment).setUseLineSpacingFromFallbacks(true).build()

    private fun drawLayout(writer: RemoteComposeWriterAndroid, layout: StaticLayout, x: Float, y: Float, color: Int) {
        writer.rcPaint.setColor(color).setTextSize(layout.paint.textSize).setTypeface(RemoteComposeWriterAndroid.FONT_TYPE_SANS_SERIF, 400, false).commit()
        repeat(layout.lineCount) { line ->
            val text = layout.text.subSequence(layout.getLineStart(line), layout.getLineEnd(line)).toString().trimEnd('\n')
            writer.drawTextRun(text, 0, text.length, 0, text.length, x + layout.getLineLeft(line), y + layout.getLineBaseline(line), false)
        }
    }
}

/** Use V6's validation as well as its legacy header, without its scaling convenience wrapper. */
internal fun createWidgetSecondsWriter(info: CreationDisplayInfo, description: String,
    profile: WidgetSecondsProfile): RemoteComposeWriterAndroid = when (profile) {
    WidgetSecondsProfile.V6 -> WidgetsProfileWriterV6(info, description, RcPlatformProfiles.WIDGETS_V6)
    WidgetSecondsProfile.V7 -> RemoteComposeWriterAndroid(info, description, RcPlatformProfiles.WIDGETS_V7)
}
