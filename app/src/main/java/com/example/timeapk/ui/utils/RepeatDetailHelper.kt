package com.example.timeapk.ui.utils

import android.content.Context
import com.example.timeapk.R
import com.nlf.calendar.Solar
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

fun formatDateWithWeekday(date: LocalDate, context: Context? = null): String {
    val locale = context?.resources?.configuration?.locales?.get(0) ?: Locale.getDefault()
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd", locale))
    return if (context != null) {
        context.getString(R.string.date_weekday_format, dateStr, weekday)
    } else {
        "$dateStr · $weekday"
    }
}

fun formatGanZhiYear(year: Int, context: Context? = null): String {
    val tianGan = listOf(
        "\u7532", "\u4e59", "\u4e19", "\u4e01", "\u620a",
        "\u5df1", "\u5e9a", "\u8f9b", "\u58ec", "\u7678"
    )
    val diZhi = listOf(
        "\u5b50", "\u4e11", "\u5bc5", "\u536f", "\u8fb0", "\u5df3",
        "\u5348", "\u672a", "\u7533", "\u9149", "\u620c", "\u4ea5"
    )
    val i = (year - 4).mod(10)
    val j = (year - 4).mod(12)
    val ganZhi = "${tianGan[i]}${diZhi[j]}"
    return if (context != null) {
        context.getString(R.string.ganzhi_year_format, ganZhi)
    } else {
        ganZhi
    }
}

fun formatLunarMonthDay(date: LocalDate): String? {
    return try {
        val lunar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
        "${lunar.monthInChinese}${lunar.dayInChinese}"
    } catch (_: Throwable) {
        null
    }
}

fun formatLunarLine(date: LocalDate, context: Context? = null): String {
    val gz = formatGanZhiYear(date.year, context)
    val lunar = formatLunarMonthDay(date)
    return if (lunar != null) "$gz $lunar" else gz
}

fun formatElapsedLiterary(period: Period, context: Context? = null): String {
    val locale = context?.resources?.configuration?.locales?.get(0) ?: Locale.getDefault()
    return formatPeriodYMD(period, locale)
}

fun formatElapsedDays(days: Long, context: Context? = null): String {
    val formatted = formatDays(days)
    return if (context != null) {
        context.getString(R.string.elapsed_days_format, formatted)
    } else {
        formatted
    }
}

fun nextOccurrenceDate(origin: LocalDate, today: LocalDate, repeatType: String): LocalDate {
    return when (repeatType) {
        REPEAT_DAILY -> if (origin.isAfter(today)) origin else today

        REPEAT_WEEKLY -> {
            if (origin.isAfter(today)) {
                origin
            } else {
                val daysBetween = ChronoUnit.DAYS.between(origin, today)
                var weeksToAdd = daysBetween / 7
                var next = origin.plusWeeks(weeksToAdd)
                while (next.isBefore(today)) {
                    weeksToAdd += 1
                    next = origin.plusWeeks(weeksToAdd)
                }
                next
            }
        }

        REPEAT_YEARLY -> {
            var next = safeWithYear(origin, today.year) ?: origin
            if (next.isBefore(today)) next = safeWithYear(origin, today.year + 1) ?: origin
            next
        }

        REPEAT_HALF_YEARLY -> {
            if (origin.isAfter(today)) {
                origin
            } else {
                val monthsBetween = ChronoUnit.MONTHS.between(origin, today)
                var halfYearsToAdd = monthsBetween / 6
                var next = origin.plusMonths(halfYearsToAdd * 6)
                while (next.isBefore(today)) {
                    halfYearsToAdd += 1
                    next = origin.plusMonths(halfYearsToAdd * 6)
                }
                next
            }
        }

        REPEAT_MONTHLY -> {
            if (origin.isAfter(today)) {
                origin
            } else {
                val monthsBetween = ChronoUnit.MONTHS.between(origin, today)
                var monthsToAdd = monthsBetween
                var next = origin.plusMonths(monthsToAdd)
                while (next.isBefore(today)) {
                    monthsToAdd += 1
                    next = origin.plusMonths(monthsToAdd)
                }
                next
            }
        }

        else -> origin
    }
}

fun previousOccurrenceDate(origin: LocalDate, today: LocalDate, repeatType: String): LocalDate? {
    if (origin.isAfter(today)) return null

    val nextOnOrAfterToday = nextOccurrenceDate(origin, today, repeatType)
    if (!nextOnOrAfterToday.isAfter(today)) return nextOnOrAfterToday

    // Subtract one repeat interval from the next occurrence to get the previous one.
    return when (repeatType) {
        REPEAT_DAILY -> today
        REPEAT_WEEKLY -> nextOnOrAfterToday.minusWeeks(1)
        REPEAT_MONTHLY -> previousMonthAnchoredOccurrence(origin, today, monthsPerRepeat = 1)
        REPEAT_HALF_YEARLY -> previousMonthAnchoredOccurrence(origin, today, monthsPerRepeat = 6)
        REPEAT_YEARLY -> previousMonthAnchoredOccurrence(origin, today, monthsPerRepeat = 12)
        else -> origin
    }
}

private fun previousMonthAnchoredOccurrence(
    origin: LocalDate,
    today: LocalDate,
    monthsPerRepeat: Long
): LocalDate {
    val monthOffset = ChronoUnit.MONTHS.between(
        origin.withDayOfMonth(1),
        today.withDayOfMonth(1)
    )
    var repeatCount = monthOffset / monthsPerRepeat
    var candidate = origin.plusMonths(repeatCount * monthsPerRepeat)
    if (candidate.isAfter(today)) {
        repeatCount -= 1
        candidate = origin.plusMonths(repeatCount * monthsPerRepeat)
    }
    return candidate
}
