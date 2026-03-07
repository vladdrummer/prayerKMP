package com.vladdrummer.prayerkmp.feature.contentlist.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.navigation.PrayerListScreen.PrayerListScreenType
import com.vladdrummer.prayerkmp.feature.tableofcontents.TableOfContentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContentListViewModel(
    private val type: PrayerListScreenType
) : ViewModel() {

    val viewState: StateFlow<ContentListViewState>
        get() = viewStateFlow.asStateFlow()
    private val viewStateFlow = MutableStateFlow(ContentListViewState())

    init {
        viewModelScope.launch {
            TableOfContentsRepository.init()
            val selectedIndex = when (type) {
                PrayerListScreenType.AllPrayer -> 0
                PrayerListScreenType.CannonAcathists -> 1
                PrayerListScreenType.Saints -> 2
            }
            val items = TableOfContentsRepository.state.value
                .getOrNull(selectedIndex)
                ?.item
                .orEmpty()
            viewStateFlow.value = ContentListViewState(items = items)
        }
    }
}
