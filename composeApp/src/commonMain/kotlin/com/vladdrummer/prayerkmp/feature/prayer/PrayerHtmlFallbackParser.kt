package com.vladdrummer.prayerkmp.feature.prayer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

fun normalizePrayerHtmlForRender(source: String): String {
    return source
        .replace("\r", "")
        .replace('\u00A0', ' ')
        .replace(Regex("(?m)^[\\t ]+"), "")
        // Some source blocks have <h5><font ...>... </h5> without </font>; close it explicitly.
        .replace(Regex("""(</h[1-6]>)""", RegexOption.IGNORE_CASE), "</font>$1")
        .replace("</font></font>", "</font>")
}

fun prayerHtmlToAnnotatedStringFallback(source: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val normalizedSource = normalizePrayerHtmlForRender(source)
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
            tag.startsWith("<p") || tag.startsWith("<div") || tag.startsWith("<li") -> Unit
            tag.startsWith("</h1") -> {
                closeBlockStyle(builder, styleStack, HtmlStyleTag.H1)
            }
            tag.startsWith("</h2") -> {
                closeBlockStyle(builder, styleStack, HtmlStyleTag.H2)
            }
            tag.startsWith("</h3") -> {
                closeBlockStyle(builder, styleStack, HtmlStyleTag.H3)
            }
            tag.startsWith("</h4") -> closeBlockStyle(builder, styleStack, HtmlStyleTag.H4)
            tag.startsWith("</h5") -> closeBlockStyle(builder, styleStack, HtmlStyleTag.H5)
            tag.startsWith("</h6") -> closeBlockStyle(builder, styleStack, HtmlStyleTag.H6)
            tag.startsWith("</p") || tag.startsWith("</div") || tag.startsWith("</li") -> {
                closeAllStyles(builder, styleStack)
                appendParagraphBreak(builder)
            }
            tag.startsWith("<h1") -> openStyle(builder, styleStack, HtmlStyleTag.H1, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp))
            tag.startsWith("<h2") -> openStyle(builder, styleStack, HtmlStyleTag.H2, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp))
            tag.startsWith("<h3") -> openStyle(builder, styleStack, HtmlStyleTag.H3, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
            tag.startsWith("<h4") -> openStyle(builder, styleStack, HtmlStyleTag.H4, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp))
            tag.startsWith("<h5") -> openStyle(builder, styleStack, HtmlStyleTag.H5, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp))
            tag.startsWith("<h6") -> openStyle(builder, styleStack, HtmlStyleTag.H6, SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
            tag.startsWith("<b") || tag.startsWith("<strong") -> openStyle(builder, styleStack, HtmlStyleTag.Bold, SpanStyle(fontWeight = FontWeight.Bold))
            tag.startsWith("<i") || tag.startsWith("<em") -> openStyle(
                builder,
                styleStack,
                HtmlStyleTag.Italic,
                SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal)
            )
            tag.startsWith("<u") -> openStyle(builder, styleStack, HtmlStyleTag.Underline, SpanStyle(textDecoration = TextDecoration.Underline))
            tag.startsWith("<sup") -> openStyle(
                builder,
                styleStack,
                HtmlStyleTag.Superscript,
                SpanStyle(
                    baselineShift = BaselineShift.Superscript,
                    fontSize = 12.sp
                )
            )
            tag.startsWith("<sub") -> openStyle(
                builder,
                styleStack,
                HtmlStyleTag.Subscript,
                SpanStyle(
                    baselineShift = BaselineShift.Subscript,
                    fontSize = 12.sp
                )
            )
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
            tag.startsWith("</sup") -> closeStyle(builder, styleStack, HtmlStyleTag.Superscript)
            tag.startsWith("</sub") -> closeStyle(builder, styleStack, HtmlStyleTag.Subscript)
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
    val chunk = decodeHtmlEntities(rawChunk.replace('\t', ' '))
        .replace(Regex("""\s+"""), " ")
    if (chunk.isBlank()) return
    val current = builder.toString()
    val endsWithWhitespace = current.isNotEmpty() && current.last().isWhitespace()
    val normalizedChunk = if (builder.length == 0 || endsWithWhitespace) {
        chunk.trimStart()
    } else {
        chunk
    }
    builder.append(normalizedChunk)
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
    H4,
    H5,
    H6,
    Bold,
    Italic,
    Underline,
    Superscript,
    Subscript,
    Font,
}

private fun closeBlockStyle(
    builder: AnnotatedString.Builder,
    stack: ArrayDeque<HtmlStyleTag>,
    headingTag: HtmlStyleTag,
) {
    closeStyle(builder, stack, headingTag)
    closeAllStyles(builder, stack)
    appendParagraphBreak(builder)
}

private fun closeAllStyles(
    builder: AnnotatedString.Builder,
    stack: ArrayDeque<HtmlStyleTag>,
) {
    while (stack.isNotEmpty()) {
        builder.pop()
        stack.removeLast()
    }
}

private fun appendParagraphBreak(builder: AnnotatedString.Builder) {
    val text = builder.toString()
    if (text.isEmpty()) return
    if (text.endsWith("\n\n")) return
    if (text.endsWith("\n")) {
        builder.append("\n")
    } else {
        builder.append("\n\n")
    }
}

private fun decodeHtmlEntities(value: String): String {
    var decoded = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("\\n", "\n")

    val decimalEntityRegex = Regex("""&#(\d+);""")
    decoded = decimalEntityRegex.replace(decoded) { match ->
        val code = match.groupValues[1].toIntOrNull() ?: return@replace match.value
        codePointToString(code) ?: match.value
    }

    val hexEntityRegex = Regex("""&#x([0-9a-fA-F]+);""")
    decoded = hexEntityRegex.replace(decoded) { match ->
        val code = match.groupValues[1].toIntOrNull(16) ?: return@replace match.value
        codePointToString(code) ?: match.value
    }

    return decoded
}

private fun codePointToString(codePoint: Int): String? {
    if (codePoint < 0 || codePoint > 0x10FFFF) return null
    return if (codePoint <= 0xFFFF) {
        codePoint.toChar().toString()
    } else {
        val value = codePoint - 0x10000
        val high = ((value shr 10) + 0xD800).toChar()
        val low = ((value and 0x3FF) + 0xDC00).toChar()
        "$high$low"
    }
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
