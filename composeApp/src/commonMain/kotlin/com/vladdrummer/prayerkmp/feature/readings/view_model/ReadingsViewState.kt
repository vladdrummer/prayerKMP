package com.vladdrummer.prayerkmp.feature.readings.view_model

data class ReadingsViewState(
    val isLoading: Boolean = true,
    val htmlText: String = "",
    val errorText: String? = null,
)

