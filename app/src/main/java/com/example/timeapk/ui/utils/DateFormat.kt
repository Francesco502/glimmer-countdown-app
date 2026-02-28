package com.example.timeapk.ui.utils

import java.text.NumberFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 0=yyyy.MM.dd 1=yyyy-MM-dd，与 UserPreferencesRepository.dateFormatModeFlow 一致 */
const val DATE_FORMAT_DOT = 0
const val DATE_FORMAT_DASH = 1

/** 根据设置返回日期格式，供首页、详情、编辑页、小组件使用 */
fun getDisplayDateFormatter(mode: Int): DateTimeFormatter = when (mode) {
    DATE_FORMAT_DASH -> DateTimeFormatter.ofPattern("yyyy-MM-dd")
    else -> DateTimeFormatter.ofPattern("yyyy.MM.dd")
}

/** 默认格式（兼容未传 mode 的调用） */
val DisplayDateFormatter: DateTimeFormatter = getDisplayDateFormatter(DATE_FORMAT_DOT)

/** 天数展示：带千分位（如 1,000），随系统 locale 变化 */
fun formatDays(days: Long, locale: Locale = Locale.getDefault()): String {
    return NumberFormat.getIntegerInstance(locale).format(days)
}

/**
 * 将天数转换为「X年Y月Z天」的友好格式。
 * 规则：
 * - < 30天：直接显示「X天」
 * - 30天~1年：显示「X月Y天」（每月按30天估算）
 * - ≥1年：显示「X年Y月」或「X年Y天」（根据 useMonths 参数）
 */
fun formatDaysAsYMD(days: Long, useMonths: Boolean = true): String {
    val totalDays = kotlin.math.abs(days)
    if (totalDays < 30) {
        return "${totalDays}天"
    }

    val years = totalDays / 365
    val remainderAfterYears = totalDays % 365

    return if (years > 0) {
        // 有年份的情况
        if (useMonths) {
            val months = remainderAfterYears / 30
            if (months > 0) {
                "${years}年${months}个月"
            } else {
                "${years}年${remainderAfterYears}天"
            }
        } else {
            "${years}年${remainderAfterYears}天"
        }
    } else {
        // 不足一年，显示月+天
        val months = totalDays / 30
        val days = totalDays % 30
        if (days > 0) {
            "${months}个月${days}天"
        } else {
            "${months}个月"
        }
    }
}

/**
 * 智能选择天数显示格式：
 * - 超过 365 天 → 显示为「X年Y月」
 * - 点击可切换为精确天数（带千分位）
 */
fun formatDaysSmart(days: Long, showAsYMD: Boolean = false): String {
    return if (showAsYMD && kotlin.math.abs(days) >= 30) {
        formatDaysAsYMD(days)
    } else {
        formatDays(days)
    }
}

fun formatPeriodYMD(period: Period, locale: Locale = Locale.getDefault()): String {
    val years = kotlin.math.abs(period.years)
    val months = kotlin.math.abs(period.months)
    val days = kotlin.math.abs(period.days)
    val parts = mutableListOf<String>()
    return if (locale.language == Locale.CHINESE.language) {
        if (years > 0) parts.add("${years}年")
        if (months > 0) parts.add("${months}个月")
        if (days > 0 || parts.isEmpty()) parts.add("${days}天")
        parts.joinToString("")
    } else {
        if (years > 0) parts.add("${years}y")
        if (months > 0) parts.add("${months}mo")
        if (days > 0 || parts.isEmpty()) parts.add("${days}d")
        parts.joinToString(" ")
    }
}

fun formatBetweenAsYMD(start: LocalDate, end: LocalDate, locale: Locale = Locale.getDefault()): String {
    return formatPeriodYMD(Period.between(start, end), locale)
}
