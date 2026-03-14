package com.vladdrummer.prayerkmp.feature.mainmenu.view_model

data class MainViewState (
    val items: List<MainMenuItem> = listOf(),
    val todayPreview: TodayPreviewState = TodayPreviewState(),
)

data class TodayPreviewState(
    val isLoading: Boolean = true,
    val celebrating: String? = null,
    val fast: String? = null,
    val nameDays: String? = null,
    val references: List<String> = emptyList(),
    val tropariAndKondaki: List<TodayHymnUi> = emptyList(),
    val errorText: String? = null,
)

data class TodayHymnUi(
    val title: String,
    val glas: String?,
    val text: String,
)
