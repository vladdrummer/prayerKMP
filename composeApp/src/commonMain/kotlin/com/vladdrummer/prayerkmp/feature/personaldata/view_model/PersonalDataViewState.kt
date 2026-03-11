package com.vladdrummer.prayerkmp.feature.personaldata.view_model

import kotlinx.serialization.Serializable

internal const val DEFAULT_NAME_IMENIT = "(Имя)"
internal const val DEFAULT_DUHOVNIK = "(Имя его)"
internal const val DEFAULT_PERSON_NAME = "(Имена их)"

@Serializable
data class PersonalPerson(
    val name: String = DEFAULT_PERSON_NAME,
    val gender: Int = 1,
    val status: Int = 0,
)

enum class PersonalSectionType(
    val title: String,
    val isDead: Boolean,
) {
    Parents("О родителях", false),
    Relatives("О родственниках", false),
    Children("О детях", false),
    Benefactors("О наставниках и благодетелях", false),
    Dead("Об усопших", true),
    GodChildren("О крестниках", false),
}

data class PersonalSectionState(
    val type: PersonalSectionType,
    val people: List<PersonalPerson> = listOf(PersonalPerson()),
)

data class PersonalDataViewState(
    val nameImenit: String = DEFAULT_NAME_IMENIT,
    val duhovnik: String = DEFAULT_DUHOVNIK,
    val googleAccountEmail: String = "",
    val isMale: Boolean = false,
    val sections: List<PersonalSectionState> = PersonalSectionType.entries.map { PersonalSectionState(type = it) },
)
