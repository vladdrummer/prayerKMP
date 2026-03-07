package com.vladdrummer.prayerkmp.feature.storage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAppStorage(): AppStorage = remember { InMemoryAppStorage() }
