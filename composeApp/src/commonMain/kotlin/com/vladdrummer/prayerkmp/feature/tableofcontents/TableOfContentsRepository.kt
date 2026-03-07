package com.vladdrummer.prayerkmp.feature.tableofcontents

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object TableOfContentsRepository {

    val state: MutableState<List<TableOfContents>> = mutableStateOf(listOf())
    private val initMutex = Mutex()
    private var isInitialized = false

    suspend fun init(forceReload: Boolean = false) {
        if (isInitialized && !forceReload) return

        initMutex.withLock {
            if (isInitialized && !forceReload) return
            state.value = loadTableOfContents()
            isInitialized = true
        }
    }
}
