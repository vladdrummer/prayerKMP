package org.example.prayerkmp.feature.mainmenu.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.Res.book_generic
import kotlinproject.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.prayerkmp.feature.tableofcontents.TableOfContentsRepository

class MainViewModel(): ViewModel() {

    val viewState: StateFlow<MainViewState>
        get() = viewStateFlow.asStateFlow()
    private val viewStateFlow = MutableStateFlow(MainViewState())

    init {
        val contentList = mutableListOf<MainMenuItem>()
        viewModelScope.launch {
            TableOfContentsRepository.init()
            TableOfContentsRepository.state.value.forEachIndexed { index, tableOfContents ->
                contentList.add(
                    MainMenuItem(
                        id = index,
                        title = tableOfContents.name,
                        resId = Res.drawable.compose_multiplatform
                    )
                )
            }
        }
    }
}