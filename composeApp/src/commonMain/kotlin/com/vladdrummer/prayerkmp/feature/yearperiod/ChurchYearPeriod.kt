package com.vladdrummer.prayerkmp.feature.yearperiod

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

enum class ChurchYearPeriod(val legacyValue: Int) {
    Regular(0),
    GreatFast(1),
    Easter(2),
    EasterToAscension(3),
    AscensionToTrinity(4),
}

data class ChurchPeriodBoundaries(
    val greatFastStart: LocalDate,
    val easterStart: LocalDate,
    val easterEnd: LocalDate,
    val easterToAscensionStart: LocalDate,
    val easterToAscensionEnd: LocalDate,
    val ascensionToTrinityStart: LocalDate,
    val ascensionToTrinityEnd: LocalDate,
    val trinity: LocalDate,
)

object ChurchYearPeriodResolver {
    fun currentPeriod(year: Int, month: Int, day: Int): ChurchYearPeriod =
        currentPeriod(LocalDate(year, month, day))

    fun currentPeriod(now: LocalDate): ChurchYearPeriod {
        val bounds = boundaries(now.year)
        return when {
            now inDateRange bounds.greatFastStart..bounds.easterStart.addDays(-1) -> ChurchYearPeriod.GreatFast
            now inDateRange bounds.easterStart..bounds.easterEnd -> ChurchYearPeriod.Easter
            now inDateRange bounds.easterToAscensionStart..bounds.easterToAscensionEnd -> ChurchYearPeriod.EasterToAscension
            now inDateRange bounds.ascensionToTrinityStart..bounds.ascensionToTrinityEnd -> ChurchYearPeriod.AscensionToTrinity
            else -> ChurchYearPeriod.Regular
        }
    }

    fun boundaries(year: Int): ChurchPeriodBoundaries {
        val easter = orthodoxEasterGregorian(year)
        val greatFastStart = easter.addDays(-48)
        val easterEnd = easter.addDays(7)
        val easterToAscensionStart = easter.addDays(8)
        val ascension = easter.addDays(39)
        val easterToAscensionEnd = ascension.addDays(-1)
        val trinity = easter.addDays(49)
        val ascensionToTrinityStart = ascension
        val ascensionToTrinityEnd = trinity.addDays(-1)

        return ChurchPeriodBoundaries(
            greatFastStart = greatFastStart,
            easterStart = easter,
            easterEnd = easterEnd,
            easterToAscensionStart = easterToAscensionStart,
            easterToAscensionEnd = easterToAscensionEnd,
            ascensionToTrinityStart = ascensionToTrinityStart,
            ascensionToTrinityEnd = ascensionToTrinityEnd,
            trinity = trinity,
        )
    }

    fun orthodoxEasterGregorian(year: Int): LocalDate {
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val monthJulian = (d + e + 114) / 31
        val dayJulian = ((d + e + 114) % 31) + 1
        val julianDate = LocalDate(year, monthJulian, dayJulian)

        // Difference between Julian and Gregorian calendars for this century.
        val century = year / 100
        val shiftDays = century - (century / 4) - 2
        return julianDate.addDays(shiftDays)
    }
}

private infix fun LocalDate.inDateRange(range: ClosedRange<LocalDate>): Boolean = this >= range.start && this <= range.endInclusive

private fun LocalDate.addDays(days: Int): LocalDate = this.plus(DatePeriod(days = days))
