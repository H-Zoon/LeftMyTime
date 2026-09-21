package com.devidea.timeleft.widget

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import com.devidea.timeleft.RemainingTimeGroup
import com.devidea.timeleft.R

/** Uses the same number/unit roles as RemainingTimeText. No parsing of translated values. */
internal fun widgetValueText(
    context: Context,
    groups: List<RemainingTimeGroup>,
    fallback: String,
    unitSizeRes: Int,
    muted: Int,
): CharSequence {
    if (groups.isEmpty()) return fallback
    return SpannableStringBuilder().apply {
        groups.forEachIndexed { index, group ->
            if (index > 0) append(' ')
            append(group.number)
            val start = length
            append('\u00a0').append(group.unit)
            setSpan(AbsoluteSizeSpan(context.resources.getDimensionPixelSize(unitSizeRes)), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(muted), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(TypefaceSpan("sans-serif"), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

/** Keep the localized status beside the value on the same baseline, with a separate text role. */
internal fun widgetValueWithStatus(context: Context, value: CharSequence, templateRes: Int?, muted: Int): CharSequence {
    if (templateRes == null) return value
    val parts = context.getString(templateRes).split("^1", limit = 2)
    require(parts.size == 2) { "Widget value template must contain ^1" }
    return SpannableStringBuilder().apply {
        fun label(text: String) {
            if (text.isEmpty()) return
            val start = length
            append(text)
            setSpan(AbsoluteSizeSpan(context.resources.getDimensionPixelSize(R.dimen.widget_label_size)), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(muted), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(TypefaceSpan("sans-serif"), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        label(parts[0])
        append(value)
        label(parts[1])
    }
}

internal class WidgetTextMeasure(private val context: Context) {
    fun px(resource: Int): Float = context.resources.getDimension(resource)
    fun dp(value: Float): Float = value * context.resources.displayMetrics.density
    private fun paint(sizeRes: Int) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
        textSize = px(sizeRes)
        typeface = Typeface.create(if (sizeRes == R.dimen.widget_summary_value_size) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        fontFeatureSettings = "tnum"
    }
    fun height(text: CharSequence, sizeRes: Int, width: Float): Int =
        StaticLayout.Builder.obtain(text, 0, text.length, paint(sizeRes), width.toInt().coerceAtLeast(1))
            // Builder defaults to BREAK_STRATEGY_SIMPLE, matching XML on every supported API.
            .setIncludePad(false)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .apply { if (Build.VERSION.SDK_INT >= 28) setUseLineSpacingFromFallbacks(true) }.build().height

    fun width(text: CharSequence, sizeRes: Int): Float = Layout.getDesiredWidth(text, paint(sizeRes))

    fun unbrokenWidth(text: CharSequence, sizeRes: Int): Float {
        // A space can wrap a group; NBSP keeps a number and its unit together.
        val boundaries = listOf(-1) + text.indices.filter { text[it] == ' ' || text[it] == '\n' } + text.length
        return boundaries.zipWithNext().maxOfOrNull { (start, end) ->
            width(text.subSequence(start + 1, end), sizeRes)
        } ?: 0f
    }

    fun fitsUnbroken(text: CharSequence, sizeRes: Int, width: Float): Boolean = unbrokenWidth(text, sizeRes) <= width
}
