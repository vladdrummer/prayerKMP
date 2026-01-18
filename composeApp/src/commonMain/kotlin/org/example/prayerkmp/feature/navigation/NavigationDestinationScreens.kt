package org.example.prayerkmp.feature.navigation

import kotlinx.serialization.Serializable

@Serializable
object MainMenu

@Serializable
data class PrayerListScreen(
    val type: PrayerListScreenType
) {
    @Serializable
    enum class PrayerListScreenType {
        AllPrayer, CannonAcathists, Saints
    }
}