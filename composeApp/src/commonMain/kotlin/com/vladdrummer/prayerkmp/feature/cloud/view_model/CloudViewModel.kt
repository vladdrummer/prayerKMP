package com.vladdrummer.prayerkmp.feature.cloud.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.cloud.CloudSyncInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CloudViewModel(
    private val email: String,
    private val cloudInteractor: CloudSyncInteractor,
    private val canUseCloud: Boolean,
) : ViewModel() {
    private val viewStateFlow = MutableStateFlow(CloudViewState())
    val viewState: StateFlow<CloudViewState> = viewStateFlow.asStateFlow()

    init {
        viewStateFlow.value = viewStateFlow.value.copy(canUseCloud = canUseCloud)
    }

    fun save() {
        if (!canUseCloud) {
            viewStateFlow.value = viewStateFlow.value.copy(
                message = "Нужна активная подписка для облачного сохранения",
                isError = false
            )
            return
        }
        viewModelScope.launch {
            viewStateFlow.value = viewStateFlow.value.copy(isBusy = true, message = "Сохраняем...", isError = false)
            val result = runCatching { cloudInteractor.saveToCloud(email) }
            viewStateFlow.value = viewStateFlow.value.copy(
                isBusy = false,
                message = result.getOrElse { it.toUiMessage() },
                isError = result.isFailure
            )
        }
    }

    fun restore() {
        if (!canUseCloud) {
            viewStateFlow.value = viewStateFlow.value.copy(
                message = "Нужна активная подписка для облачного восстановления",
                isError = false
            )
            return
        }
        viewModelScope.launch {
            viewStateFlow.value = viewStateFlow.value.copy(isBusy = true, message = "Восстанавливаем...", isError = false)
            val result = runCatching { cloudInteractor.restoreFromCloud(email) }
            viewStateFlow.value = viewStateFlow.value.copy(
                isBusy = false,
                message = result.getOrElse { it.toUiMessage() },
                isError = result.isFailure
            )
        }
    }
}

private fun Throwable.toUiMessage(): String {
    val source = message.orEmpty()
    return if (source.contains("Не удалось обработать ответ сервера", ignoreCase = true) ||
        source.contains("Не удалось обработать ответ от сервера", ignoreCase = true)
    ) {
        "не удалось обработать ответ от сервера"
    } else {
        source.ifBlank { "ошибка связи" }
    }
}
