package com.example.timeapk.ui.utils

import android.content.Context
import com.example.timeapk.R
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.ui.home.EventUiState
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.example.timeapk.ui.utils.getNextLunarOccurrence
import com.example.timeapk.ui.utils.getLunarElapsedPeriod

object DisplayModes {
    const val PAST_DAYS = 0
    const val PAST_YMD = 1
    const val UNTIL_DAYS = 2
    const val UNTIL_YMD = 3
    const val MILESTONE = 4
}

fun getAvailableDisplayModes(eventState: EventUiState, showMilestone: Boolean): List<Int> {
    val category = eventState.event.category
    val isRepeating = eventState.event.repeatType != REPEAT_NONE
    val isPast = eventState.isPast
    val hasMilestone = showMilestone && eventState.nextMilestoneDays != null && eventState.nextMilestoneValue != null
    val targetLocalDate = com.example.timeapk.ui.utils.eventDateToLocalDate(eventState.event.date)
    val hasStarted = !targetLocalDate.isAfter(java.time.LocalDate.now())

    val modes = mutableListOf<Int>()

    if (!hasStarted) {
        // 未开始的事件（无论是单次还是重复），只显示倒计时
        modes.addAll(listOf(DisplayModes.UNTIL_DAYS, DisplayModes.UNTIL_YMD))
    } else if (isRepeating) {
        // 已经开始的重复事件
        when (category) {
            CATEGORY_ANNIVERSARY -> {
                // 纪念日重复：已经天数 -> 已经年月天 -> 还有天数 -> 还有年月天
                modes.addAll(listOf(DisplayModes.PAST_DAYS, DisplayModes.PAST_YMD, DisplayModes.UNTIL_DAYS, DisplayModes.UNTIL_YMD))
            }
            CATEGORY_BIRTHDAY -> {
                // 生日重复：还有天数 -> 还有年月天 -> 已经天数 -> 已经年月天
                modes.addAll(listOf(DisplayModes.UNTIL_DAYS, DisplayModes.UNTIL_YMD, DisplayModes.PAST_DAYS, DisplayModes.PAST_YMD))
            }
            else -> { // CATEGORY_OTHER
                // 其他重复：只看还有（例如房租）
                modes.addAll(listOf(DisplayModes.UNTIL_DAYS, DisplayModes.UNTIL_YMD))
            }
        }
    } else {
        // 已经开始的单次事件
        if (isPast) {
            modes.addAll(listOf(DisplayModes.PAST_DAYS, DisplayModes.PAST_YMD))
        } else {
            // isPast 为 false 但 hasStarted 为 true，说明就是今天
            modes.addAll(listOf(DisplayModes.UNTIL_DAYS, DisplayModes.UNTIL_YMD))
        }
    }

    if (hasMilestone) {
        modes.add(DisplayModes.MILESTONE)
    }

    // 默认回退（理论上不会走到）
    return if (modes.isEmpty()) listOf(DisplayModes.PAST_DAYS) else modes
}

/**
 * 获取倒计时模式下的标签文案。
 * 如果是重复事件，会明确指出“距离 第X年”或“距离 X岁生日”。
 */
fun getUntilLabel(context: Context, eventState: EventUiState): String {
    val repeatType = eventState.event.repeatType
    val category = eventState.event.category
    val isLunar = eventState.event.isLunar
    
    if (repeatType == REPEAT_NONE) {
        return context.getString(R.string.days_until_label)
    }

    val originDate = eventDateToLocalDate(eventState.event.date)
    val today = LocalDate.now()
    if (!originDate.isBefore(today)) {
        return context.getString(R.string.days_until_label)
    }

    val nextDate = if (isLunar && repeatType == REPEAT_YEARLY) {
        getNextLunarOccurrence(originDate, today)
    } else {
        nextOccurrenceDate(originDate, today, repeatType)
    }
    
    when (category) {
        CATEGORY_ANNIVERSARY -> {
            if (repeatType == REPEAT_YEARLY) {
                val years = if (isLunar) {
                    val period = getLunarElapsedPeriod(originDate, nextDate.minusDays(1))
                    period.years + 1
                } else {
                    nextDate.year - originDate.year
                }
                if (years > 0) return context.getString(R.string.until_anniversary_label, years)
            } else if (repeatType == REPEAT_MONTHLY) {
                val months = ChronoUnit.MONTHS.between(originDate.withDayOfMonth(1), nextDate.withDayOfMonth(1))
                if (months > 0) return context.getString(R.string.until_month_anniversary_label, months)
            } else if (repeatType == REPEAT_HALF_YEARLY) {
                val halfYears = ChronoUnit.MONTHS.between(originDate.withDayOfMonth(1), nextDate.withDayOfMonth(1)) / 6
                if (halfYears > 0) return context.getString(R.string.until_half_year_anniversary_label, halfYears)
            }
        }
        CATEGORY_BIRTHDAY -> {
            if (repeatType == REPEAT_YEARLY) {
                val years = if (isLunar) {
                    val period = getLunarElapsedPeriod(originDate, nextDate.minusDays(1))
                    period.years + 1
                } else {
                    nextDate.year - originDate.year
                }
                if (years > 0) {
                    return if (isLunar) {
                        context.getString(R.string.until_lunar_birthday_label, years)
                    } else {
                        context.getString(R.string.until_birthday_label, years)
                    }
                }
            }
        }
    }
    
    return context.getString(R.string.days_until_label)
}
