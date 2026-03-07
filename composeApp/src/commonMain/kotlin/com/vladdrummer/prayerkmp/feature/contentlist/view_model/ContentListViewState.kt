package com.vladdrummer.prayerkmp.feature.contentlist.view_model

import com.vladdrummer.prayerkmp.feature.tableofcontents.PrayerData

data class ContentListViewState(
    val items: List<PrayerData> = listOf()
)
