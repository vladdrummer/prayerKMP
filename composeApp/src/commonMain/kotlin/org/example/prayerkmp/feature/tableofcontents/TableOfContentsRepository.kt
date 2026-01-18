package org.example.prayerkmp.feature.tableofcontents

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope

object TableOfContentsRepository {

    val state: MutableState<List<TableOfContents>> = mutableStateOf(listOf())
    suspend fun init() {
        state.value = loadTableOfContents()
    }
}