package com.vladdrummer.prayerkmp.feature.psalter.view_model

import com.vladdrummer.prayerkmp.feature.psalter.PsalterMode

data class PsalterSelectableName(
    val id: String,
    val title: String,
    val selected: Boolean = false,
)

data class PsalterViewState(
    val selectedMode: PsalterMode = PsalterMode.Usual,
    val kathismas: List<Int> = (1..20).toList(),
    val expandedKathismas: Set<Int> = emptySet(),
    val kathismaPsalms: Map<Int, List<Int>> = emptyMap(),
    val deadNames: List<PsalterSelectableName> = emptyList(),
    val healthNames: List<PsalterSelectableName> = emptyList(),
    val lastReadMode: PsalterMode? = null,
    val lastReadKathisma: Int? = null,
    val lastReadPage: Int? = null,
)
