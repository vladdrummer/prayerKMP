package com.vladdrummer.prayerkmp.feature.prayer

import kotlinx.datetime.LocalDate
import java.time.LocalDate as AndroidLocalDate
import java.time.LocalTime as AndroidLocalTime

actual fun currentLocalDate(): LocalDate {
    val now = AndroidLocalDate.now()
    return LocalDate(now.year, now.monthValue, now.dayOfMonth)
}

actual fun currentHourOfDay(): Int = AndroidLocalTime.now().hour
