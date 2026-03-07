package com.vladdrummer.prayerkmp.feature.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class InMemoryAppStorage : AppStorage {
    private val state = MutableStateFlow<Map<String, Any>>(emptyMap())

    override fun booleanFlow(key: String, default: Boolean): Flow<Boolean> {
        return state.map { map -> (map[key] as? Boolean) ?: default }
    }

    override suspend fun setBoolean(key: String, value: Boolean) {
        state.value = state.value + (key to value)
    }

    override fun stringFlow(key: String, default: String): Flow<String> {
        return state.map { map -> (map[key] as? String) ?: default }
    }

    override suspend fun setString(key: String, value: String) {
        state.value = state.value + (key to value)
    }
}
