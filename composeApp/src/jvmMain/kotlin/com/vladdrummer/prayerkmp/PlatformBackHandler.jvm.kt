package com.vladdrummer.prayerkmp

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // Desktop has no default back dispatcher in this app.
}
