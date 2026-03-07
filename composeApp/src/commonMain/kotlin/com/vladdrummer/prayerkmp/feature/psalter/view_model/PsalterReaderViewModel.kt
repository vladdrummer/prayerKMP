package com.vladdrummer.prayerkmp.feature.psalter.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.psalter.PsalterMode
import com.vladdrummer.prayerkmp.feature.psalter.PsalterRepository
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PsalterReaderViewModel(
    private val mode: PsalterMode,
    private val kathisma: Int,
    private val startPsalm: Int? = null,
    private val initialPage: Int? = null,
    private val storage: AppStorage,
) : ViewModel() {
    private val viewStateFlow = MutableStateFlow(PsalterReaderViewState())
    val viewState: StateFlow<PsalterReaderViewState> = viewStateFlow.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            viewStateFlow.value = viewStateFlow.value.copy(isLoading = true, errorText = null)
            runCatching {
                PsalterRepository.buildKathismaPages(kathisma = kathisma, mode = mode)
            }.onSuccess { pages ->
                if (pages.isEmpty()) {
                    viewStateFlow.value = viewStateFlow.value.copy(
                        isLoading = false,
                        pages = emptyList(),
                        currentPage = 0,
                        errorText = "Не удалось загрузить текст кафизмы"
                    )
                } else {
                    val startIndex = startPsalm?.let { psalm ->
                        pages.indexOfFirst { page -> page.title.startsWith("Псалом $psalm") }
                            .takeIf { it >= 0 }
                    } ?: initialPage?.coerceIn(0, pages.lastIndex) ?: 0
                    viewStateFlow.value = viewStateFlow.value.copy(
                        isLoading = false,
                        pages = pages,
                        currentPage = startIndex,
                        errorText = null
                    )
                    savePosition(startIndex)
                }
            }.onFailure {
                viewStateFlow.value = viewStateFlow.value.copy(
                    isLoading = false,
                    pages = emptyList(),
                    errorText = "Не удалось загрузить текст кафизмы"
                )
            }
        }
    }

    fun onPageChanged(page: Int) {
        viewStateFlow.value = viewStateFlow.value.copy(currentPage = page)
        savePosition(page)
    }

    private fun savePosition(page: Int) {
        viewModelScope.launch {
            storage.setString(AppStorageKeys.PsalterLastMode, mode.id)
            storage.setString(AppStorageKeys.PsalterLastKathisma, kathisma.toString())
            storage.setString(AppStorageKeys.PsalterLastPage, page.toString())
        }
    }
}
