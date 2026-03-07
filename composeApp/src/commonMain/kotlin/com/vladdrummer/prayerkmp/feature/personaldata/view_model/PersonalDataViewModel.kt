package com.vladdrummer.prayerkmp.feature.personaldata.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class PersonalDataViewModel(
    private val storage: AppStorage,
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }

    private val viewStateFlow = MutableStateFlow(PersonalDataViewState())
    val viewState: StateFlow<PersonalDataViewState> = viewStateFlow.asStateFlow()

    init {
        loadFromStorage()
    }

    private fun loadFromStorage() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.Default) {
                coroutineScope {
                    val nameImenit = async {
                        storage.stringFlow(AppStorageKeys.NameImenit, DEFAULT_NAME_IMENIT).first()
                    }
                    val duhovnik = async {
                        storage.stringFlow(AppStorageKeys.Duhovnik, DEFAULT_DUHOVNIK).first()
                    }
                    val isMale = async {
                        storage.booleanFlow(AppStorageKeys.MyGenderMale, false).first()
                    }
                    val parents = async {
                        decodePeople(storage.stringFlow(AppStorageKeys.PersonalParents, "").first())
                    }
                    val relatives = async {
                        decodePeople(storage.stringFlow(AppStorageKeys.PersonalRelatives, "").first())
                    }
                    val children = async {
                        decodePeople(storage.stringFlow(AppStorageKeys.PersonalChildren, "").first())
                    }
                    val benefactors = async {
                        decodePeople(storage.stringFlow(AppStorageKeys.PersonalBenefactors, "").first())
                    }
                    val dead = async {
                        decodePeople(storage.stringFlow(AppStorageKeys.PersonalDead, "").first())
                    }
                    val godChildren = async {
                        decodePeople(storage.stringFlow(AppStorageKeys.PersonalGodChildren, "").first())
                    }
                    LoadedPersonalData(
                        nameImenit = nameImenit.await(),
                        duhovnik = duhovnik.await(),
                        isMale = isMale.await(),
                        parents = parents.await(),
                        relatives = relatives.await(),
                        children = children.await(),
                        benefactors = benefactors.await(),
                        dead = dead.await(),
                        godChildren = godChildren.await(),
                    )
                }
            }

            viewStateFlow.value = PersonalDataViewState(
                nameImenit = loaded.nameImenit,
                duhovnik = loaded.duhovnik,
                isMale = loaded.isMale,
                sections = listOf(
                    PersonalSectionState(PersonalSectionType.Parents, loaded.parents),
                    PersonalSectionState(PersonalSectionType.Relatives, loaded.relatives),
                    PersonalSectionState(PersonalSectionType.Children, loaded.children),
                    PersonalSectionState(PersonalSectionType.Benefactors, loaded.benefactors),
                    PersonalSectionState(PersonalSectionType.Dead, loaded.dead),
                    PersonalSectionState(PersonalSectionType.GodChildren, loaded.godChildren),
                )
            )
        }
    }

    fun onNameImenitChanged(value: String) {
        viewStateFlow.value = viewStateFlow.value.copy(
            nameImenit = if (value.trim().isEmpty()) DEFAULT_NAME_IMENIT else value
        )
        viewModelScope.launch {
            storage.setString(
                AppStorageKeys.NameImenit,
                if (value.trim().isEmpty()) DEFAULT_NAME_IMENIT else value
            )
        }
    }

    fun onDuhovnikChanged(value: String) {
        viewStateFlow.value = viewStateFlow.value.copy(
            duhovnik = if (value.trim().isEmpty()) DEFAULT_DUHOVNIK else value
        )
        viewModelScope.launch {
            storage.setString(
                AppStorageKeys.Duhovnik,
                if (value.trim().isEmpty()) DEFAULT_DUHOVNIK else value
            )
        }
    }

    fun onGenderChanged(isMale: Boolean) {
        viewStateFlow.value = viewStateFlow.value.copy(isMale = isMale)
        viewModelScope.launch { storage.setBoolean(AppStorageKeys.MyGenderMale, isMale) }
    }

    fun onPersonNameChanged(type: PersonalSectionType, index: Int, value: String) {
        updateSection(type) { list ->
            list.toMutableList().apply {
                if (index in indices) {
                    this[index] = this[index].copy(
                        name = if (value.trim().isEmpty()) DEFAULT_PERSON_NAME else value
                    )
                }
            }
        }
    }

    fun onPersonGenderChanged(type: PersonalSectionType, index: Int, gender: Int) {
        updateSection(type) { list ->
            list.toMutableList().apply {
                if (index in indices) this[index] = this[index].copy(gender = gender)
            }
        }
    }

    fun onPersonStatusChanged(type: PersonalSectionType, index: Int, status: Int) {
        updateSection(type) { list ->
            list.toMutableList().apply {
                if (index in indices) {
                    val statuses = statusList(type.isDead)
                    val safeStatus = status.coerceIn(0, statuses.lastIndex)
                    this[index] = this[index].copy(status = safeStatus)
                }
            }
        }
    }

    fun onPersonAdded(type: PersonalSectionType) {
        updateSection(type) { list ->
            (list + PersonalPerson()).toMutableList()
        }
    }

    fun onPersonRemoved(type: PersonalSectionType, index: Int) {
        updateSection(type) { list ->
            val mutable = list.toMutableList()
            if (index in mutable.indices) mutable.removeAt(index)
            if (mutable.isEmpty()) mutable += PersonalPerson()
            mutable
        }
    }

    private fun updateSection(type: PersonalSectionType, mutate: (List<PersonalPerson>) -> List<PersonalPerson>) {
        val state = viewStateFlow.value
        val section = state.sections.firstOrNull { it.type == type } ?: return
        val updated = mutate(section.people)
        viewStateFlow.value = state.copy(
            sections = state.sections.map { if (it.type == type) it.copy(people = updated) else it }
        )
        viewModelScope.launch {
            storage.setString(keyFor(type), encodePeople(updated))
        }
    }

    private fun keyFor(type: PersonalSectionType): String = when (type) {
        PersonalSectionType.Parents -> AppStorageKeys.PersonalParents
        PersonalSectionType.Relatives -> AppStorageKeys.PersonalRelatives
        PersonalSectionType.Children -> AppStorageKeys.PersonalChildren
        PersonalSectionType.Benefactors -> AppStorageKeys.PersonalBenefactors
        PersonalSectionType.Dead -> AppStorageKeys.PersonalDead
        PersonalSectionType.GodChildren -> AppStorageKeys.PersonalGodChildren
    }

    private fun encodePeople(people: List<PersonalPerson>): String =
        json.encodeToString(ListSerializer(PersonalPerson.serializer()), people)

    private fun decodePeople(raw: String): List<PersonalPerson> {
        if (raw.isBlank()) return listOf(PersonalPerson())
        return runCatching {
            json.decodeFromString(ListSerializer(PersonalPerson.serializer()), raw)
        }
            .getOrElse { listOf(PersonalPerson()) }
            .ifEmpty { listOf(PersonalPerson()) }
    }
}

private data class LoadedPersonalData(
    val nameImenit: String,
    val duhovnik: String,
    val isMale: Boolean,
    val parents: List<PersonalPerson>,
    val relatives: List<PersonalPerson>,
    val children: List<PersonalPerson>,
    val benefactors: List<PersonalPerson>,
    val dead: List<PersonalPerson>,
    val godChildren: List<PersonalPerson>,
)

fun statusList(isDead: Boolean): List<String> {
    return if (isDead) {
        listOf("", "новопрест.", "уб.")
    } else {
        listOf(
            "",
            "мл.",
            "отр.",
            "бол.",
            "воин.",
            "болящ.",
            "путеш.",
            "закл.",
            "непразд.",
            "иер.",
            "прот.",
            "мон.",
            "иеромнх.",
            "послуш.",
            "патр.",
            "митр.",
            "архиеп.",
            "еп.",
            "протопресв.",
            "архим.",
            "прот.",
            "игум.",
            "архидиак.",
            "протодиак.",
            "иеродиак.",
            "диак.",
            "иподиак.",
            "схимон.",
        )
    }
}
