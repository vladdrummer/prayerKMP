package com.vladdrummer.prayerkmp.feature.mainmenu.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vladdrummer.prayerkmp.feature.prayer.currentLocalDate
import com.vladdrummer.prayerkmp.feature.readings.ReadingsRepository
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.all_prayer
import kotlinproject.composeapp.generated.resources.book_generic
import kotlinproject.composeapp.generated.resources.canons_and_acathists
import kotlinproject.composeapp.generated.resources.saints
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.vladdrummer.prayerkmp.feature.tableofcontents.TableOfContentsRepository
import kotlinproject.composeapp.generated.resources.bible
import kotlinproject.composeapp.generated.resources.cloud
import kotlinproject.composeapp.generated.resources.gospel
import kotlinproject.composeapp.generated.resources.message_board
import kotlinproject.composeapp.generated.resources.personal_data
import kotlinproject.composeapp.generated.resources.psalter
import kotlinproject.composeapp.generated.resources.rule_edit
import kotlinproject.composeapp.generated.resources.support

class MainViewModel(
    private val storage: AppStorage,
): ViewModel() {
    companion object {
        const val PERSONAL_DATA_ITEM_ID = 100
        const val RULE_EDIT_ITEM_ID = 101
        const val READINGS_ITEM_ID = 102
        const val BIBLE_ITEM_ID = 103
        const val PSALTER_ITEM_ID = 104
        const val MESSAGE_BOARD_ITEM_ID = 105
        const val SUPPORT_ITEM_ID = 106
        const val CLOUD_ITEM_ID = 107
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
                    id = CLOUD_ITEM_ID,
                    title = "Облачное сохранение",
                    drawable = Res.drawable.cloud
                )
            )
            contentList.add(
                MainMenuItem(
                    id = SUPPORT_ITEM_ID,
                    title = "Поддержать проект",
                    drawable = Res.drawable.support
                )
            )
            viewStateFlow.value = MainViewState(
                items = contentList,
                todayPreview = TodayPreviewState(isLoading = true),
            )
            loadTodayPreview()
        }
    }

    private fun loadTodayPreview() {
        viewModelScope.launch {
            logToday("loadTodayPreview: start")
            runCatching {
                coroutineScope {
                    val refsDeferred = async { ReadingsRepository.loadTodayReferences(storage = storage) }
                    val calendarDeferred = async { ReadingsRepository.loadTodayCalendarPreview() }
                    val nameDaysDeferred = async { ReadingsRepository.loadTodayNameDays() }
                    val refs = refsDeferred.await()
                    val calendar = calendarDeferred.await()
                    val nameDays = nameDaysDeferred.await()
                    TodayHtmlPreview(
                        celebrating = calendar.celebrating,
                        fast = calendar.fast,
                        nameDays = nameDays,
                        references = refs,
                        tropariAndKondaki = calendar.hymns.map { hymn ->
                            TodayHymnUi(
                                title = hymn.title,
                                glas = hymn.glas,
                                text = hymn.text,
                            )
                        },
                    )
                }
            }.onSuccess { html ->
                logToday("loadTodayPreview: refs loaded, count=${html.references.size}")
                val preview = html
                logToday(
                    "loadTodayPreview: parsed celebrating='${preview.celebrating.orEmpty().take(80)}', " +
                        "fast='${preview.fast.orEmpty().take(80)}', " +
                        "nameDays='${preview.nameDays.orEmpty().take(80)}', refs=${preview.references.size}"
                )
                viewStateFlow.value = viewStateFlow.value.copy(
                    todayPreview = TodayPreviewState(
                        isLoading = false,
                        celebrating = preview.celebrating,
                        fast = preview.fast,
                        nameDays = preview.nameDays,
                        references = preview.references,
                        tropariAndKondaki = preview.tropariAndKondaki,
                        errorText = null,
                    )
                )
                logToday("loadTodayPreview: state updated success")
            }.onFailure {
                logToday("loadTodayPreview: failed ${it::class.simpleName}: ${it.message}")
                viewStateFlow.value = viewStateFlow.value.copy(
                    todayPreview = TodayPreviewState(
                        isLoading = false,
                        celebrating = null,
                        fast = null,
                        nameDays = null,
                        references = emptyList(),
                        tropariAndKondaki = emptyList(),
                        errorText = "Не удалось загрузить блок \"Сегодня\"",
                    )
                )
                logToday("loadTodayPreview: state updated with error")
            }
        }
    }

    private fun logToday(message: String) {
        println("today-preview: $message")
    }
}

private data class TodayHtmlPreview(
    val celebrating: String?,
    val fast: String?,
    val nameDays: String?,
    val references: List<String>,
    val tropariAndKondaki: List<TodayHymnUi>,
)
