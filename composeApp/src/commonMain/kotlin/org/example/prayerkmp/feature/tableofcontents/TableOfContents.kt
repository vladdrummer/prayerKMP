package org.example.prayerkmp.feature.tableofcontents

import kotlinproject.composeapp.generated.resources.Res
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
    val readBytes = Res.readBytes("files/tableofcontents.json")
    val jsonString = readBytes.decodeToString()
    return Json.decodeFromString(jsonString)
}