package com.vladdrummer.prayerkmp.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleEmailAuthBottomSheet(
    onDismiss: () -> Unit,
    onAuthorized: (String) -> Unit,
) {
    var step by remember { mutableStateOf(AuthStep.Warning) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authLauncher = rememberGoogleEmailAuthLauncher { result ->
        when {
            !result.email.isNullOrBlank() -> onAuthorized(result.email)
            result.canceled -> {
                step = AuthStep.Error
                errorMessage = "Авторизация отменена"
            }
            !result.errorMessage.isNullOrBlank() -> {
                step = AuthStep.Error
                errorMessage = result.errorMessage
            }
            else -> {
                step = AuthStep.Error
                errorMessage = "Не удалось получить email аккаунта"
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Авторизация",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть"
                    )
                }
            }

            when (step) {
                AuthStep.Warning -> {
                    Text(
                        text = "Приложение запросит доступ к аккаунту и получит email для авторизации.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                step = AuthStep.Loading
                                errorMessage = null
                                authLauncher()
                            }
                        ) { Text("Продолжить") }
                        TextButton(onClick = onDismiss) { Text("Отмена") }
                    }
                }

                AuthStep.Loading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                        Text("Открываем авторизацию...")
                    }
                }

                AuthStep.Error -> {
                    Text(
                        text = errorMessage ?: "Не удалось авторизоваться",
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                step = AuthStep.Loading
                                errorMessage = null
                                authLauncher()
                            }
                        ) { Text("Повторить") }
                        OutlinedButton(onClick = onDismiss) { Text("Отмена") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private enum class AuthStep {
    Warning,
    Loading,
    Error,
}
