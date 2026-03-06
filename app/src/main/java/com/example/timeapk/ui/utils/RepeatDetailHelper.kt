package com.example.timeapk.ui.utils

import android.content.Context
import com.example.timeapk.R
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

fun formatDateWithWeekday(date: LocalDate, context: Context? = null): String {
    val weekday = if (context != null) {
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> context.getString(R.string.weekday_mon)
            DayOfWeek.TUESDAY -> context.getString(R.string.weekday_tue)
            DayOfWeek.WEDNESDAY -> context.getString(R.string.weekday_wed)
            DayOfWeek.THURSDAY -> context.getString(R.string.weekday_thu)
            DayOfWeek.FRIDAY -> context.getString(R.string.weekday_fri)
            DayOfWeek.SATURDAY -> context.getString(R.string.weekday_sat)
            DayOfWeek.SUNDAY -> context.getString(R.string.weekday_sun)
        }
    } else {
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }
    }
    val dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    return "$dateStr · $weekday"
}

fun formatGanZhiYear(year: Int): String {
    val tianGan = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val diZhi = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    val i = (year - 4).mod(10)
    val j = (year - 4).mod(12)
    return "岁次${tianGan[i]}${diZhi[j]}"
}

fun formatLunarMonthDay(date: LocalDate): String? {
    return try {
        val clazz = Class.forName("com.nlf.calendar.Solar")
        val ctor = clazz.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val solar = ctor.newInstance(date.year, date.monthValue, date.dayOfMonth)
        val lunar = clazz.getMethod("getLunar").invoke(solar) ?: return null
        val lunarClass = lunar.javaClass
        val month = lunarClass.getMethod("getMonthInChinese").invoke(lunar) as? String ?: return null
        val day = lunarClass.getMethod("getDayInChinese").invoke(lunar) as? String ?: return null
        "$month$day"
    } catch (_: Throwable) {
        null
    }
}

fun formatLunarLine(date: LocalDate): String {
    val gz = formatGanZhiYear(date.year)
    val lunar = formatLunarMonthDay(date)
    return if (lunar != null) "$gz $lunar" else gz
}

fun formatElapsedLiterary(period: Period, context: Context? = null): String {
    val y = kotlin.math.abs(period.years)
    val m = kotlin.math.abs(period.months)
    val d = kotlin.math.abs(period.days)

    val isEnglish = context != null && context.resources.configuration.locales[0].language == "en"
    if (isEnglish) {
        val parts = mutableListOf<String>()
        if (y > 0) parts += "$y year${if (y > 1) "s" else ""}"
        if (m > 0) parts += "$m month${if (m > 1) "s" else ""}"
        if (d > 0) parts += "$d day${if (d > 1) "s" else ""}"
        if (parts.isEmpty()) parts += "0 days"
        return parts.joinToString(", ")
    }

    val yStr = when (y) {
        0 -> null
        1 -> "一载"
        2 -> "两载"
        else -> "${y}载"
    }
    val mStr = when (m) {
        0 -> null
        1 -> "一月"
        else -> "${m}月"
    }
    val dayCn = listOf(
        "", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十", "卅一"
    )
    val dStr = when {
        d == 0 -> if (yStr == null && mStr == null) "〇日" else null
        d in 1..31 -> "又${dayCn[d]}日"
        else -> "又${d}日"
    }
    return listOfNotNull(yStr, mStr, dStr).joinToString("")
}

fun formatElapsedDays(days: Long, context: Context? = null): String {
    val formatted = formatDays(days)
    return if (context != null) {
        context.getString(R.string.elapsed_days_format, formatted)
    } else {
        "忽度 $formatted 日"
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

    val previousProbeDate = today.minusDays(1)
    if (previousProbeDate.isBefore(origin)) return origin
    return nextOccurrenceDate(origin, previousProbeDate, repeatType)
}
