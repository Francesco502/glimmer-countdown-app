package com.example.timeapk.ui.utils

import android.content.Context
import com.example.timeapk.R
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_YEARLY
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Period

/**
 * 重复事件详情页「缘起｜已历｜静候」六行展示所需文案与农历/干支。
 * 农历、干支优先使用 cn.6tail:lunar（com.nlf.calendar），不可用时回退到公历与简单干支。
 */

/** 公历日期 + 星期（逢周X） */
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
            else -> ""
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
            else -> ""
        }
    }
    val dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    return if (context != null) {
        context.getString(R.string.date_weekday_format, dateStr, weekday)
    } else {
        "$dateStr · 逢$weekday"
    }
}

/** 干支年（岁次XX）：由公历年份近似 */
fun formatGanZhiYear(year: Int): String {
    val tianGan = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val diZhi = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    val i = (year - 4).mod(10)
    val j = (year - 4).mod(12)
    return "岁次${tianGan[i]}${diZhi[j]}"
}

/** 农历月日（如 四月初九）。若未接入农历库则返回 null，由调用方显示占位。 */
fun formatLunarMonthDay(date: LocalDate): String? {
    return try {
        val clazz = Class.forName("com.nlf.calendar.Solar")
        val ctor = clazz.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val solar = ctor.newInstance(date.year, date.monthValue, date.dayOfMonth)
        val getLunar = clazz.getMethod("getLunar")
        val lunar = getLunar.invoke(solar) ?: return null
        val lunarClass = lunar.javaClass
        val getMonthInChinese = lunarClass.getMethod("getMonthInChinese")
        val getDayInChinese = lunarClass.getMethod("getDayInChinese")
        val month = getMonthInChinese.invoke(lunar) as? String ?: return null
        val day = getDayInChinese.invoke(lunar) as? String ?: return null
        "$month$day"
    } catch (_: Throwable) {
        null
    }
}

/** 公历日期对应的「岁次XX 农历月日」第二行；无农历时仅岁次。 */
fun formatLunarLine(date: LocalDate): String {
    val gz = formatGanZhiYear(date.year)
    val lunar = formatLunarMonthDay(date)
    return if (lunar != null) "$gz $lunar" else gz
}

/** 已历：文学化「X载X月又X日」(中文) / "X years, X months, X days" (英文) */
fun formatElapsedLiterary(period: Period, context: Context? = null): String {
    val y = kotlin.math.abs(period.years)
    val m = kotlin.math.abs(period.months)
    val d = kotlin.math.abs(period.days)

    val isEnglish = context != null &&
        context.resources.configuration.locales[0].language == "en"
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
    val dayCn = listOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十", "卅一")
    val dStr = when {
        d == 0 -> if (yStr == null && mStr == null) "〇日" else null
        d in 1..31 -> "又${dayCn[d]}日"
        else -> "又${d}日"
    }
    return listOfNotNull(yStr, mStr, dStr).joinToString("")
}

/** 已历第二行：「忽度 N 日」 */
fun formatElapsedDays(days: Long, context: Context? = null): String {
    val formatted = formatDays(days)
    return if (context != null) {
        context.getString(R.string.elapsed_days_format, formatted)
    } else {
        "忽度 $formatted 日"
    }
}

/** 计算下一次发生日（用于静候）；若今日即发生则返回 today。 */
fun nextOccurrenceDate(origin: LocalDate, today: LocalDate, repeatType: String): LocalDate {
    return when (repeatType) {
        REPEAT_YEARLY -> {
            var next = safeWithYear(origin, today.year) ?: origin
            if (next.isBefore(today)) next = safeWithYear(origin, today.year + 1) ?: origin
            next
        }
        REPEAT_HALF_YEARLY -> {
            if (origin.isBefore(today)) {
                val monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(origin, today)
                val periods = ((monthsBetween / 6) + 1) * 6
                var next = origin.plusMonths(periods)
                if (next.isBefore(today)) next = next.plusMonths(6)
                next
            } else origin
        }
        REPEAT_MONTHLY -> {
            if (origin.isBefore(today)) {
                val monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(origin, today)
                var next = origin.plusMonths(monthsBetween + 1)
                if (next.isBefore(today)) next = next.plusMonths(1)
                next
            } else origin
        }
        else -> origin
    }
}
