package com.vladdrummer.prayerkmp.feature.prayer

object PrayerScrollSession {
    @Volatile
    var currentResId: String = ""

    @Volatile
    var currentScroll: Int = 0
}
