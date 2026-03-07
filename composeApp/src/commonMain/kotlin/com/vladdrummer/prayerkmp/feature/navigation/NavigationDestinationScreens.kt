package com.vladdrummer.prayerkmp.feature.navigation

import kotlinx.serialization.Serializable

@Serializable
object MainMenu

@Serializable
object PersonalData

@Serializable
object RuleEdit

@Serializable
object Favorites

@Serializable
object Bible

@Serializable
object Psalter

@Serializable
data class BiblePrayerBridge(
    val book: String,
    val chapter: Int,
    val title: String = "Молитва перед чтением"
)

@Serializable
data class BibleReader(
    val book: String,
    val chapter: Int,
)

@Serializable
data class PsalterBeforePrayer(
    val mode: String,
    val kathisma: Int,
    val startPsalm: Int? = null,
    val startPage: Int? = null,
    val title: String = "Перед началом чтения Псалтири",
)

@Serializable
data class PsalterReader(
    val mode: String,
    val kathisma: Int,
    val startPsalm: Int? = null,
    val startPage: Int? = null,
    val title: String = "Псалтирь",
)

@Serializable
data class PsalterAfterPrayer(
    val mode: String,
    val kathisma: Int,
    val title: String = "По окончании чтения Псалтири",
)

@Serializable
data class GospelReadings(
    val title: String
)

@Serializable
data class PrayerScreen(
    val resId: String,
    val title: String,
    val addable: Boolean = false,
)

@Serializable
data class PrayerListScreen(
    val type: PrayerListScreenType,
    val title: String
) {
    @Serializable
    enum class PrayerListScreenType {
        AllPrayer, CannonAcathists, Saints
    }
}
