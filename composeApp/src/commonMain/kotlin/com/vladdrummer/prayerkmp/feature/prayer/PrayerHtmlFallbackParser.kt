package com.vladdrummer.prayerkmp.feature.prayer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

fun prayerHtmlToAnnotatedStringFallback(source: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val normalizedSource = source
        .replace("\r", "")
        .replace('\u00A0', ' ')
        .replace(Regex("(?m)^[\\t ]+"), "")
    val tagRegex = Regex("""<[^>]+>""", setOf(RegexOption.IGNORE_CASE))
    val styleStack = ArrayDeque<HtmlStyleTag>()
    var cursor = 0

    tagRegex.findAll(normalizedSource).forEach { match ->
        if (match.range.first > cursor) {
            appendChunk(builder, normalizedSource.substring(cursor, match.range.first))
        }
        val tag = match.value.lowercase()
        when {
            tag.startsWith("<br") -> builder.append("\n")
            tag.startsWith("</h1") -> {
                closeStyle(builder, styleStack, HtmlStyleTag.H1)
                builder.append("\n")
            }
            tag.startsWith("</h2") -> {
                closeStyle(builder, styleStack, HtmlStyleTag.H2)
                builder.append("\n")
            }
            tag.startsWith("</h3") -> {
                closeStyle(builder, styleStack, HtmlStyleTag.H3)
                builder.append("\n")
            }
            tag.startsWith("</p") || tag.startsWith("</div") || tag.startsWith("</li") -> builder.append("\n")
            tag.startsWith("<h1") -> openStyle(builder, styleStack, HtmlStyleTag.H1, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp))
            tag.startsWith("<h2") -> openStyle(builder, styleStack, HtmlStyleTag.H2, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp))
            tag.startsWith("<h3") -> openStyle(builder, styleStack, HtmlStyleTag.H3, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
            tag.startsWith("<b") || tag.startsWith("<strong") -> openStyle(builder, styleStack, HtmlStyleTag.Bold, SpanStyle(fontWeight = FontWeight.Bold))
            tag.startsWith("<i") || tag.startsWith("<em") -> openStyle(builder, styleStack, HtmlStyleTag.Italic, SpanStyle(fontStyle = FontStyle.Italic))
            tag.startsWith("<u") -> openStyle(builder, styleStack, HtmlStyleTag.Underline, SpanStyle(textDecoration = TextDecoration.Underline))
            tag.startsWith("<font") -> {
                val colorValue = extractFontColor(tag)
                if (colorValue != null) {
                    openStyle(builder, styleStack, HtmlStyleTag.Font, SpanStyle(color = colorValue))
                }
            }
            tag.startsWith("</font") -> closeStyle(builder, styleStack, HtmlStyleTag.Font)
            tag.startsWith("</b") || tag.startsWith("</strong") -> closeStyle(builder, styleStack, HtmlStyleTag.Bold)
            tag.startsWith("</i") || tag.startsWith("</em") -> closeStyle(builder, styleStack, HtmlStyleTag.Italic)
            tag.startsWith("</u") -> closeStyle(builder, styleStack, HtmlStyleTag.Underline)
        }
        cursor = match.range.last + 1
    }
    if (cursor < normalizedSource.length) {
        appendChunk(builder, normalizedSource.substring(cursor))
    }
    while (styleStack.isNotEmpty()) {
        builder.pop()
        styleStack.removeLast()
    }
    return builder.toAnnotatedString()
}

private fun appendChunk(builder: AnnotatedString.Builder, rawChunk: String) {
    val chunk = rawChunk.replace('\t', ' ')
    if (chunk.isBlank()) return
    builder.append(chunk)
}

private fun openStyle(
    builder: AnnotatedString.Builder,
    stack: ArrayDeque<HtmlStyleTag>,
    tag: HtmlStyleTag,
    style: SpanStyle,
) {
    builder.pushStyle(style)
    stack.addLast(tag)
}

private fun closeStyle(
    builder: AnnotatedString.Builder,
    stack: ArrayDeque<HtmlStyleTag>,
    target: HtmlStyleTag,
) {
    if (stack.isEmpty()) return
    while (stack.isNotEmpty()) {
        val tag = stack.removeLast()
        builder.pop()
        if (tag == target) return
    }
}

private enum class HtmlStyleTag {
    H1,
    H2,
    H3,
    Bold,
    Italic,
    Underline,
    Font,
}

private fun extractFontColor(tag: String): Color? {
    val match = Regex("""color\s*=\s*["'](#?[0-9a-f]{6,8})["']""", setOf(RegexOption.IGNORE_CASE)).find(tag) ?: return null
    val raw = match.groupValues[1].removePrefix("#")
    val normalized = when (raw.length) {
        6 -> "ff$raw"
        8 -> raw
        else -> return null
    }
    val value = normalized.toLongOrNull(16) ?: return null
    return Color(value)
}

