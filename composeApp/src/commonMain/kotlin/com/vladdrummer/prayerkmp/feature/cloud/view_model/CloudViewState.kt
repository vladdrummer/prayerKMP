package com.vladdrummer.prayerkmp.feature.cloud.view_model

data class CloudViewState(
    val isBusy: Boolean = false,
    val message: String = "Вы можете сохранить настройки в облако и восстановить их на другом устройстве.",
    val canUseCloud: Boolean = true,
    val isError: Boolean = false,
)
