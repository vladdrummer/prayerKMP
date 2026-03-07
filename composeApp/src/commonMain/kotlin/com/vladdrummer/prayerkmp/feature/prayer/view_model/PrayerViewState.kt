package com.vladdrummer.prayerkmp.feature.prayer.view_model

import com.vladdrummer.prayerkmp.feature.yearperiod.ChurchYearPeriod

data class PrayerViewState(
    val title: String = "",
    val resId: String = "",
    val text: String = "",
    val isLoading: Boolean = true,
    val period: ChurchYearPeriod = ChurchYearPeriod.Regular,
    val addable: Boolean = false,
    val isAddedToMorning: Boolean = false,
    val isAddedToEvening: Boolean = false,
    val fontSizeSp: Int = 20,
    val fontIndex: Int = 0,
    val initialScrollPosition: Int = 0,
)
