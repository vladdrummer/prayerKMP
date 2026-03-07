package com.vladdrummer.prayerkmp.feature.ruleedit.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.arrays.PrayerArraysRepository
import com.vladdrummer.prayerkmp.feature.prayer.PrayerTextBuilder
import com.vladdrummer.prayerkmp.feature.prayer.currentLocalDate
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import com.vladdrummer.prayerkmp.feature.tableofcontents.TableOfContentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class RuleEditViewModel(
    private val storage: AppStorage,
    private val textBuilder: PrayerTextBuilder,
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }
    private val viewStateFlow = MutableStateFlow(RuleEditViewState())
    val viewState: StateFlow<RuleEditViewState> = viewStateFlow.asStateFlow()
    private var titleByResIdCache: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch {
            if (titleByResIdCache.isEmpty()) {
                titleByResIdCache = TableOfContentsRepository.state.value
                    .asSequence()
                    .flatMap { it.item.asSequence() }
                    .filter { !it.resid.isNullOrBlank() }
                    .associate { it.resid.orEmpty() to (it.name ?: it.resid.orEmpty()) }
            }
            loadRule(RuleType.Morning)
        }
    }

    fun selectRule(type: RuleType) {
        if (viewStateFlow.value.selectedRule == type) return
        viewModelScope.launch { loadRule(type) }
    }

    fun setPartEnabled(index: Int, enabled: Boolean) {
        val current = viewStateFlow.value
        if (index !in current.parts.indices) return
        val updatedParts = current.parts.toMutableList().apply {
            this[index] = this[index].copy(enabled = enabled)
        }
        viewStateFlow.value = current.copy(parts = updatedParts)

        viewModelScope.launch {
            saveEnabledFlags(current.selectedRule, updatedParts.map { it.enabled })
        }
    }

    fun removeAdditionalPrayer(resId: String) {
        val currentRule = viewStateFlow.value.selectedRule
        viewModelScope.launch {
            val key = additionalKey(currentRule)
            val ids = loadStringList(key).toMutableList()
            val idx = ids.indexOf(resId)
            if (idx >= 0) {
                ids.removeAt(idx)
                saveStringList(key, ids)
                loadRule(currentRule)
            }
        }
    }

    fun openPartPreview(index: Int) {
        val part = viewStateFlow.value.parts.getOrNull(index) ?: return
        viewStateFlow.value = viewStateFlow.value.copy(
            previewTitle = part.title,
            previewHtml = part.html,
            isPreviewLoading = false,
        )
    }

    fun openAdditionalPrayerPreview(resId: String) {
        val prayer = viewStateFlow.value.additionalPrayers.firstOrNull { it.resId == resId } ?: return
        viewModelScope.launch {
            viewStateFlow.value = viewStateFlow.value.copy(
                previewTitle = prayer.title,
                previewHtml = null,
                isPreviewLoading = true,
            )
            val html = runCatching {
                textBuilder.build(
                    resId = resId,
                    date = currentLocalDate(),
                    fontIndex = 0,
                ).text
            }.getOrElse { "Не удалось загрузить текст: $resId" }
            viewStateFlow.value = viewStateFlow.value.copy(
                previewHtml = html,
                isPreviewLoading = false,
            )
        }
    }

    fun closePreview() {
        viewStateFlow.value = viewStateFlow.value.copy(
            previewTitle = null,
            previewHtml = null,
            isPreviewLoading = false,
        )
    }

    private suspend fun loadRule(type: RuleType) {
        viewStateFlow.value = viewStateFlow.value.copy(selectedRule = type)
        val ruleArrayName = if (type == RuleType.Morning) "morning" else "evening"
        val titlesArrayName = if (type == RuleType.Morning) "morninglist" else "eveninglist"
        val parsedParts = PrayerArraysRepository.getArray(ruleArrayName)
        val parsedTitles = PrayerArraysRepository.getArray(titlesArrayName)

        val enabled = loadEnabledFlags(type, parsedParts.size)
        val partsUi = parsedParts.mapIndexed { index, html ->
            RulePartUi(
                index = index,
                title = parsedTitles.getOrNull(index)?.takeIf { it.isNotBlank() } ?: extractRuleTitle(html, index),
                enabled = enabled.getOrNull(index) ?: true,
                html = html,
            )
        }

        val additionalUi = loadStringList(additionalKey(type)).map { resId ->
            AdditionalPrayerUi(resId = resId, title = titleByResIdCache[resId] ?: resId)
        }

        viewStateFlow.value = RuleEditViewState(
            selectedRule = type,
            parts = partsUi,
            additionalPrayers = additionalUi,
            previewTitle = null,
            previewHtml = null,
            isPreviewLoading = false,
        )
    }

    private suspend fun loadEnabledFlags(type: RuleType, size: Int): List<Boolean> {
        val key = enabledKey(type)
        val raw = storage.stringFlow(key, "").first()
        if (raw.isBlank()) return List(size) { true }
        val parsed = runCatching {
            json.decodeFromString(ListSerializer(Boolean.serializer()), raw)
        }.getOrDefault(emptyList())
        if (parsed.isEmpty()) return List(size) { true }
        return List(size) { idx -> parsed.getOrNull(idx) ?: true }
    }

    private suspend fun saveEnabledFlags(type: RuleType, values: List<Boolean>) {
        val key = enabledKey(type)
        val normalized = values.ifEmpty { listOf(true) }
        storage.setString(key, json.encodeToString(ListSerializer(Boolean.serializer()), normalized))
    }

    private suspend fun loadStringList(key: String): List<String> {
        val raw = storage.stringFlow(key, "").first()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private suspend fun saveStringList(key: String, values: List<String>) {
        storage.setString(key, json.encodeToString(ListSerializer(String.serializer()), values))
    }

    private fun enabledKey(type: RuleType): String {
        return if (type == RuleType.Morning) AppStorageKeys.MorningRuleEnabled else AppStorageKeys.EveningRuleEnabled
    }

    private fun additionalKey(type: RuleType): String {
        return if (type == RuleType.Morning) AppStorageKeys.AdditionalMorningPrayers else AppStorageKeys.AdditionalEveningPrayers
    }

    private fun extractRuleTitle(html: String, index: Int): String {
        val h2 = Regex("""<h2[^>]*>([\s\S]*?)</h2>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
        val b = Regex("""<b[^>]*>([\s\S]*?)</b>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
        val hAny = Regex("""<h[1-6][^>]*>([\s\S]*?)</h[1-6]>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
        val source = h2 ?: b ?: hAny ?: html
        val cleaned = stripTags(source).replace(Regex("""\s+"""), " ").trim()
        if (cleaned.isNotBlank()) return cleaned

        val fallback = stripTags(html).replace(Regex("""\s+"""), " ").trim()
        return if (fallback.isNotBlank()) fallback.take(90) else "Часть ${index + 1}"
    }

    private fun stripTags(text: String): String = text.replace(Regex("""<[^>]+>"""), " ")
}
