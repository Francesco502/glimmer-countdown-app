package com.example.timeapk.ui.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 将事件的毫秒时间戳转为 LocalDate。
 * DatePicker 返回的是 UTC 午夜，必须用 UTC 解释才能保证任何时区下日期不偏移。
 */
fun eventDateToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * 安全地将日期更换年份，处理闰日（如 2000-02-29）在非闰年时自动回退到 02-28。
 */
fun safeWithYear(date: LocalDate, year: Int): LocalDate? = try {
    date.withYear(year)
} catch (_: Exception) {
    try { LocalDate.of(year, date.monthValue, 28) } catch (_: Exception) { null }
}
