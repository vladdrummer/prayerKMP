package com.vladdrummer.prayerkmp.feature.yearperiod

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ChurchYearPeriodResolverTest {
    @Test
    fun periods_match_2024_reference_dates() {
        assertEquals(
            ChurchYearPeriod.GreatFast,
            ChurchYearPeriodResolver.currentPeriod(LocalDate(2024, 3, 18))
        )
        assertEquals(
            ChurchYearPeriod.Easter,
            ChurchYearPeriodResolver.currentPeriod(LocalDate(2024, 5, 5))
        )
        assertEquals(
            ChurchYearPeriod.EasterToAscension,
            ChurchYearPeriodResolver.currentPeriod(LocalDate(2024, 5, 13))
        )
        assertEquals(
            ChurchYearPeriod.AscensionToTrinity,
            ChurchYearPeriodResolver.currentPeriod(LocalDate(2024, 6, 13))
        )
        assertEquals(
            ChurchYearPeriod.Regular,
            ChurchYearPeriodResolver.currentPeriod(LocalDate(2024, 6, 23))
        )
    }

    @Test
    fun easter_dates_match_known_values() {
        assertEquals(LocalDate(2025, 4, 20), ChurchYearPeriodResolver.orthodoxEasterGregorian(2025))
        assertEquals(LocalDate(2026, 4, 12), ChurchYearPeriodResolver.orthodoxEasterGregorian(2026))
    }
}
