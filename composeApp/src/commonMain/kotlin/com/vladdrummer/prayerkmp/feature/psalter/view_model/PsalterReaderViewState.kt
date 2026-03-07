package com.vladdrummer.prayerkmp.feature.psalter.view_model

import com.vladdrummer.prayerkmp.feature.psalter.PsalterReaderPage

data class PsalterReaderViewState(
    val isLoading: Boolean = true,
    val pages: List<PsalterReaderPage> = emptyList(),
    val currentPage: Int = 0,
    val errorText: String? = null,
)
