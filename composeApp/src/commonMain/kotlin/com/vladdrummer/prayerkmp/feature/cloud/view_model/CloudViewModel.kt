package com.vladdrummer.prayerkmp.feature.cloud.view_model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CloudViewModel : ViewModel() {
    private val viewStateFlow = MutableStateFlow(CloudViewState())
    val viewState: StateFlow<CloudViewState> = viewStateFlow.asStateFlow()
}

