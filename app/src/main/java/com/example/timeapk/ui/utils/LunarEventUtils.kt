package com.example.timeapk.ui.utils

import android.content.Context
import com.example.timeapk.R
import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import java.time.LocalDate
import java.time.Period
import java.util.Locale
import kotlin.math.absoluteValue

private val GanZhiTransliterations = mapOf(
    '甲' to "Jia",
    '乙' to "Yi",
    '丙' to "Bing",
    '丁' to "Ding",
    '戊' to "Wu",
    '己' to "Ji",
    '庚' to "Geng",
    '辛' to "Xin",
    '壬' to "Ren",
    '癸' to "Gui",
    '子' to "Zi",
    '丑' to "Chou",
    '寅' to "Yin",
    '卯' to "Mao",
    '辰' to "Chen",
    '巳' to "Si",
    '午' to "Wu",
    '未' to "Wei",
    '申' to "Shen",
    '酉' to "You",
    '戌' to "Xu",
    '亥' to "Hai"
)

/**
 * 农历事件相关的核心计算工具。
 *
 * 约定：
 * - originSolarDate / today 均为公历 LocalDate。
 * - Event.date 始终存储公历 UTC 午夜毫秒数，isLunar 仅表示展示与重复逻辑按农历计算。
 */

/**
 * 获取「下次发生」的公历日期（含今天）。
 *
 * 对于农历事件，保持原始农历月日不变，在 today 当年/之后寻找下一次对应的公历日期。
 */
private fun lunarYearOf(date: LocalDate): Int =
    Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar.year

fun getNextLunarOccurrence(originSolarDate: LocalDate, today: LocalDate): LocalDate {
    // 若 today 早于起始日期，则直接返回起始公历日期，避免出现「出生前就有生日」的情况
    if (today.isBefore(originSolarDate)) {
        return originSolarDate
    }

    return try {
        val originSolar = Solar.fromYmd(
            originSolarDate.year,
            originSolarDate.monthValue,
            originSolarDate.dayOfMonth
        )
        val originLunar = originSolar.lunar
        val lunarMonth = originLunar.month  // 1..12，闰月为负
        val lunarDay = originLunar.day

        var year = lunarYearOf(today)
        // 安全上限，避免极端情况下的死循环
        repeat(100) {
            val candidate = buildLunarSolarDateForYear(year, lunarMonth, lunarDay)
            if (candidate != null && !candidate.isBefore(today)) {
                return candidate
            }
            year++
        }

        // 理论上不会走到这里，兜底返回起始日期
        originSolarDate
    } catch (_: Throwable) {
        // 库不可用或数据异常时，兜底使用简单公历逻辑
        if (!originSolarDate.isBefore(today)) originSolarDate else {
            originSolarDate.plusYears(
                (today.year - originSolarDate.year)
                    .coerceAtLeast(0)
                    .toLong()
            )
        }
    }
}

/**
 * 获取「上次发生」的公历日期（含今天）。
 *
 * 对于农历事件，保持原始农历月日不变，在 today 当年/之前寻找最近一次对应的公历日期。
 */
fun getPreviousLunarOccurrence(originSolarDate: LocalDate, today: LocalDate): LocalDate {
    if (today.isBefore(originSolarDate)) {
        return originSolarDate
    }

    return try {
        val originSolar = Solar.fromYmd(
            originSolarDate.year,
            originSolarDate.monthValue,
            originSolarDate.dayOfMonth
        )
        val originLunar = originSolar.lunar
        val lunarMonth = originLunar.month
        val lunarDay = originLunar.day
        val minYear = originLunar.year

        var year = lunarYearOf(today)
        repeat(100) {
            if (year < minYear) {
                return originSolarDate
            }
            val candidate = buildLunarSolarDateForYear(year, lunarMonth, lunarDay)
            if (candidate != null && !candidate.isAfter(today)) {
                return candidate
            }
            year--
        }

        originSolarDate
    } catch (_: Throwable) {
        originSolarDate
    }
}

/**
 * 计算农历语义下「已历多少年/月/日」。
 *
 * years 部分基于农历周年数，months/days 则基于最近一次农历周年到 today 的公历 Period。
 */
fun getLunarElapsedPeriod(originSolarDate: LocalDate, today: LocalDate): Period {
    if (today.isBefore(originSolarDate)) {
        return Period.ZERO
    }

    return try {
        val originSolar = Solar.fromYmd(
            originSolarDate.year,
            originSolarDate.monthValue,
            originSolarDate.dayOfMonth
        )
        val originLunar = originSolar.lunar

        val years = computeLunarYearsElapsed(originLunar, originSolarDate, today)
        if (years <= 0) {
            // 对于还未到首个农历周年的情况，直接返回公历差值即可
            return Period.between(originSolarDate, today)
        }

        // 找到「第 years 个农历周年」对应的公历日期，作为周期锚点
        val anchorDate = buildLunarSolarDateForYear(
            originLunar.year + years,
            originLunar.month,
            originLunar.day
        ) ?: return Period.between(originSolarDate, today)

        val tail = Period.between(anchorDate, today)
        Period.of(years, tail.months, tail.days)
    } catch (_: Throwable) {
        // 兜底：回退到简单公历差值
        Period.between(originSolarDate, today)
    }
}

fun formatLunarDateString(solarDate: LocalDate, context: Context? = null): String {
    return try {
        val solar = Solar.fromYmd(
            solarDate.year,
            solarDate.monthValue,
            solarDate.dayOfMonth
        )
        val lunar = solar.lunar
        val locale = context?.resources?.configuration?.locales?.get(0) ?: Locale.getDefault()
        val useEnglishText = locale.language == Locale.ENGLISH.language
        val ganZhiText = if (useEnglishText) {
            transliterateGanZhi(lunar.yearInGanZhi)
        } else {
            lunar.yearInGanZhi
        }
        val monthText = if (useEnglishText) {
            if (lunar.month < 0) {
                context?.getString(R.string.lunar_leap_month_number_format, lunar.month.absoluteValue)
                    ?: "${lunar.month.absoluteValue} (leap)"
            } else {
                lunar.month.toString()
            }
        } else {
            lunar.monthInChinese
        }
        val monthSuffix = if (useEnglishText) "" else context?.getString(R.string.lunar_month_suffix).orEmpty()
        val dayText = if (useEnglishText) lunar.day.toString() else lunar.dayInChinese
        if (context != null) {
            context.getString(
                R.string.lunar_date_full_format,
                ganZhiText,
                monthText,
                monthSuffix,
                dayText
            )
        } else if (useEnglishText) {
            "Ganzhi $ganZhiText · Lunar month $monthText, day $dayText"
        } else {
            "$ganZhiText $monthText $dayText"
        }
    } catch (_: Throwable) {
        if (context != null) {
            context.getString(
                R.string.lunar_date_fallback_format,
                solarDate.year,
                solarDate.monthValue,
                solarDate.dayOfMonth
            )
        } else {
            "${solarDate.year}-${solarDate.monthValue}-${solarDate.dayOfMonth}"
        }
    }
}

private fun transliterateGanZhi(value: String): String {
    val parts = value.map { character -> GanZhiTransliterations[character] ?: return value }
    return parts.joinToString("-")
}

/**
 * 尝试在给定公历 year 中构造指定农历月日（含闰月）的对应公历日期。
 *
 * @param year 公历年份，将作为农历年份使用
 * @param lunarMonth 农历月，1..12，闰月为负
 * @param lunarDay 农历日，1..31
 */
internal fun buildLunarSolarDateForYear(
    year: Int,
    lunarMonth: Int,
    lunarDay: Int
): LocalDate? {
    return try {
        val lunar: Lunar = Lunar.fromYmd(year, lunarMonth, lunarDay)
        val solar = lunar.solar
        LocalDate.of(solar.year, solar.month, solar.day)
    } catch (_: Throwable) {
        null
    }
}

/**
 * 计算从 originLunar 对应的起点到 today 为止，已经完整经历了多少个「农历周年」。
 */
private fun computeLunarYearsElapsed(
    originLunar: Lunar,
    originSolarDate: LocalDate,
    today: LocalDate
): Int {
    // 使用农历年差作为基准
    val todaySolar = Solar.fromYmd(today.year, today.monthValue, today.dayOfMonth)
    val todayLunar = todaySolar.lunar
    var years = todayLunar.year - originLunar.year
    if (years <= 0) return 0

    // 判断当前农历年的这次周年是否已经过完
    val thisYearAnniversary = buildLunarSolarDateForYear(
        todayLunar.year,
        originLunar.month,
        originLunar.day
    )

    if (thisYearAnniversary == null) {
        // 兜底：退化为简单公历逻辑，避免崩溃
        if (today.monthValue < originSolarDate.monthValue ||
            (today.monthValue == originSolarDate.monthValue &&
                today.dayOfMonth < originSolarDate.dayOfMonth)
        ) {
            years--
        }
    } else if (thisYearAnniversary.isAfter(today)) {
        years--
    }

    return years.coerceAtLeast(0)
}
