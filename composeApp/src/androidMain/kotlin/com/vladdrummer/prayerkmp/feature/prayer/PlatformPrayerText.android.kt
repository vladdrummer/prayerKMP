package com.vladdrummer.prayerkmp.feature.prayer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ReplacementSpan
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

private data class CounterToken(
    val end: Int,
    val target: Int,
)

private data class CounterRenderResult(
    val text: SpannableStringBuilder,
    val hasCounters: Boolean,
)

private val counterRegex = Regex("""\(?\s*(12|40)\s*раз(?:а)?\s*\)?""", RegexOption.IGNORE_CASE)

@Composable
actual fun PlatformPrayerText(
    html: String,
    fontSizeSp: Int,
    fontIndex: Int,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val typeface = rememberPrayerTypeface(context, fontIndex)
    val counterState = remember(html) { mutableStateMapOf<Int, Int>() }
    val chipBg = Color.WHITE
    val chipFg = MaterialTheme.colorScheme.onBackground.toArgb()

    AndroidView(
        modifier = modifier,
        factory = {
            TextView(context).apply {
                setTextColor(textColor)
                textSize = fontSizeSp.toFloat()
                typeface?.let { tf -> setTypeface(tf) }
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            textView.textSize = fontSizeSp.toFloat()
            textView.typeface = typeface

            val rendered = buildPrayerTextWithCounters(
                sourceHtml = html,
                chipBackgroundColor = chipBg,
                chipTextColor = chipFg,
                state = counterState,
                textView = textView,
            )

            if (rendered.hasCounters) {
                textView.setTextIsSelectable(false)
                textView.movementMethod = LinkMovementMethod.getInstance()
                textView.linksClickable = true
                textView.highlightColor = Color.TRANSPARENT
            } else {
                textView.setTextIsSelectable(true)
                textView.movementMethod = null
                textView.linksClickable = false
            }
            textView.text = rendered.text
        }
    )
}

@Composable
private fun rememberPrayerTypeface(context: android.content.Context, fontIndex: Int): Typeface? {
    return remember(context, fontIndex) {
        when (fontIndex) {
            0 -> Typeface.SERIF
            1 -> Typeface.SANS_SERIF
            2 -> Typeface.createFromAsset(context.assets, "fonts/css.TTF")
            3 -> Typeface.createFromAsset(context.assets, "fonts/FITPUSTV.TTF")
            4 -> Typeface.createFromAsset(context.assets, "fonts/rusfont238.ttf")
            else -> Typeface.SERIF
        }
    }
}

private fun buildPrayerTextWithCounters(
    sourceHtml: String,
    chipBackgroundColor: Int,
    chipTextColor: Int,
    state: MutableMap<Int, Int>,
    textView: TextView,
): CounterRenderResult {
    val base = HtmlCompat.fromHtml(sourceHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
    val builder = SpannableStringBuilder(base)
    val plain = builder.toString()
    val tokens = counterRegex.findAll(plain).mapIndexed { index, match ->
        val target = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
        CounterToken(
            end = match.range.last + 1,
            target = target,
        ) to index
    }.filter { it.first.target > 0 }.toList()

    tokens.asReversed().forEach { (token, id) ->
        val insertion = token.end
        builder.insert(insertion, " \uFFFC")
        val spanStart = insertion + 1
        val spanEnd = spanStart + 1

        val countSpan = CounterChipSpan(
            id = id,
            target = token.target,
            state = state,
            bgColor = chipBackgroundColor,
            textColor = chipTextColor,
        )
        val clickSpan = CounterClickSpan(
            id = id,
            target = token.target,
            state = state,
            textView = textView,
        )

        builder.setSpan(countSpan, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(clickSpan, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    return CounterRenderResult(
        text = builder,
        hasCounters = tokens.isNotEmpty(),
    )
}

private class CounterClickSpan(
    private val id: Int,
    private val target: Int,
    private val state: MutableMap<Int, Int>,
    private val textView: TextView,
) : ClickableSpan() {
    override fun onClick(widget: android.view.View) {
        val current = state[id] ?: 0
        if (current < target) {
            state[id] = current + 1
        }
        textView.invalidate()
        textView.requestLayout()
    }

    override fun updateDrawState(ds: android.text.TextPaint) {
        ds.isUnderlineText = false
    }
}

private class CounterChipSpan(
    private val id: Int,
    private val target: Int,
    private val state: MutableMap<Int, Int>,
    private val bgColor: Int,
    private val textColor: Int,
) : ReplacementSpan() {
    private val hPad = 40.5f
    private val vPad = 15.75f
    private val minWidth = 81f
    private val strokeWidth = 4.5f
    private val shadowOffsetY = 3f
    private val shadowColor = Color.argb(60, 0, 0, 0)

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        val label = currentLabel()
        val textWidth = paint.measureText(label)
        return maxOf((textWidth + hPad * 2f).toInt(), minWidth.toInt())
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val label = currentLabel()
        val oldColor = paint.color
        val oldStyle = paint.style
        val oldStrokeWidth = paint.strokeWidth

        val textWidth = paint.measureText(label)
        val width = maxOf(textWidth + hPad * 2f, minWidth)
        val fm = paint.fontMetrics
        val rectTop = y + fm.ascent - vPad
        val rectBottom = y + fm.descent + vPad
        val rect = RectF(x, rectTop, x + width, rectBottom)
        val corner = (rectBottom - rectTop) / 2f

        paint.color = shadowColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            RectF(rect.left, rect.top + shadowOffsetY, rect.right, rect.bottom + shadowOffsetY),
            corner,
            corner,
            paint
        )

        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, corner, corner, paint)

        paint.color = textColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        canvas.drawRoundRect(rect, corner, corner, paint)

        paint.color = textColor
        paint.style = Paint.Style.FILL
        val textX = x + (width - textWidth) / 2f
        canvas.drawText(label, textX, y.toFloat(), paint)

        paint.color = oldColor
        paint.style = oldStyle
        paint.strokeWidth = oldStrokeWidth
    }

    private fun currentLabel(): String {
        val value = state[id] ?: 0
        return if (value >= target) "✓" else "$value"
    }
}
