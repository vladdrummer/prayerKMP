package com.vladdrummer.prayerkmp.feature.tableofcontents

import kotlinproject.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TableOfContents (
    val name: String,
    val item: List<PrayerData>
)
@Serializable
data class PrayerData(
    val resid    : String? = null,
    val addable  : Boolean = false,
    val chsResId : String? = null,
    val name    : String? = null
)

suspend fun loadTableOfContents(): List<TableOfContents> {
    return withContext(Dispatchers.Default) {
        val path = "files/tableofcontents.json"
        val rawBytes = Res.readBytes(path)
        println("app-init: toc bytes=${rawBytes.size}, head=${rawBytes.hexHead()}")
        val jsonString = rawBytes.decodeToString()
        println("app-init: toc text prefix='${jsonString.preview()}'")
        runCatching { Json.decodeFromString<List<TableOfContents>>(jsonString) }
            .onFailure {
                println("app-init: toc parse failed, error=$it")
            }
            .getOrElse { error ->
                throw IllegalStateException(
                    "Failed to parse $path, prefix='${jsonString.preview()}'",
                    error
                )
            }
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
