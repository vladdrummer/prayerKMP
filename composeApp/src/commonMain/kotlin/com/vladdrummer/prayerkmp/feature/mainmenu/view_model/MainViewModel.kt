package com.vladdrummer.prayerkmp.feature.mainmenu.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.prayer.currentLocalDate
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.all_prayer
import kotlinproject.composeapp.generated.resources.book_generic
import kotlinproject.composeapp.generated.resources.canons_and_acathists
import kotlinproject.composeapp.generated.resources.saints
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.vladdrummer.prayerkmp.feature.tableofcontents.TableOfContentsRepository
import kotlinproject.composeapp.generated.resources.bible
import kotlinproject.composeapp.generated.resources.gospel
import kotlinproject.composeapp.generated.resources.message_board
import kotlinproject.composeapp.generated.resources.personal_data
import kotlinproject.composeapp.generated.resources.psalter
import kotlinproject.composeapp.generated.resources.rule_edit
import kotlinproject.composeapp.generated.resources.support

class MainViewModel(): ViewModel() {
    companion object {
        const val PERSONAL_DATA_ITEM_ID = 100
        const val RULE_EDIT_ITEM_ID = 101
        const val READINGS_ITEM_ID = 102
        const val BIBLE_ITEM_ID = 103
        const val PSALTER_ITEM_ID = 104
        const val MESSAGE_BOARD_ITEM_ID = 105
        const val SUPPORT_ITEM_ID = 106
    }

    val viewState: StateFlow<MainViewState>
        get() = viewStateFlow.asStateFlow()
    private val viewStateFlow = MutableStateFlow(MainViewState())

    init {
        viewModelScope.launch {
            val contentList = mutableListOf<MainMenuItem>()
            TableOfContentsRepository.init()
            TableOfContentsRepository.state.value.take(3).forEachIndexed { index, tableOfContents ->
                contentList.add(
                    MainMenuItem(
                        id = index,
                        title = tableOfContents.name,
                        drawable = when (index) {
                            0 -> Res.drawable.all_prayer
                            1 -> Res.drawable.canons_and_acathists
                            else -> Res.drawable.saints
                        }
                    )
                )
            }
            contentList.add(
                MainMenuItem(
                    id = PERSONAL_DATA_ITEM_ID,
                    title = "Персональные данные",
                    drawable = Res.drawable.personal_data
                )
            )
            contentList.add(
                MainMenuItem(
                    id = RULE_EDIT_ITEM_ID,
                    title = "Редактор правил",
                    drawable = Res.drawable.rule_edit
                )
            )
            val date = currentLocalDate()
            val dateText = "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthNumber.toString().padStart(2, '0')}.${date.year}"
            contentList.add(
                MainMenuItem(
                    id = READINGS_ITEM_ID,
                    title = "Евангельские чтения на $dateText",
                    drawable = Res.drawable.gospel
                )
            )
            contentList.add(
                MainMenuItem(
                    id = BIBLE_ITEM_ID,
                    title = "Библия",
                    drawable = Res.drawable.bible
                )
            )
            contentList.add(
                MainMenuItem(
                    id = PSALTER_ITEM_ID,
                    title = "Псалтирь",
                    drawable = Res.drawable.psalter
                )
            )
            contentList.add(
                MainMenuItem(
                    id = MESSAGE_BOARD_ITEM_ID,
                    title = "Молитвы друг за друга",
                    drawable = Res.drawable.message_board
                )
            )
            contentList.add(
                MainMenuItem(
                    id = SUPPORT_ITEM_ID,
                    title = "Поддержать проект",
                    drawable = Res.drawable.support
                )
            )
            viewStateFlow.value = MainViewState(items = contentList)
        }
    }
}
