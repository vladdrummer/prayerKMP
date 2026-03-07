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
        val readBytes = Res.readBytes("files/tableofcontents.json")
        val jsonString = readBytes.decodeToString()
        Json.decodeFromString(jsonString)
    }
}
