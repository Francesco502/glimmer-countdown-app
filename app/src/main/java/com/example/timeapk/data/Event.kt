package com.example.timeapk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 浜嬩欢鍒嗙被锛氱敓鏃?/ 绾康鏃?/ 鍏朵粬 */
const val CATEGORY_BIRTHDAY = "birthday"
const val CATEGORY_ANNIVERSARY = "anniversary"
const val CATEGORY_OTHER = "other"

/** 閲嶅绫诲瀷锛氭棤 / 姣忓勾 / 姣忓崐骞?/ 姣忔湀 */
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
    /** 閲嶅绫诲瀷锛歊EPEAT_* */
    val repeatType: String = REPEAT_NONE,
    /** 鎻愬墠鍑犲ぉ鎻愰啋锛?=褰撳ぉ */
    val remindDaysBefore: Int = 0,
    /** 褰撳ぉ鎻愰啋鏃跺埢锛堜粠 0 璧风殑鍒嗛挓鏁帮紝濡?480=8:00锛夛紝0=0:00 */
    val reminderTimeMinutesOfDay: Int = 480,
    /** 鏄惁寮€鍚彁閱?*/
    val remindEnabled: Boolean = false,
    /** 鏄惁灏嗘彁閱掑悓姝ュ埌绯荤粺鏃ョ▼锛堟棩鍘嗭級锛岀敱绯荤粺鍦ㄨ瀹氭椂闂撮€氳繃閫氱煡鏍忔彁閱?*/
    val syncToScheduleEnabled: Boolean = true,
    /** 宸插啓鍏ョ郴缁熸棩绋嬫椂瀵瑰簲鐨勪簨浠?ID锛岀敤浜庢洿鏂?鍒犻櫎 */
    val scheduleEventId: Long? = null,
    /** 鐩爣绯荤粺鏃ュ巻 ID锛堜负 null 鏃朵娇鐢ㄩ粯璁ゅ彲鍐欐棩鍘嗭級 */
    val targetCalendarId: Long? = null,
    /** 鏈€杩戜竴娆℃棩绋嬪悓姝ユ椂闂达紙姣锛? */
    val lastScheduleSyncAt: Long? = null,
    /** 鏈€杩戜竴娆℃棩绋嬪悓姝ラ敊璇俊鎭紝null 琛ㄧず姝ｅ父 */
    val lastScheduleSyncError: String? = null,
    /** 鍒涘缓鏃堕棿锛岀敤浜庢帓搴忎笌杩佺Щ */
    val createdAt: Long = System.currentTimeMillis(),
    /** 鏄惁涓哄啘鍘嗘棩鏈?*/
    val isLunar: Boolean = false
)
