package com.vladdrummer.prayerkmp.feature.messageboard.view_model

data class MessageBoardMessageUi(
    val id: Int,
    val name: String,
    val message: String,
    val date: String,
    val votes: Int,
    val senderType: Int,
    val gmail: String,
)

data class MessageBoardViewState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<MessageBoardMessageUi> = emptyList(),
    val currentUserEmail: String = "",
    val suggestedName: String = "",
)
