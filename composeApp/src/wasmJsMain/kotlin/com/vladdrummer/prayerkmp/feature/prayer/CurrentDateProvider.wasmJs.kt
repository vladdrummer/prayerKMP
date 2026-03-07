package com.vladdrummer.prayerkmp.feature.prayer

import kotlinx.datetime.LocalDate
import kotlin.js.Date

actual fun currentLocalDate(): LocalDate {
    val now = Date()
    val year = now.getFullYear().toInt()
    val month = now.getMonth().toInt() + 1
    val day = now.getDate().toInt()
    return LocalDate(year, month, day)
}
