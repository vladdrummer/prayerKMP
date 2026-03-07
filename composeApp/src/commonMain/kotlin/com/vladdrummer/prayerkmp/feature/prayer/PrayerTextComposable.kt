package com.vladdrummer.prayerkmp.feature.prayer

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.prayer_css
import kotlinproject.composeapp.generated.resources.prayer_fitpustv
import kotlinproject.composeapp.generated.resources.prayer_rusfont238
import org.jetbrains.compose.resources.Font

@Composable
internal fun PrayerTextComposable(
    html: String,
    fontSizeSp: Int,
    fontIndex: Int,
    modifier: Modifier = Modifier,
) {
    val baseAnnotated = remember(html) { prayerHtmlToAnnotatedStringFallback(html) }
    val counterState = remember(html) { mutableStateMapOf<Int, Int>() }
    LaunchedEffect(html) { counterState.clear() }
    val (styledAnnotated, counterSpecs) = remember(baseAnnotated) {
        buildAnnotatedWithCounters(baseAnnotated)
    }
    val baseStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSizeSp.sp,
            fontFamily = prayerFontFamily(fontIndex)
    )

    val inlineContent = remember(counterSpecs, counterState.toMap()) {
        counterSpecs.associate { spec ->
            spec.inlineId to InlineTextContent(
                Placeholder(
                    width = 36.sp,
                    height = 36.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                val current = counterState[spec.id] ?: 0
                val label = if (current >= spec.target) "✓" else current.toString()
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), CircleShape)
                        .clickable {
                            if (current < spec.target) counterState[spec.id] = current + 1
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    Text(
        text = styledAnnotated,
        style = baseStyle,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
        inlineContent = inlineContent
    )
}

@Composable
private fun prayerFontFamily(fontIndex: Int): FontFamily {
    return when (fontIndex) {
        1 -> FontFamily.SansSerif
        2 -> FontFamily(Font(Res.font.prayer_css))
        3 -> FontFamily(Font(Res.font.prayer_fitpustv))
        4 -> FontFamily(Font(Res.font.prayer_rusfont238))
        else -> FontFamily.Serif
    }
}

private val counterRegex = Regex(
    """\(\s*(12|40)\s*(?:раз(?:а)?)?\s*\)\.?|\b(12|40)\s*раз(?:а)?\.?""",
    RegexOption.IGNORE_CASE
)

private data class CounterSpec(
    val id: Int,
    val target: Int,
    val inlineId: String,
)

private fun buildAnnotatedWithCounters(
    source: AnnotatedString,
): Pair<AnnotatedString, List<CounterSpec>> {
    val specs = mutableListOf<CounterSpec>()
    val matches = counterRegex.findAll(source.text).toList()
    if (matches.isEmpty()) return source to emptyList()

    var cursor = 0
    val annotated = buildAnnotatedString {
        matches.forEachIndexed { id, match ->
            val target = extractCounterTarget(match) ?: return@forEachIndexed
            val insertionPoint = computeCounterInsertionPoint(source.text, match.range.last + 1, target)
            if (cursor < insertionPoint) {
                append(source.subSequence(cursor, insertionPoint))
                cursor = insertionPoint
            }
            val inlineId = "counter_$id"
            append(" ")
            appendInlineContent(inlineId, "[0]")
            append(" ")
            specs += CounterSpec(id = id, target = target, inlineId = inlineId)
        }
        if (cursor < source.length) {
            append(source.subSequence(cursor, source.length))
        }
    }
    return annotated to specs
}

private fun extractCounterTarget(match: MatchResult): Int? {
    return match.groupValues
        .drop(1)
        .firstOrNull { it.isNotBlank() }
        ?.toIntOrNull()
}

private fun computeCounterInsertionPoint(
    text: String,
    matchEndExclusive: Int,
    target: Int,
): Int {
    if (target != 40 && target != 12) return matchEndExclusive
    val lineEnd = text.indexOf('\n', startIndex = matchEndExclusive).let { if (it == -1) text.length else it }
    val lineTail = normalizeCounterTemplate(text.substring(matchEndExclusive, lineEnd))
    val templates = if (target == 40) COUNTER_40_TAIL_TEMPLATES else COUNTER_12_TAIL_TEMPLATES
    return if (lineTail in templates) lineEnd else matchEndExclusive
}

private fun normalizeCounterTemplate(value: String): String {
    return value
        .replace('\u00A0', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()
        .replace(Regex("""^[\s\.,:;!?\-–—]+"""), "")
        .lowercase()
}

private val COUNTER_40_TAIL_TEMPLATES = setOf(
    "и поклонися, елико ти мощно.",
    "и поклоны, сколько хочешь.)",
    "и поклонов, елико мощно, с молитвою :",
)

private val COUNTER_12_TAIL_TEMPLATES = setOf(
    "с поясными поклонами",
)
