package com.example.timeapk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 事件分类：生日 / 纪念日 / 其他 */
const val CATEGORY_BIRTHDAY = "birthday"
const val CATEGORY_ANNIVERSARY = "anniversary"
const val CATEGORY_OTHER = "other"

/** 重复类型：无 / 每天 / 每周 / 每年 / 每半年 / 每月 */
const val REPEAT_NONE = "none"
const val REPEAT_DAILY = "daily"
const val REPEAT_WEEKLY = "weekly"
const val REPEAT_YEARLY = "yearly"
const val REPEAT_HALF_YEARLY = "half_yearly"
const val REPEAT_MONTHLY = "monthly"

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: Long,
    val category: String,
    val note: String = "",
    val colorHex: String? = null,
    /** 重复类型：REPEAT_* */
    val repeatType: String = REPEAT_NONE,
    /** 提前几天提醒：0 表示当天 */
    val remindDaysBefore: Int = 0,
    /** 当天提醒时刻：从 0 开始的分钟数，例如 480 表示 8:00；0 表示 0:00 */
    val reminderTimeMinutesOfDay: Int = 480,
    /** 是否开启提醒 */
    val remindEnabled: Boolean = false,
    /** 是否将提醒同步到系统日程，由系统在设定时间通过通知栏提醒 */
    val syncToScheduleEnabled: Boolean = true,
    /** 已写入系统日程时对应的事件 ID，用于更新或删除 */
    val scheduleEventId: Long? = null,
    /** 目标系统日历 ID；为 null 时使用默认可写日历 */
    val targetCalendarId: Long? = null,
    /** 最近一次日程同步时间，单位毫秒 */
    val lastScheduleSyncAt: Long? = null,
    /** 最近一次日程同步错误信息；null 表示正常 */
    val lastScheduleSyncError: String? = null,
    /** 创建时间，用于排序与迁移 */
    val createdAt: Long = System.currentTimeMillis(),
    /** 是否为农历日期 */
    val isLunar: Boolean = false
)
