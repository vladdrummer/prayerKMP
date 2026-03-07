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
                val xmlText = Res.readBytes(ARRAYS_XML_PATH).decodeToString()
                parseAndroidStringArraysXml(xmlText)
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
        code.toChar().toString()
    }

    val hexEntityRegex = Regex("""&#x([0-9a-fA-F]+);""")
    decoded = hexEntityRegex.replace(decoded) { match ->
        val code = match.groupValues[1].toIntOrNull(16) ?: return@replace match.value
        code.toChar().toString()
    }

    return decoded
}
