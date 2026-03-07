package com.vladdrummer.prayerkmp.feature.storage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private object AppDataStoreHolder {
    @Volatile
    private var instance: DataStore<Preferences>? = null

    fun get(): DataStore<Preferences> {
        return instance ?: synchronized(this) {
            instance ?: createDataStore().also { instance = it }
        }
    }
}

private class IosAppStorage(
    private val dataStore: DataStore<Preferences>
) : AppStorage {
    override fun booleanFlow(key: String, default: Boolean): Flow<Boolean> {
        val prefKey = booleanPreferencesKey(key)
        return dataStore.data.map { prefs -> prefs[prefKey] ?: default }
    }

    override suspend fun setBoolean(key: String, value: Boolean) {
        val prefKey = booleanPreferencesKey(key)
        dataStore.edit { prefs -> prefs[prefKey] = value }
    }

    override fun stringFlow(key: String, default: String): Flow<String> {
        val prefKey = stringPreferencesKey(key)
        return dataStore.data.map { prefs -> prefs[prefKey] ?: default }
    }

    override suspend fun setString(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        dataStore.edit { prefs -> prefs[prefKey] = value }
    }
}

@Composable
actual fun rememberAppStorage(): AppStorage = remember { IosAppStorage(AppDataStoreHolder.get()) }
