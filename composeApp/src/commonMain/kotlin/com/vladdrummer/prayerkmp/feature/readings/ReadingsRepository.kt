package com.vladdrummer.prayerkmp.feature.readings

import com.vladdrummer.prayerkmp.feature.prayer.currentLocalDate
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.strings.getString
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinproject.composeapp.generated.resources.Res
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ReadingsRepository {
    private const val READINGS_URL = "https://vladdrummer.ru/readings.json"
    private const val BOOKS_XML_PATH = "files/book.xml"
    private const val READINGS_XML_PATH = "files/readings.xml"
    private const val CACHE_NOT_FOUND = "__NOT_FOUND__"
    private const val CACHE_KEY_PREFIX = "readings_cache_"
    private const val CALENDAR_URL_TEMPLATE = "https://days.pravoslavie.ru/Days/%s.html"

    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient by lazy { HttpClient() }

    private var booksByName: Map<String, String>? = null
    private var localReadingsByDate: Map<String, List<ReadingRef>>? = null
    private var nameDaysByDate: Map<String, String>? = null

    data class CalendarPreview(
        val celebrating: String?,
        val fast: String?,
        val hymns: List<CalendarHymn>,
    )

    data class CalendarHymn(
        val title: String,
        val glas: String?,
        val text: String,
    )

    suspend fun loadTodayCalendarPreview(): CalendarPreview {
        val sourceDate = currentLocalDate() - DatePeriod(days = 13)
        val day = sourceDate.dayOfMonth.toString().padStart(2, '0')
        val month = sourceDate.monthNumber.toString().padStart(2, '0')
        val dateKey = "${sourceDate.year}$month$day"
        val url = "https://days.pravoslavie.ru/Days/$dateKey.html"
        readingsLog("loadTodayCalendarPreview start, dateKey=$dateKey, url=$url")

        val html = runCatching { httpClient.get(url).bodyAsText() }
            .onFailure { readingsLog("calendar html request failed: ${it.message}") }
            .getOrNull()
            ?: return CalendarPreview(null, null, emptyList())

        readingsLog("calendar html loaded, chars=${html.length}")
        val celebrating = extractCalendarCelebrating(html)
        val fast = extractCalendarFast(html)
        val hymns = extractCalendarHymns(html)
        readingsLog(
            "calendar parsed celebrating='${celebrating.orEmpty().take(80)}', fast='${fast.orEmpty().take(80)}', hymns=${hymns.size}"
        )
        return CalendarPreview(celebrating = celebrating, fast = fast, hymns = hymns)
    }

    suspend fun loadTodayNameDays(): String? {
        val today = currentLocalDate()
        val key = "${today.dayOfMonth}.${today.monthNumber}."
        val map = nameDaysByDate ?: run {
            val xml = Res.readBytes("files/imenini.xml").decodeToString()
            val parsed = parseNameDays(xml)
            nameDaysByDate = parsed
            readingsLog("name days xml parsed, dates=${parsed.size}")
            parsed
        }
        val result = map[key]
        readingsLog("loadTodayNameDays key=$key found=${!result.isNullOrBlank()}")
        return result
    }

    suspend fun loadTodayReferences(storage: AppStorage): List<String> {
        val today = currentLocalDate()
        val dateCompact = "${today.dayOfMonth}.${today.monthNumber}.${today.year}"
        val datePadded = "${today.dayOfMonth.toString().padStart(2, '0')}.${today.monthNumber.toString().padStart(2, '0')}.${today.year}"
        readingsLog("loadTodayReferences start, dateCompact=$dateCompact, datePadded=$datePadded")

        val cacheState = loadReadingsFromCache(storage, datePadded)
        val readings = when (cacheState) {
            is CacheState.Found -> {
                readingsLog("refs cache hit: found refs=${cacheState.readings.size}, skip network")
                cacheState.readings
            }
            CacheState.NotFound -> {
                readingsLog("refs cache hit: not found marker, trying network refresh")
                val networkReadings = loadReadingsFromNetwork(dateCompact, datePadded)
                if (!networkReadings.isNullOrEmpty()) {
                    saveReadingsToCache(storage, datePadded, networkReadings)
                    networkReadings
                } else {
                    val localReadings = loadReadingsFromLocalXml(dateCompact, datePadded)
                    if (!localReadings.isNullOrEmpty()) {
                        saveReadingsToCache(storage, datePadded, localReadings)
                        localReadings
                    } else {
                        saveNotFoundToCache(storage, datePadded)
                        emptyList()
                    }
                }
            }
            CacheState.Miss -> {
                readingsLog("refs cache miss, loading from network")
                val networkReadings = loadReadingsFromNetwork(dateCompact, datePadded)
                if (!networkReadings.isNullOrEmpty()) {
                    saveReadingsToCache(storage, datePadded, networkReadings)
                    networkReadings
                } else {
                    val localReadings = loadReadingsFromLocalXml(dateCompact, datePadded)
                    if (!localReadings.isNullOrEmpty()) {
                        saveReadingsToCache(storage, datePadded, localReadings)
                        localReadings
                    } else {
                        saveNotFoundToCache(storage, datePadded)
                        emptyList()
                    }
                }
            }
        }
        val refs = readings.map { ref ->
            if (ref.from != 0) {
                "${ref.book} ${ref.chapter}:${ref.from}-${ref.to}"
            } else {
                "${ref.book} ${ref.chapter}"
            }
        }
        readingsLog("loadTodayReferences success, refs=${refs.size}")
        return refs
    }

    suspend fun loadTodayHtml(
        storage: AppStorage,
        introHtml: String? = null,
        outroHtml: String? = null,
    ): String {
        val today = currentLocalDate()
        val dateCompact = "${today.dayOfMonth}.${today.monthNumber}.${today.year}"
        val datePadded = "${today.dayOfMonth.toString().padStart(2, '0')}.${today.monthNumber.toString().padStart(2, '0')}.${today.year}"
        readingsLog("loadTodayHtml start, dateCompact=$dateCompact, datePadded=$datePadded")

        val cacheState = loadReadingsFromCache(storage, datePadded)
        val readings = when (cacheState) {
            is CacheState.Found -> {
                readingsLog("cache hit: found refs=${cacheState.readings.size}, skip network")
                cacheState.readings
            }
            CacheState.NotFound -> {
                readingsLog("cache hit: not found marker, trying network refresh")
                val networkReadings = loadReadingsFromNetwork(dateCompact, datePadded)
                if (!networkReadings.isNullOrEmpty()) {
                    saveReadingsToCache(storage, datePadded, networkReadings)
                    networkReadings
                } else {
                    val localReadings = loadReadingsFromLocalXml(dateCompact, datePadded)
                    if (!localReadings.isNullOrEmpty()) {
                        readingsLog("cache not-found, but local xml has refs=${localReadings.size}, replacing cache")
                        saveReadingsToCache(storage, datePadded, localReadings)
                        localReadings
                    } else {
                        saveNotFoundToCache(storage, datePadded)
                        null
                    }
                }
            }
            CacheState.Miss -> {
                readingsLog("cache miss, loading from network")
                val networkReadings = loadReadingsFromNetwork(dateCompact, datePadded)
                if (!networkReadings.isNullOrEmpty()) {
                    saveReadingsToCache(storage, datePadded, networkReadings)
                    networkReadings
                } else {
                    val localReadings = loadReadingsFromLocalXml(dateCompact, datePadded)
                    if (!localReadings.isNullOrEmpty()) {
                        readingsLog("network empty, fallback local xml refs=${localReadings.size}, cache it")
                        saveReadingsToCache(storage, datePadded, localReadings)
                        localReadings
                    } else {
                        saveNotFoundToCache(storage, datePadded)
                        null
                    }
                }
            }
        }
        readingsLog("loadTodayHtml readings loaded, count=${readings?.size ?: 0}")

        if (readings.isNullOrEmpty()) {
            readingsLog("loadTodayHtml no readings found, returning result_not_found")
            return getString("result_not_found")
        }

        val titleText = "${getString("gospel_readings_for")} ${today.dayOfMonth.toString().padStart(2, '0')}.${today.monthNumber.toString().padStart(2, '0')}.${today.year}"
        val introText = introHtml ?: getString("readings_prayer_beginning")
        val outroText = outroHtml ?: getString("readings_prayer_end")
        val chapterLabel = getString("chapter_bible")
        val rendered = buildString {
            append("<font color=\"#aa2c2c\"><b>")
            append(titleText)
            append("</b></font><br /><br />")
            append(introText)
            readings.forEach { ref ->
                readingsLog("render heading for ${ref.book} ${ref.chapter}:${ref.from}-${ref.to}")
                append("<font color=\"#aa2c2c\"><b>")
                append(ref.book)
                append(' ')
                append(chapterLabel)
                append(':')
                append(ref.chapter)
                if (ref.from != 0) {
                    append(':')
                    append(ref.from)
                    append('-')
                    append(ref.to)
                }
                append("<br /><br /></b></font>")
                val lines = renderReadingLines(ref)
                readingsLog("render body for ${ref.book} ${ref.chapter}:${ref.from}-${ref.to}, chars=${lines.length}")
                append(lines)
                append("<br /><br />")
            }
            append(outroText)
        }
        return if (rendered.isBlank()) {
            readingsLog("rendered html is blank, returning result_not_found")
            getString("result_not_found")
        } else {
            readingsLog("loadTodayHtml success, html chars=${rendered.length}")
            rendered
        }
    }

    private suspend fun loadReadingsFromNetwork(dateCompact: String, datePadded: String): List<ReadingRef>? {
        readingsLog("network request start, url=$READINGS_URL")
        val raw = runCatching { httpClient.get(READINGS_URL).bodyAsText() }
            .onFailure { readingsLog("network request failed: ${it.message}") }
            .getOrNull() ?: return null
        readingsLog("network response received, chars=${raw.length}")

        val list = runCatching { json.decodeFromString<List<ReadingEntryDto>>(raw) }
            .onFailure { readingsLog("network json parse failed: ${it.message}") }
            .getOrNull() ?: return null
        readingsLog("network parsed entries count=${list.size}")

        val entry = list.firstOrNull { it.date == dateCompact || it.date == datePadded }
        if (entry == null) {
            readingsLog("network has no entry for dateCompact=$dateCompact or datePadded=$datePadded")
            return null
        }

        val refs = entry.reading.map {
            ReadingRef(it.book.trim(), it.chapter.toIntSafe(), it.from.toIntSafe(), it.to.toIntSafe())
        }
        readingsLog("network matched entry date=${entry.date}, refs count=${refs.size}")
        return refs
    }

    private suspend fun loadReadingsFromLocalXml(dateCompact: String, datePadded: String): List<ReadingRef>? {
        val cached = localReadingsByDate
        val byDate = if (cached != null) cached else {
            readingsLog("local xml cache miss, reading $READINGS_XML_PATH")
            val xml = Res.readBytes(READINGS_XML_PATH).decodeToString()
            readingsLog("local readings xml loaded, chars=${xml.length}")
            val parsed = parseReadingsXml(xml)
            localReadingsByDate = parsed
            readingsLog("local readings xml parsed, dates=${parsed.size}")
            parsed
        }
        if (cached != null) {
            readingsLog("local xml cache hit, dates=${cached.size}")
        }
        val refs = byDate[dateCompact]
            ?: byDate[datePadded]
            ?: findByDayMonth(byDate, dateCompact)
            ?: findByDayMonth(byDate, datePadded)
        readingsLog("local xml lookup result count=${refs?.size ?: 0} for dateCompact=$dateCompact, datePadded=$datePadded")
        return refs
    }

    private fun findByDayMonth(
        byDate: Map<String, List<ReadingRef>>,
        dateValue: String,
    ): List<ReadingRef>? {
        val (targetDay, targetMonth) = parseDayMonth(dateValue) ?: return null
        return byDate.entries.firstOrNull { entry ->
            val (day, month) = parseDayMonth(entry.key) ?: return@firstOrNull false
            day == targetDay && month == targetMonth
        }?.value
    }

    private fun parseDayMonth(value: String): Pair<Int, Int>? {
        val parts = value.split('.')
        if (parts.size < 2) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        return day to month
    }

    private suspend fun renderReadingLines(ref: ReadingRef): String {
        val books = booksByName ?: run {
            readingsLog("books cache miss, reading $BOOKS_XML_PATH")
            val xml = Res.readBytes(BOOKS_XML_PATH).decodeToString()
            readingsLog("books xml loaded, chars=${xml.length}")
            val parsed = parseBooks(xml)
            booksByName = parsed
            readingsLog("books xml parsed, books=${parsed.size}")
            parsed
        }
        if (booksByName != null) {
            readingsLog("books cache available, books=${books.size}")
        }
        val bookBody = books[ref.book]
        if (bookBody == null) {
            readingsLog("book not found in xml: ${ref.book}")
            return ""
        }
        val chapterBody = extractChapterBody(bookBody, ref.chapter)
        if (chapterBody == null) {
            readingsLog("chapter not found in book=${ref.book}, chapter=${ref.chapter}")
            return ""
        }
        val lineRegex = Regex("""<LINE\b[^>]*\bnumber="(\d+)"[^>]*>([\s\S]*?)</LINE>""")
        var totalLines = 0
        var matchedLines = 0
        val result = buildString {
            lineRegex.findAll(chapterBody).forEach { match ->
                totalLines += 1
                val verse = match.groupValues[1].toIntSafe()
                if (verse in ref.from..ref.to) {
                    matchedLines += 1
                    val verseText = decodeXmlEntities(match.groupValues[2]).trim().replace(Regex("""\s+"""), " ")
                    append("<sup><font color=\"#aa2c2c\">")
                    append(verse)
                    append("</font></sup> ")
                    append(verseText)
                    append("<br />")
                }
            }
        }
        readingsLog(
            "chapter processed book=${ref.book}, chapter=${ref.chapter}, linesTotal=$totalLines, linesMatched=$matchedLines, resultChars=${result.length}"
        )
        return result
    }

    private fun parseBooks(xml: String): Map<String, String> {
        val map = linkedMapOf<String, String>()
        val bookRegex = Regex("""<BOOK\b([^>]*)>([\s\S]*?)</BOOK>""")
        val nameRegex = Regex("\\bname=\"([^\"]+)\"")
        bookRegex.findAll(xml).forEach { match ->
            val attrs = match.groupValues[1]
            val body = match.groupValues[2]
            val name = nameRegex.find(attrs)?.groupValues?.get(1)?.trim().orEmpty()
            if (name.isNotBlank()) {
                map[name] = body
            }
        }
        return map
    }

    private fun parseReadingsXml(xml: String): Map<String, List<ReadingRef>> {
        val dateRegex = Regex("""<date\b[^>]*\bdate="([^"]+)"[^>]*>([\s\S]*?)</date>""")
        val readingRegex = Regex(
            """<reading\b[^>]*\bbook="([^"]+)"[^>]*\bchapter="([^"]+)"[^>]*\bfrom="([^"]+)"[^>]*\bto="([^"]+)"[^>]*>\s*</reading>"""
        )
        val out = linkedMapOf<String, List<ReadingRef>>()
        dateRegex.findAll(xml).forEach { dateMatch ->
            val date = dateMatch.groupValues[1].trim()
            val body = dateMatch.groupValues[2]
            val values = readingRegex.findAll(body).map { readingMatch ->
                ReadingRef(
                    book = readingMatch.groupValues[1].trim(),
                    chapter = readingMatch.groupValues[2].toIntSafe(),
                    from = readingMatch.groupValues[3].toIntSafe(),
                    to = readingMatch.groupValues[4].toIntSafe(),
                )
            }.toList()
            if (values.isNotEmpty()) {
                out[date] = values
            }
        }
        return out
    }

    private fun extractChapterBody(bookBody: String, chapter: Int): String? {
        val escaped = Regex.escape(chapter.toString())
        val chapterRegex = Regex("""<CHAPTER\b[^>]*\bnumber="$escaped"[^>]*>([\s\S]*?)</CHAPTER>""")
        return chapterRegex.find(bookBody)?.groupValues?.get(1)
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

    private fun String.toIntSafe(): Int = trim().toIntOrNull() ?: 0

    private suspend fun loadReadingsFromCache(storage: AppStorage, datePadded: String): CacheState {
        val key = cacheKey(datePadded)
        val raw = storage.stringFlow(key, "").first()
        if (raw.isBlank()) {
            readingsLog("cache miss for key=$key")
            return CacheState.Miss
        }
        if (raw == CACHE_NOT_FOUND) {
            readingsLog("cache contains not-found marker for key=$key")
            return CacheState.NotFound
        }
        val decoded = runCatching { json.decodeFromString<List<ReadingRef>>(raw) }
            .onFailure { readingsLog("cache decode failed for key=$key: ${it.message}") }
            .getOrNull()
        if (decoded.isNullOrEmpty()) {
            readingsLog("cache invalid/empty for key=$key")
            return CacheState.Miss
        }
        return CacheState.Found(decoded)
    }

    private suspend fun saveReadingsToCache(storage: AppStorage, datePadded: String, readings: List<ReadingRef>) {
        val key = cacheKey(datePadded)
        val encoded = json.encodeToString(readings)
        storage.setString(key, encoded)
        readingsLog("cache saved for key=$key, refs=${readings.size}, chars=${encoded.length}")
    }

    private suspend fun saveNotFoundToCache(storage: AppStorage, datePadded: String) {
        val key = cacheKey(datePadded)
        storage.setString(key, CACHE_NOT_FOUND)
        readingsLog("cache saved not-found marker for key=$key")
    }

    private fun cacheKey(datePadded: String): String = "$CACHE_KEY_PREFIX$datePadded"

    private fun readingsLog(message: String) {
        println("readings: $message")
    }

    private fun parseNameDays(xml: String): Map<String, String> {
        val dateRegex = Regex("""<date\b([^>]*)/>""", setOf(RegexOption.IGNORE_CASE))
        val attrRegex = Regex("""(\w+)="([^"]*)"""")
        val out = linkedMapOf<String, String>()
        dateRegex.findAll(xml).forEach { match ->
            val attrs = match.groupValues[1]
            val attrMap = attrRegex.findAll(attrs).associate { it.groupValues[1] to it.groupValues[2] }
            val date = attrMap["date"]?.trim().orEmpty()
            if (date.isBlank()) return@forEach
            val names = attrMap.entries
                .filter { it.key.startsWith("name") }
                .sortedBy { it.key.removePrefix("name").toIntOrNull() ?: Int.MAX_VALUE }
                .map { it.value.trim() }
                .filter { it.isNotBlank() }
            if (names.isNotEmpty()) {
                out[date] = names.joinToString(", ")
            }
        }
        return out
    }

    private fun extractCalendarCelebrating(html: String): String? {
        val blockRegex = Regex("""(?is)<div\s+class\s*=\s*["']?dd_text["']?\s*>(.*?)</div>""")
        val raw = blockRegex.find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (raw.isBlank()) return null
        return raw
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), ", ")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun extractCalendarFast(html: String): String? {
        val blockRegex = Regex("""(?is)<span\s+class\s*=\s*["']?dd_tptxt["']?\s*>(.*?)</span>""")
        val raw = blockRegex.find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (raw.isBlank()) return null
        return raw
            .replace(Regex("""<[^>]+>"""), " ")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun extractCalendarHymns(html: String): List<CalendarHymn> {
        val blockRegex = Regex(
            """(?is)<div\s+class=trop><div\s+class=trop_title>(.*?)</div>(?:\s*<div\s+class=trop_glas>(.*?)</div>)?\s*<div\s+class=trop_text>(.*?)(?:</div>|<br>)"""
        )
        return blockRegex.findAll(html).mapNotNull { match ->
            val title = decodeSimpleHtml(match.groupValues[1])
            val glas = decodeSimpleHtml(match.groupValues.getOrElse(2) { "" }).takeIf { it.isNotBlank() }
            val textRaw = match.groupValues.getOrElse(3) { "" }
            val text = decodeSimpleHtml(
                textRaw
                    .replace("<p>", "")
                    .replace("</p>", "")
                    .replace("//", "/")
                    .replace("/", "<br />")
            )
            if (title.isBlank() || text.isBlank()) null else CalendarHymn(title = title, glas = glas, text = text)
        }.toList()
    }

    private fun decodeSimpleHtml(value: String): String {
        return value
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }
}

@Serializable
private data class ReadingEntryDto(
    @SerialName("date") val date: String,
    @SerialName("reading") val reading: List<ReadingDto>,
)

@Serializable
private data class ReadingDto(
    @SerialName("book") val book: String,
    @SerialName("chapter") val chapter: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
)

@Serializable
private data class ReadingRef(
    val book: String,
    val chapter: Int,
    val from: Int,
    val to: Int,
)

private sealed class CacheState {
    data class Found(val readings: List<ReadingRef>) : CacheState()
    data object NotFound : CacheState()
    data object Miss : CacheState()
}
