package com.example.timeapk.ui.utils

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

const val DATE_FORMAT_DOT = 0
const val DATE_FORMAT_DASH = 1

fun getDisplayDateFormatter(mode: Int): DateTimeFormatter = when (mode) {
    DATE_FORMAT_DASH -> DateTimeFormatter.ofPattern("yyyy-MM-dd")
    else -> DateTimeFormatter.ofPattern("yyyy.MM.dd")
}

val DisplayDateFormatter: DateTimeFormatter = getDisplayDateFormatter(DATE_FORMAT_DOT)

fun formatDays(days: Long, locale: Locale = Locale.getDefault()): String {
    return NumberFormat.getIntegerInstance(locale).format(days)
}

private fun localizedDuration(
    years: Long = 0L,
    months: Long = 0L,
    days: Long = 0L,
    locale: Locale = Locale.getDefault()
): String {
    val measures = mutableListOf<Measure>()
    if (years > 0) measures += Measure(years, MeasureUnit.YEAR)
    if (months > 0) measures += Measure(months, MeasureUnit.MONTH)
    if (days > 0 || measures.isEmpty()) measures += Measure(days, MeasureUnit.DAY)
    val formatter = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT)
    return formatter.formatMeasures(*measures.toTypedArray())
}

fun formatDaysAsYMD(
    days: Long,
    useMonths: Boolean = true,
    locale: Locale = Locale.getDefault()
): String {
    val totalDays = abs(days)
    if (totalDays < 30) {
        return localizedDuration(days = totalDays, locale = locale)
    }

    val years = totalDays / 365
    val remainderAfterYears = totalDays % 365

    if (years > 0) {
        val months = remainderAfterYears / 30
        return when {
            useMonths && months > 0 -> localizedDuration(
                years = years,
                months = months,
                locale = locale
            )

            else -> localizedDuration(
                years = years,
                days = remainderAfterYears,
                locale = locale
            )
        }
    }

    val months = totalDays / 30
    val remainDays = totalDays % 30
    return localizedDuration(months = months, days = remainDays, locale = locale)
}

fun formatDaysSmart(
    days: Long,
    showAsYMD: Boolean = false,
    locale: Locale = Locale.getDefault()
): String {
    return if (showAsYMD && abs(days) >= 30) {
        formatDaysAsYMD(days, locale = locale)
    } else {
        formatDays(days, locale)
    }
}

fun formatPeriodYMD(period: Period, locale: Locale = Locale.getDefault()): String {
    val years = abs(period.years)
    val months = abs(period.months)
    val days = abs(period.days)
    return localizedDuration(
        years = years.toLong(),
        months = months.toLong(),
        days = days.toLong(),
        locale = locale
    )
}

fun formatBetweenAsYMD(start: LocalDate, end: LocalDate, locale: Locale = Locale.getDefault()): String {
    return formatPeriodYMD(Period.between(start, end), locale)
}
