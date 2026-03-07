package com.vladdrummer.prayerkmp.feature.psalter

import com.vladdrummer.prayerkmp.feature.bible.BibleRepository
import com.vladdrummer.prayerkmp.feature.strings.getString
import kotlinproject.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class PsalterMode(val id: String) {
    Usual("usual"),
    OverDead("dead"),
    OverHealth("health");

    companion object {
        fun fromId(id: String): PsalterMode = entries.firstOrNull { it.id == id } ?: Usual
    }
}

data class PsalterReaderPage(
    val title: String,
    val html: String,
    val isAfterKathismaPrayer: Boolean = false,
)

object PsalterRepository {
    val kathismas: List<Int> = (1..20).toList()
    fun getPsalmsForKathisma(kathisma: Int): List<Int> = kathismaPsalmRanges[kathisma]?.toList().orEmpty()

    suspend fun buildKathismaPages(kathisma: Int, mode: PsalterMode): List<PsalterReaderPage> {
        return withContext(Dispatchers.Default) {
            val psalms = kathismaPsalmRanges[kathisma] ?: return@withContext emptyList()
            val poKaphismam = LegacyPsalterData.loadPoKaphismam()
            if (poKaphismam.size < 20) return@withContext emptyList()

            val slava = when (mode) {
                PsalterMode.Usual -> getString("slavaPsalter")
                PsalterMode.OverDead -> buildSlavaForDead()
                PsalterMode.OverHealth -> getString("slavaPsalter") + "<i><font color=\"#aa2c2c\">Поминовение о здравии</font></i><br /><br />"
            }
            val kaphForDead = if (mode == PsalterMode.OverDead) {
                val v = getString("kaphForDead")
                if (v == "kaphForDead") "<i><font color=\"#aa2c2c\">Тропари (по усопшему)</font></i><br /><br />" else v
            } else {
                ""
            }

            buildList {
                psalms.forEach { psalm ->
                    if (psalm == 118) {
                        val chapterHtml = BibleRepository.getChapterHtml("Псалтирь", 118).orEmpty()
                        add(PsalterReaderPage("Псалом 118", sliceVerses(chapterHtml, 1..71)))
                        add(PsalterReaderPage("Слава", slava))
                        add(PsalterReaderPage("Псалом 118 (часть 2)", sliceVerses(chapterHtml, 72..131)))
                        add(PsalterReaderPage("Слава", slava))
                        add(PsalterReaderPage("Псалом 118 (часть 3)", sliceVerses(chapterHtml, 132..176)))
                    } else {
                        add(
                            PsalterReaderPage(
                                title = "Псалом $psalm",
                                html = BibleRepository.getChapterHtml("Псалтирь", psalm).orEmpty()
                            )
                        )
                    }

                    if (psalm in slavaAfter) {
                        add(PsalterReaderPage("Слава", slava))
                    }
                }

                val kaphIndex = kathisma - 1
                if (kaphIndex in poKaphismam.indices) {
                    add(
                        PsalterReaderPage(
                            title = "По прочтению кафизмы $kathisma",
                            html = poKaphismam[kaphIndex] + kaphForDead,
                            isAfterKathismaPrayer = true
                        )
                    )
                }
            }
        }
    }

    private suspend fun buildSlavaForDead(): String {
        val slavaBase = getString("slavaPsalter")
        val part1Raw = getString("slavaForDead1")
        val part2Raw = getString("slavaForDead2")
        val part1 = if (part1Raw == "slavaForDead1") "" else part1Raw
        val part2 = if (part2Raw == "slavaForDead2") "" else part2Raw
        if (part1.isBlank() || part2.isBlank()) return slavaBase
        return buildString {
            append(slavaBase)
            append(part1)
            append(" (Имя его), ")
            append(part2)
        }
    }

    private fun sliceVerses(html: String, range: IntRange): String {
        val lineRegex = Regex("""<sup><small><font color="#aa2c2c">(\d+)</font></small></sup>\s*([\s\S]*?)<br\s*/>""")
        val sliced = lineRegex.findAll(html)
            .filter { match ->
                val verse = match.groupValues[1].toIntOrNull() ?: return@filter false
                verse in range
            }
            .joinToString(separator = "") { it.value }
        return if (sliced.isNotBlank()) sliced else html
    }

    private object LegacyPsalterData {
        private const val PSALTER_XML_PATH = "files/psalter.xml"
        private val mutex = Mutex()
        private var cachedPoKaphismam: List<String>? = null

        suspend fun loadPoKaphismam(): List<String> {
            cachedPoKaphismam?.let { return it }
            return mutex.withLock {
                cachedPoKaphismam?.let { return@withLock it }
                val xml = Res.readBytes(PSALTER_XML_PATH).decodeToString()
                val block = Regex("""<string-array\s+name="poKaphismam"[^>]*>([\s\S]*?)</string-array>""")
                    .find(xml)
                    ?.groupValues
                    ?.get(1)
                    .orEmpty()
                val items = Regex("""<item\b[^>]*>([\s\S]*?)</item>""")
                    .findAll(block)
                    .map { match ->
                        val raw = match.groupValues[1].trim()
                        val cdata = Regex("""^<!\[CDATA\[([\s\S]*)]]>$""")
                            .find(raw)
                            ?.groupValues
                            ?.get(1)
                        decodeEntities(cdata ?: raw)
                    }
                    .toList()
                cachedPoKaphismam = items
                items
            }
        }

        private fun decodeEntities(value: String): String {
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

    private val slavaAfter = setOf(
        3, 6, 8, 10, 13, 16, 17, 20, 23, 26, 29, 31, 33, 35, 36, 39, 42, 45, 48, 50,
        54, 57, 60, 63, 66, 67, 69, 71, 73, 76, 77, 80, 84, 87, 88, 90, 93, 96, 100, 102,
        103, 104, 105, 106, 108, 111, 114, 117, 118, 123, 128, 133, 136, 139, 142, 144, 147, 150
    )

    private val kathismaPsalmRanges: Map<Int, IntRange> = mapOf(
        1 to (1..8),
        2 to (9..16),
        3 to (17..23),
        4 to (24..31),
        5 to (32..36),
        6 to (37..45),
        7 to (46..54),
        8 to (55..63),
        9 to (64..69),
        10 to (70..76),
        11 to (77..84),
        12 to (85..90),
        13 to (91..100),
        14 to (101..104),
        15 to (105..108),
        16 to (109..117),
        17 to (118..118),
        18 to (119..133),
        19 to (134..142),
        20 to (143..150),
    )
}
