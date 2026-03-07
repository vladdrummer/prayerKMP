package com.vladdrummer.prayerkmp.feature.favorites.view_model

data class FavoritePrayerUi(
    val resId: String,
    val title: String,
    val addable: Boolean,
)

data class FavoritesViewState(
    val isLoading: Boolean = true,
    val items: List<FavoritePrayerUi> = emptyList(),
)
