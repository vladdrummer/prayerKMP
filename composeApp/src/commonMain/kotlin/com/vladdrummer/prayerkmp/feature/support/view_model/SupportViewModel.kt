package com.vladdrummer.prayerkmp.feature.support.view_model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupportViewModel : ViewModel() {
    private val viewStateFlow = MutableStateFlow(SupportViewState())
    val viewState: StateFlow<SupportViewState> = viewStateFlow.asStateFlow()
}
