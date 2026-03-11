package com.vladdrummer.prayerkmp.feature.favorites.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import com.vladdrummer.prayerkmp.feature.tableofcontents.TableOfContentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class FavoritesViewModel(
    private val storage: AppStorage,
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }
    private val viewStateFlow = MutableStateFlow(FavoritesViewState())
    val viewState: StateFlow<FavoritesViewState> = viewStateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { TableOfContentsRepository.init() }
            storage.stringFlow(AppStorageKeys.FavoritePrayers, "")
                .distinctUntilChanged()
                .collectLatest { raw ->
                    viewStateFlow.value = viewStateFlow.value.copy(isLoading = true)
                    val ids = parseFavoriteIds(raw)
                    val byId = TableOfContentsRepository.state.value
                        .asSequence()
                        .flatMap { it.item.asSequence() }
                        .filter { !it.resid.isNullOrBlank() }
                        .associateBy { it.resid.orEmpty() }

                    val items = ids.map { id ->
                        val p = byId[id]
                        FavoritePrayerUi(
                            resId = id,
                            title = p?.name ?: id,
                            addable = p?.addable ?: false,
                        )
                    }

                    viewStateFlow.value = FavoritesViewState(
                        isLoading = false,
                        items = items,
                    )
                }
        }
    }

    fun removeFavorite(resId: String) {
        viewModelScope.launch {
            val current = loadFavoriteIds().toMutableList()
            current.removeAll { it == resId }
            saveFavoriteIds(current)
        }
    }

    fun moveFavorite(fromIndex: Int, toIndex: Int) {
        val currentItems = viewStateFlow.value.items
        if (fromIndex == toIndex) return
        if (fromIndex !in currentItems.indices || toIndex !in currentItems.indices) return

        val updated = currentItems.toMutableList().apply {
            val moved = removeAt(fromIndex)
            add(toIndex, moved)
        }
        viewStateFlow.value = viewStateFlow.value.copy(items = updated)

        viewModelScope.launch {
            saveFavoriteIds(updated.map { it.resId })
        }
    }

    private suspend fun loadFavoriteIds(): List<String> {
        val raw = storage.stringFlow(AppStorageKeys.FavoritePrayers, "").first()
        return parseFavoriteIds(raw)
    }

    private fun parseFavoriteIds(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private suspend fun saveFavoriteIds(values: List<String>) {
        storage.setString(
            AppStorageKeys.FavoritePrayers,
            json.encodeToString(ListSerializer(String.serializer()), values.distinct())
        )
    }
}
