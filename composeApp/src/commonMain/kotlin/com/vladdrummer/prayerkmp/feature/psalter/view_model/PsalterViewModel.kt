package com.vladdrummer.prayerkmp.feature.psalter.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.psalter.PsalterMode
import com.vladdrummer.prayerkmp.feature.psalter.PsalterRepository
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.DEFAULT_PERSON_NAME
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.PersonalPerson
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class PsalterViewModel(
    private val storage: AppStorage,
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }
    private val viewStateFlow = MutableStateFlow(
        PsalterViewState(
            kathismas = PsalterRepository.kathismas,
            kathismaPsalms = PsalterRepository.kathismas.associateWith { PsalterRepository.getPsalmsForKathisma(it) }
        )
    )
    val viewState: StateFlow<PsalterViewState> = viewStateFlow.asStateFlow()

    init {
        loadFromStorage()
    }

    private fun loadFromStorage() {
        viewModelScope.launch {
            val lastMode = PsalterMode.fromId(storage.stringFlow(AppStorageKeys.PsalterLastMode, PsalterMode.Usual.id).first())
            val lastKathisma = storage.stringFlow(AppStorageKeys.PsalterLastKathisma, "").first().toIntOrNull()
            val lastPage = storage.stringFlow(AppStorageKeys.PsalterLastPage, "").first().toIntOrNull()

            val selectedDead = decodeStringList(storage.stringFlow(AppStorageKeys.PsalterSelectedDeadNames, "").first()).toSet()
            val selectedHealth = decodeStringList(storage.stringFlow(AppStorageKeys.PsalterSelectedHealthNames, "").first()).toSet()

            val deadNames = decodePeople(storage.stringFlow(AppStorageKeys.PersonalDead, "").first())
                .mapNotNull { person ->
                    val title = person.name.trim()
                    if (title.isBlank() || title == DEFAULT_PERSON_NAME) null
                    else PsalterSelectableName(id = "dead:$title", title = title, selected = selectedDead.contains("dead:$title"))
                }
                .distinctBy { it.id }

            val healthPeople = buildList {
                addAll(decodePeople(storage.stringFlow(AppStorageKeys.PersonalRelatives, "").first()))
                addAll(decodePeople(storage.stringFlow(AppStorageKeys.PersonalChildren, "").first()))
                addAll(decodePeople(storage.stringFlow(AppStorageKeys.PersonalGodChildren, "").first()))
                addAll(decodePeople(storage.stringFlow(AppStorageKeys.PersonalParents, "").first()))
                addAll(decodePeople(storage.stringFlow(AppStorageKeys.PersonalBenefactors, "").first()))
            }
            val healthNames = healthPeople
                .mapNotNull { person ->
                    val title = person.name.trim()
                    if (title.isBlank() || title == DEFAULT_PERSON_NAME) null
                    else PsalterSelectableName(id = "health:$title", title = title, selected = selectedHealth.contains("health:$title"))
                }
                .distinctBy { it.id }

            viewStateFlow.value = viewStateFlow.value.copy(
                selectedMode = lastMode,
                deadNames = deadNames,
                healthNames = healthNames,
                lastReadMode = if (lastKathisma != null && lastPage != null) lastMode else null,
                lastReadKathisma = lastKathisma,
                lastReadPage = lastPage,
            )
        }
    }

    fun selectMode(mode: PsalterMode) {
        viewStateFlow.value = viewStateFlow.value.copy(selectedMode = mode)
        viewModelScope.launch {
            storage.setString(AppStorageKeys.PsalterLastMode, mode.id)
        }
    }

    fun toggleKathisma(kathisma: Int) {
        val expanded = viewStateFlow.value.expandedKathismas.toMutableSet()
        if (expanded.contains(kathisma)) expanded.remove(kathisma) else expanded.add(kathisma)
        viewStateFlow.value = viewStateFlow.value.copy(
            expandedKathismas = expanded
        )
    }

    fun toggleNameSelection(id: String) {
        val state = viewStateFlow.value
        if (id.startsWith("dead:")) {
            val updated = state.deadNames.map { if (it.id == id) it.copy(selected = !it.selected) else it }
            viewStateFlow.value = state.copy(deadNames = updated)
            saveSelectedNames(AppStorageKeys.PsalterSelectedDeadNames, updated)
        } else {
            val updated = state.healthNames.map { if (it.id == id) it.copy(selected = !it.selected) else it }
            viewStateFlow.value = state.copy(healthNames = updated)
            saveSelectedNames(AppStorageKeys.PsalterSelectedHealthNames, updated)
        }
    }

    private fun saveSelectedNames(key: String, names: List<PsalterSelectableName>) {
        val selected = names.filter { it.selected }.map { it.id }
        viewModelScope.launch {
            storage.setString(key, json.encodeToString(ListSerializer(String.serializer()), selected))
        }
    }

    private fun decodeStringList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(String.serializer()), raw) }.getOrDefault(emptyList())
    }

    private fun decodePeople(raw: String): List<PersonalPerson> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(PersonalPerson.serializer()), raw)
        }.getOrDefault(emptyList())
    }
}
