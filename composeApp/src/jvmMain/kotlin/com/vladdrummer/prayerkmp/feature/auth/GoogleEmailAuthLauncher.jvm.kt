package com.vladdrummer.prayerkmp.feature.auth

import androidx.compose.runtime.Composable

@Composable
actual fun rememberGoogleEmailAuthLauncher(
    onResult: (GoogleEmailAuthResult) -> Unit
): () -> Unit {
    return { onResult(GoogleEmailAuthResult(errorMessage = "Авторизация через Google доступна на Android")) }
}

