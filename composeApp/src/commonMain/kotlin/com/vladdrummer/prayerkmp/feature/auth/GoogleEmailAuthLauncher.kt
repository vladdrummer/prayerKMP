package com.vladdrummer.prayerkmp.feature.auth

import androidx.compose.runtime.Composable

data class GoogleEmailAuthResult(
    val email: String? = null,
    val canceled: Boolean = false,
    val errorMessage: String? = null,
)

@Composable
expect fun rememberGoogleEmailAuthLauncher(
    onResult: (GoogleEmailAuthResult) -> Unit
): () -> Unit

