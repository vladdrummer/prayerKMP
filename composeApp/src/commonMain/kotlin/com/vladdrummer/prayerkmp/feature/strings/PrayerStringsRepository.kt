package com.vladdrummer.prayerkmp.feature.strings

import kotlinproject.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object PrayerStringsRepository {
    private const val STRINGS_XML_PATH = "files/strings.xml"

    private val initMutex = Mutex()
    private var isInitialized = false
    private var valuesById: Map<String, String> = emptyMap()

    suspend fun init(forceReload: Boolean = false) {
        if (isInitialized && !forceReload) return

        initMutex.withLock {
            if (isInitialized && !forceReload) return
            valuesById = withContext(Dispatchers.Default) {
                val rawBytes = Res.readBytes(STRINGS_XML_PATH)
                println("app-init: strings bytes=${rawBytes.size}, head=${rawBytes.hexHead()}")
                val xmlText = rawBytes.decodeToString()
                println("app-init: strings text prefix='${xmlText.preview()}'")
                runCatching { parseAndroidStringsXml(xmlText) }
                    .onFailure {
                        println("app-init: strings parse failed, error=$it")
                    }
                    .getOrElse { error ->
                        throw IllegalStateException(
                            "Failed to parse $STRINGS_XML_PATH, prefix='${xmlText.preview()}'",
                            error
                        )
                    }
            }
            isInitialized = true
        }
    }

    fun getString(resId: String): String = valuesById[resId] ?: resId

    suspend fun getStringAsync(resId: String): String {
        init()
        return getString(resId)
    }

    fun size(): Int = valuesById.size
}

suspend fun getString(resId: String): String = PrayerStringsRepository.getStringAsync(resId)

internal fun parseAndroidStringsXml(xmlText: String): Map<String, String> {
    val stringRegex = Regex("""<string\b[^>]*\bname\s*=\s*"([^"]+)"[^>]*>([\s\S]*?)</string>""")
    val map = linkedMapOf<String, String>()

    stringRegex.findAll(xmlText).forEach { match ->
        val key = match.groupValues[1]
        val rawValue = match.groupValues[2]
        map[key] = extractStringValue(rawValue)
    }

    return map
}

private fun extractStringValue(rawValue: String): String {
    val trimmed = rawValue.trim()
    val cdataMatch = Regex("""^<!\[CDATA\[([\s\S]*)]]>$""").find(trimmed)
    val value = cdataMatch?.groupValues?.get(1) ?: trimmed
    return decodeXmlEntities(value)
}

private fun decodeXmlEntities(value: String): String {
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

private fun ByteArray.hexHead(limit: Int = 8): String {
    return take(limit).joinToString(" ") { byte ->
        val unsigned = byte.toInt() and 0xFF
        unsigned.toString(16).padStart(2, '0')
    }
}

private fun String.preview(limit: Int = 80): String {
    return replace("\n", "\\n").take(limit)
}
