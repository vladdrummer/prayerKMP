package com.vladdrummer.prayerkmp.feature.messageboard.view_model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MessageBoardViewModel : ViewModel() {
    private val viewStateFlow = MutableStateFlow(MessageBoardViewState())
    val viewState: StateFlow<MessageBoardViewState> = viewStateFlow.asStateFlow()
}
