package com.vladdrummer.prayerkmp.feature.cloud

import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CloudSyncInteractor(
    private val storage: AppStorage,
    private val repository: CloudRepository = CloudRepository(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveToCloud(email: String): String {
        val payload = CloudPayload(
            stringValues = buildMap {
                STRING_KEYS.forEach { key ->
                    put(key, storage.stringFlow(key, "").first())
                }
            },
            booleanValues = buildMap {
                BOOLEAN_KEYS.forEach { key ->
                    put(key, storage.booleanFlow(key, false).first())
                }
            }
        )
        val stateJson = json.encodeToString(payload)
        repository.save(email = email, stateJson = stateJson)
        return "Сохранено в облако"
    }

    suspend fun restoreFromCloud(email: String): String {
        val response = repository.load(email = email)
        val payload = runCatching {
            json.decodeFromString(CloudPayload.serializer(), response.stateJson)
        }.getOrElse {
            throw IllegalStateException("Данные в облаке повреждены")
        }

        payload.stringValues.forEach { (key, value) ->
            if (key in STRING_KEYS) storage.setString(key, value)
        }
        payload.booleanValues.forEach { (key, value) ->
            if (key in BOOLEAN_KEYS) storage.setBoolean(key, value)
        }
        return "Восстановлено из облака"
    }
}

@Serializable
private data class CloudPayload(
    val stringValues: Map<String, String> = emptyMap(),
    val booleanValues: Map<String, Boolean> = emptyMap(),
)

private val STRING_KEYS = setOf(
    AppStorageKeys.NameImenit,
    AppStorageKeys.Duhovnik,
    AppStorageKeys.PersonalParents,
    AppStorageKeys.PersonalRelatives,
    AppStorageKeys.PersonalChildren,
    AppStorageKeys.PersonalBenefactors,
    AppStorageKeys.PersonalDead,
    AppStorageKeys.PersonalGodChildren,
    AppStorageKeys.AdditionalMorningPrayers,
    AppStorageKeys.AdditionalEveningPrayers,
    AppStorageKeys.MorningRuleEnabled,
    AppStorageKeys.EveningRuleEnabled,
    AppStorageKeys.FavoritePrayers,
    AppStorageKeys.SavedPrayerResId,
    AppStorageKeys.SavedPrayerScroll,
    AppStorageKeys.SavedPrayerScrollMap,
    AppStorageKeys.BibleLastBook,
    AppStorageKeys.BibleLastChapter,
    AppStorageKeys.BibleFontSizeSp,
    AppStorageKeys.BibleFontIndex,
    AppStorageKeys.PsalterLastMode,
    AppStorageKeys.PsalterLastKathisma,
    AppStorageKeys.PsalterLastPage,
    AppStorageKeys.PsalterSelectedDeadNames,
    AppStorageKeys.PsalterSelectedHealthNames,
)

private val BOOLEAN_KEYS = setOf(
    AppStorageKeys.DarkTheme,
    AppStorageKeys.MyGenderMale,
)

