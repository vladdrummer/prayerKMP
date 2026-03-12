package com.vladdrummer.prayerkmp.feature.storage

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

interface AppStorage {
    fun booleanFlow(key: String, default: Boolean = false): Flow<Boolean>
    suspend fun setBoolean(key: String, value: Boolean)

    fun stringFlow(key: String, default: String = ""): Flow<String>
    suspend fun setString(key: String, value: String)
}

object AppStorageKeys {
    const val DarkTheme = "dark_theme"
    const val NameImenit = "name_imenit"
    const val Duhovnik = "duhovnik"
    const val MyGenderMale = "myGender"
    const val PersonalParents = "personal_parents"
    const val PersonalRelatives = "personal_relatives"
    const val PersonalChildren = "personal_children"
    const val PersonalBenefactors = "personal_benefactors"
    const val PersonalDead = "personal_dead"
    const val PersonalGodChildren = "personal_god_children"
    const val AdditionalMorningPrayers = "additional_morning_prayers"
    const val AdditionalEveningPrayers = "additional_evening_prayers"
    const val MorningRuleEnabled = "morning_rule_enabled"
    const val EveningRuleEnabled = "evening_rule_enabled"
    const val FavoritePrayers = "favorite_prayers"
    const val SavedPrayerResId = "saved_prayer_res_id"
    const val SavedPrayerScroll = "saved_prayer_scroll"
    const val SavedPrayerScrollMap = "saved_prayer_scroll_map"
    const val BibleLastBook = "bible_last_book"
    const val BibleLastChapter = "bible_last_chapter"
    const val BibleFontSizeSp = "bible_font_size_sp"
    const val BibleFontIndex = "bible_font_index"
    const val PsalterLastMode = "psalter_last_mode"
    const val PsalterLastKathisma = "psalter_last_kathisma"
    const val PsalterLastPage = "psalter_last_page"
    const val PsalterSelectedDeadNames = "psalter_selected_dead_names"
    const val PsalterSelectedHealthNames = "psalter_selected_health_names"
    const val GoogleAccountEmail = "google_account_email"
    const val MessageBoardVotedIds = "message_board_voted_ids"
    const val PremiumSubscriptionActive = "premium_subscription_active"
    const val PremiumSubscriptionEmail = "premium_subscription_email"
    const val PremiumSubscriptionSource = "premium_subscription_source"
    const val PremiumSubscriptionCheckedAt = "premium_subscription_checked_at"
}

@Composable
expect fun rememberAppStorage(): AppStorage
