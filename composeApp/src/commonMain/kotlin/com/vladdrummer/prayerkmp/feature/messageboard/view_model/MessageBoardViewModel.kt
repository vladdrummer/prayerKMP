package com.vladdrummer.prayerkmp.feature.messageboard.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.messageboard.MessageBoardMessageDto
import com.vladdrummer.prayerkmp.feature.messageboard.MessageBoardRepository
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

class MessageBoardViewModel(
    private val storage: AppStorage,
    private val currentUserEmail: String,
    private val senderType: Int,
    private val repository: MessageBoardRepository = MessageBoardRepository(),
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }
    private val votedIds = mutableSetOf<Int>()

    private val viewStateFlow = MutableStateFlow(
        MessageBoardViewState(
            isLoading = true,
            currentUserEmail = currentUserEmail
        )
    )
    val viewState: StateFlow<MessageBoardViewState> = viewStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            val suggestedName = storage.stringFlow(AppStorageKeys.NameImenit, "").first()
            val votedRaw = storage.stringFlow(AppStorageKeys.MessageBoardVotedIds, "").first()
            votedIds.clear()
            votedIds += runCatching {
                json.decodeFromString(ListSerializer(Int.serializer()), votedRaw)
            }.getOrDefault(emptyList())
            viewStateFlow.value = viewStateFlow.value.copy(
                suggestedName = suggestedName.trim()
            )
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            viewStateFlow.value = viewStateFlow.value.copy(
                isLoading = true,
                errorMessage = null
            )
            val result = runCatching { repository.showData() }
            viewStateFlow.value = result.fold(
                onSuccess = { response ->
                    viewStateFlow.value.copy(
                        isLoading = false,
                        messages = response.messages.map { it.toUi() },
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    viewStateFlow.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить данные"
                    )
                }
            )
        }
    }

    fun postMessage(text: String, name: String) {
        mutateMessageBoard {
            repository.postData(
                text = text.trim(),
                gmail = currentUserEmail,
                name = name.trim(),
                senderType = senderType
            )
        }
    }

    fun editMessage(id: Int, text: String, name: String) {
        mutateMessageBoard {
            repository.editData(
                id = id,
                text = text.trim(),
                gmail = currentUserEmail,
                name = name.trim()
            )
        }
    }

    fun removeMessage(id: Int) {
        mutateMessageBoard {
            repository.removeData(
                id = id,
                gmail = currentUserEmail
            )
        }
    }

    fun vote(id: Int) {
        if (id in votedIds) {
            viewStateFlow.value = viewStateFlow.value.copy(
                errorMessage = "Вы уже голосовали за эту просьбу"
            )
            return
        }
        mutateMessageBoard {
            val response = repository.performVote(id = id, gmail = currentUserEmail)
            votedIds += id
            storage.setString(
                AppStorageKeys.MessageBoardVotedIds,
                json.encodeToString(ListSerializer(Int.serializer()), votedIds.toList())
            )
            response
        }
    }

    private fun mutateMessageBoard(action: suspend () -> com.vladdrummer.prayerkmp.feature.messageboard.MessageBoardResponse) {
        viewModelScope.launch {
            viewStateFlow.value = viewStateFlow.value.copy(
                isSubmitting = true,
                errorMessage = null
            )
            val result = runCatching { action() }
            viewStateFlow.value = result.fold(
                onSuccess = { response ->
                    viewStateFlow.value.copy(
                        isSubmitting = false,
                        messages = response.messages.map { it.toUi() },
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    viewStateFlow.value.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Операция не выполнена"
                    )
                }
            )
        }
    }

    private fun MessageBoardMessageDto.toUi(): MessageBoardMessageUi {
        return MessageBoardMessageUi(
            id = id,
            name = name,
            message = message,
            date = date,
            votes = votes,
            senderType = senderType,
            gmail = gmail
        )
    }
}
