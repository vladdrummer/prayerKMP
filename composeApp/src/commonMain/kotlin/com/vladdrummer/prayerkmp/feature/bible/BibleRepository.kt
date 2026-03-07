package com.vladdrummer.prayerkmp.feature.bible

import kotlinproject.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BibleRepository {
    private const val BOOKS_XML_PATH = "files/book.xml"

    private var booksCache: List<BibleBook> = emptyList()
    private var booksByNameCache: Map<String, BibleBook> = emptyMap()
    private var isLoaded: Boolean = false

    suspend fun getBooks(): List<BibleBookSummary> = withContext(Dispatchers.Default) {
        ensureLoaded()
        booksCache.map { BibleBookSummary(name = it.name, chapters = it.chapters) }
    }

    suspend fun getChapters(bookName: String): List<Int> = withContext(Dispatchers.Default) {
        ensureLoaded()
        booksByNameCache[bookName]?.chapters.orEmpty()
    }

    suspend fun getChapterHtml(bookName: String, chapterNumber: Int): String? = withContext(Dispatchers.Default) {
        ensureLoaded()
        val book = booksByNameCache[bookName] ?: return@withContext null
        val chapterBody = extractChapterBody(book.body, chapterNumber) ?: return@withContext null
        renderChapterHtml(chapterBody)
    }

    private suspend fun ensureLoaded() {
        if (isLoaded) return
        val xml = Res.readBytes(BOOKS_XML_PATH).decodeToString()
        val books = parseBooks(xml)
        booksCache = books
        booksByNameCache = books.associateBy { it.name }
        isLoaded = true
    }

    private fun parseBooks(xml: String): List<BibleBook> {
        val books = mutableListOf<BibleBook>()
        val bookRegex = Regex("""<BOOK\b([^>]*)>([\s\S]*?)</BOOK>""")
        val nameRegex = Regex("""\bname="([^"]+)"""")
        val chapterRegex = Regex("""<CHAPTER\b[^>]*\bnumber="([^"]+)"[^>]*>""")
        bookRegex.findAll(xml).forEach { match ->
            val attrs = match.groupValues[1]
            val body = match.groupValues[2]
            val name = decodeXmlEntities(nameRegex.find(attrs)?.groupValues?.get(1).orEmpty()).trim()
            if (name.isBlank()) return@forEach
            val chapters = chapterRegex.findAll(body)
                .mapNotNull { chapterMatch -> chapterMatch.groupValues[1].trim().toIntOrNull() }
                .toList()
            books += BibleBook(name = name, body = body, chapters = chapters)
        }
        return books
    }

    private fun extractChapterBody(bookBody: String, chapter: Int): String? {
        val escaped = Regex.escape(chapter.toString())
        val chapterRegex = Regex("""<CHAPTER\b[^>]*\bnumber="$escaped"[^>]*>([\s\S]*?)</CHAPTER>""")
        return chapterRegex.find(bookBody)?.groupValues?.get(1)
    }

    private fun renderChapterHtml(chapterBody: String): String {
        val lineRegex = Regex("""<LINE\b[^>]*\bnumber="(\d+)"[^>]*>([\s\S]*?)</LINE>""")
        return buildString {
            lineRegex.findAll(chapterBody).forEach { match ->
                val verse = match.groupValues[1].toIntOrNull() ?: return@forEach
                val text = decodeXmlEntities(match.groupValues[2]).trim().replace(Regex("""\s+"""), " ")
                append("<sup><small><font color=\"#aa2c2c\">")
                append(verse)
                append("</font></small></sup> ")
                append(highlightFirstLetter(text))
                append("<br />")
            }
        }
    }

    private fun highlightFirstLetter(text: String): String {
        if (text.isBlank()) return text
        val firstIndex = text.indexOfFirst { !it.isWhitespace() }
        if (firstIndex < 0) return text
        val first = text[firstIndex]
        val prefix = text.substring(0, firstIndex)
        val suffix = text.substring(firstIndex + 1)
        return "$prefix<font color=\"#aa2c2c\">$first</font>$suffix"
    }

    private fun decodeXmlEntities(value: String): String {
        var decoded = value
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
        val decimalEntityRegex = Regex("""&#(\d+);""")
        decoded = decimalEntityRegex.replace(decoded) { match ->
            val code = match.groupValues[1].toIntOrNull() ?: return@replace match.value
            code.toChar().toString()
        }
        val hexEntityRegex = Regex("""&#x([0-9a-fA-F]+);""")
        decoded = hexEntityRegex.replace(decoded) { match ->
            val code = match.groupValues[1].toIntOrNull(16) ?: return@replace match.value
            code.toChar().toString()
        }
        return decoded
    }
}

data class BibleBookSummary(
    val name: String,
    val chapters: List<Int>,
)

private data class BibleBook(
    val name: String,
    val body: String,
    val chapters: List<Int>,
)
