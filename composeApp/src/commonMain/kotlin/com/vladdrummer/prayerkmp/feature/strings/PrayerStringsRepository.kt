package com.vladdrummer.prayerkmp.feature.strings

object PrayerStringsRepository {
    fun init(forceReload: Boolean = false) = Unit

    fun getString(resId: String): String = PrayerStringsMap.values[resId] ?: resId

    fun size(): Int = PrayerStringsMap.values.size
}

fun getString(resId: String): String = PrayerStringsRepository.getString(resId)
