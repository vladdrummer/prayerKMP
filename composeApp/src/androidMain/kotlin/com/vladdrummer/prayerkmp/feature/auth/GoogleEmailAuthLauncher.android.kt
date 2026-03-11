package com.vladdrummer.prayerkmp.feature.auth

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private const val TAG = "GoogleEmailAuth"

@Composable
actual fun rememberGoogleEmailAuthLauncher(
    onResult: (GoogleEmailAuthResult) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
    }
    val signInClient = remember(context, gso) { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "ActivityResult: resultCode=${result.resultCode}, hasData=${result.data != null}")
        val data: Intent? = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val authAttempt = runCatching { task.getResult(ApiException::class.java) }
        val account = authAttempt.getOrNull()
        val apiException = authAttempt.exceptionOrNull() as? ApiException
        val email = account?.email
        if (!email.isNullOrBlank()) {
            Log.d(TAG, "Success: email=$email")
            onResult(GoogleEmailAuthResult(email = email))
        } else if (apiException != null) {
            val code = apiException.statusCode
            val message = apiException.message ?: "unknown"
            Log.e(TAG, "ApiException: code=$code, message=$message", apiException)
            val humanMessage = when (code) {
                10 -> "Google Sign-In не настроен для этого приложения (code=10). Проверьте SHA-1/SHA-256 и OAuth client."
                12501 -> "Вход отменён пользователем (code=12501)."
                7 -> "Ошибка сети при авторизации (code=7)."
                else -> "Google Sign-In ошибка: code=$code, $message"
            }
            onResult(
                GoogleEmailAuthResult(
                    errorMessage = humanMessage
                )
            )
        } else if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
            Log.w(TAG, "Canceled: no ApiException, resultCode=CANCELED")
            onResult(GoogleEmailAuthResult(canceled = true))
        } else {
            Log.e(TAG, "Failed: no email, no ApiException, resultCode=${result.resultCode}")
            onResult(GoogleEmailAuthResult(errorMessage = "Не удалось получить email Google-аккаунта"))
        }
    }

    return remember(signInClient, launcher) {
        {
            Log.d(TAG, "Launch sign-in intent")
            launcher.launch(signInClient.signInIntent)
        }
    }
}
