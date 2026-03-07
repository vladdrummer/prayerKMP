package com.vladdrummer.prayerkmp

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // No hardware/system back event on iOS.
}
