package com.vladdrummer.prayerkmp.feature.prayer

import kotlinx.datetime.LocalDate
import java.time.LocalDate as JvmLocalDate
import java.time.LocalTime as JvmLocalTime

actual fun currentLocalDate(): LocalDate {
    val now = JvmLocalDate.now()
    return LocalDate(now.year, now.monthValue, now.dayOfMonth)
}

actual fun currentHourOfDay(): Int = JvmLocalTime.now().hour
