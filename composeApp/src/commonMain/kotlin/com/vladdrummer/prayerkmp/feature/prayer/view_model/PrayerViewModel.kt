package com.vladdrummer.prayerkmp.feature.prayer.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.prayer.PrayerTextBuilder
import com.vladdrummer.prayerkmp.feature.prayer.currentLocalDate
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import com.vladdrummer.prayerkmp.feature.tableofcontents.TableOfContentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class PrayerViewModel(
    private val resId: String,
    private val title: String,
    private val addable: Boolean,
    private val storage: AppStorage,
    private val textBuilder: PrayerTextBuilder,
) : ViewModel() {
    private companion object {
        private const val CHURCH_SLAVONIC_FONT_INDEX = 2
        private const val FONT_COUNT = 5
        private const val DEFAULT_FONT_INDEX = 0
        private const val DEFAULT_FONT_SIZE_SP = 20
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val viewStateFlow = MutableStateFlow(
        PrayerViewState(
            title = title,
            resId = resId,
            addable = addable,
            isLoading = true
        )
    )
    val viewState: StateFlow<PrayerViewState> = viewStateFlow.asStateFlow()

    init {
        load()
        refreshAdditionalFlags()
    }

    fun load(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                viewStateFlow.value = viewStateFlow.value.copy(isLoading = true)
            }
            val built = withContext(Dispatchers.Default) {
                val effectiveResId = resolveResIdForCurrentFont()
                textBuilder.build(
                    resId = effectiveResId,
                    date = currentLocalDate(),
                    fontIndex = viewStateFlow.value.fontIndex,
                )
            }
            val savedScroll = loadSavedScrollPosition()
            viewStateFlow.value = viewStateFlow.value.copy(
                text = built.text,
                period = built.period,
                initialScrollPosition = savedScroll,
                isLoading = false
            )
        }
    }

    fun increaseFontSize() {
        val size = (viewStateFlow.value.fontSizeSp + 1).coerceAtMost(50)
        viewStateFlow.value = viewStateFlow.value.copy(fontSizeSp = size)
    }

    fun decreaseFontSize() {
        val size = (viewStateFlow.value.fontSizeSp - 1).coerceAtLeast(12)
        viewStateFlow.value = viewStateFlow.value.copy(fontSizeSp = size)
    }

    fun switchFont() {
        val next = (viewStateFlow.value.fontIndex + 1) % FONT_COUNT
        viewStateFlow.value = viewStateFlow.value.copy(fontIndex = next)
        load(showLoading = false)
    }

    fun resetFontDefaults() {
        val previous = viewStateFlow.value
        viewStateFlow.value = previous.copy(
            fontSizeSp = DEFAULT_FONT_SIZE_SP,
            fontIndex = DEFAULT_FONT_INDEX
        )
        if (previous.fontIndex != DEFAULT_FONT_INDEX) {
            load(showLoading = false)
        }
    }

    fun toggleMorning() {
        if (!addable) return
        viewModelScope.launch {
            val current = loadIdList(AppStorageKeys.AdditionalMorningPrayers).toMutableList()
            if (current.contains(resId)) current.remove(resId) else current.add(resId)
            saveIdList(AppStorageKeys.AdditionalMorningPrayers, current)
            refreshAdditionalFlags()
        }
    }

    fun toggleEvening() {
        if (!addable) return
        viewModelScope.launch {
            val current = loadIdList(AppStorageKeys.AdditionalEveningPrayers).toMutableList()
            if (current.contains(resId)) current.remove(resId) else current.add(resId)
            saveIdList(AppStorageKeys.AdditionalEveningPrayers, current)
            refreshAdditionalFlags()
        }
    }

    private fun refreshAdditionalFlags() {
        viewModelScope.launch {
            val morning = loadIdList(AppStorageKeys.AdditionalMorningPrayers)
            val evening = loadIdList(AppStorageKeys.AdditionalEveningPrayers)
            viewStateFlow.value = viewStateFlow.value.copy(
                isAddedToMorning = morning.contains(resId),
                isAddedToEvening = evening.contains(resId)
            )
        }
    }

    private suspend fun loadIdList(key: String): List<String> {
        val raw = storage.stringFlow(key, "").first()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private suspend fun saveIdList(key: String, values: List<String>) {
        storage.setString(key, json.encodeToString(ListSerializer(String.serializer()), values))
    }

    private suspend fun resolveResIdForCurrentFont(): String {
        if (viewStateFlow.value.fontIndex != CHURCH_SLAVONIC_FONT_INDEX) return resId

        runCatching { TableOfContentsRepository.init() }
        val chsResId = TableOfContentsRepository.state.value
            .asSequence()
            .flatMap { it.item.asSequence() }
            .firstOrNull { it.resid == resId }
            ?.chsResId
            ?.trim()
            .orEmpty()

        return if (chsResId.isNotBlank()) chsResId else resId
    }

    private suspend fun loadSavedScrollPosition(): Int {
        val mapRaw = storage.stringFlow(AppStorageKeys.SavedPrayerScrollMap, "").first()
        if (mapRaw.isBlank()) return 0
        val map = runCatching {
            json.decodeFromString(MapSerializer(String.serializer(), Int.serializer()), mapRaw)
        }.getOrDefault(emptyMap())
        return map[resId]?.coerceAtLeast(0) ?: 0
    }
}
