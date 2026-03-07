package com.vladdrummer.prayerkmp.feature.arrays

import kotlinproject.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object PrayerArraysRepository {
    private const val ARRAYS_XML_PATH = "files/arrays.xml"

    private val initMutex = Mutex()
    private var isInitialized = false
    private var arraysByName: Map<String, List<String>> = emptyMap()

    suspend fun init(forceReload: Boolean = false) {
        if (isInitialized && !forceReload) return

        initMutex.withLock {
            if (isInitialized && !forceReload) return
            arraysByName = withContext(Dispatchers.Default) {
                val rawBytes = Res.readBytes(ARRAYS_XML_PATH)
                println("app-init: arrays bytes=${rawBytes.size}, head=${rawBytes.hexHead()}")
                val xmlText = rawBytes.decodeToString()
                println("app-init: arrays text prefix='${xmlText.preview()}'")
                runCatching { parseAndroidStringArraysXml(xmlText) }
                    .onFailure {
                        println("app-init: arrays parse failed, error=$it")
                    }
                    .getOrElse { error ->
                        throw IllegalStateException(
                            "Failed to parse $ARRAYS_XML_PATH, prefix='${xmlText.preview()}'",
                            error
                        )
                    }
            }
            isInitialized = true
        }
    }

    suspend fun getArray(name: String): List<String> {
        init()
        return arraysByName[name].orEmpty()
    }
}

internal fun parseAndroidStringArraysXml(xmlText: String): Map<String, List<String>> {
    val arrayRegex = Regex("""<string-array\s+name="([^"]+)"[^>]*>([\s\S]*?)</string-array>""")
    val itemRegex = Regex("""<item\b[^>]*>([\s\S]*?)</item>""")
    val out = linkedMapOf<String, List<String>>()

    arrayRegex.findAll(xmlText).forEach { arrayMatch ->
        val name = arrayMatch.groupValues[1]
        val block = arrayMatch.groupValues[2]
        val values = itemRegex.findAll(block).map { itemMatch ->
            val raw = itemMatch.groupValues[1].trim()
            val value = Regex("""^<!\[CDATA\[([\s\S]*)]]>$""").find(raw)?.groupValues?.get(1) ?: raw
            decodeXmlEntities(value)
        }.toList()
        out[name] = values
    }

    return out
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
