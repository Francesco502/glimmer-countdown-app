package com.example.timeapk.ui.utils

import android.content.Context
import com.example.timeapk.R
import java.time.LocalDate
import java.time.Period

/**
 * 生日详情区块：岁数、属相、八字、五行、星座。
 * 农历日期与岁次复用 RepeatDetailHelper.formatLunarLine / formatLunarMonthDay。
 */

/** 周岁：若今年未过生日则减 1 */
fun ageInYears(birthDate: LocalDate, today: LocalDate): Int {
    var age = today.year - birthDate.year
    if (today.monthValue < birthDate.monthValue || (today.monthValue == birthDate.monthValue && today.dayOfMonth < birthDate.dayOfMonth)) {
        age--
    }
    return kotlin.math.max(0, age)
}

/** 年龄的完整周期：岁、月、日（用于显示「27岁3月15日」） */
fun agePeriod(birthDate: LocalDate, today: LocalDate): Period {
    val safeToday = if (birthDate.isAfter(today)) birthDate else today
    return Period.between(birthDate, safeToday)
}

/** 星座（公历月日），支持 i18n */
fun constellationFromDate(date: LocalDate, context: Context? = null): String {
    val m = date.monthValue
    val d = date.dayOfMonth
    val index = when {
        m == 1 && d >= 20 || m == 2 && d <= 18 -> 0  // Aquarius
        m == 2 && d >= 19 || m == 3 && d <= 20 -> 1  // Pisces
        m == 3 && d >= 21 || m == 4 && d <= 19 -> 2  // Aries
        m == 4 && d >= 20 || m == 5 && d <= 20 -> 3  // Taurus
        m == 5 && d >= 21 || m == 6 && d <= 21 -> 4  // Gemini
        m == 6 && d >= 22 || m == 7 && d <= 22 -> 5  // Cancer
        m == 7 && d >= 23 || m == 8 && d <= 22 -> 6  // Leo
        m == 8 && d >= 23 || m == 9 && d <= 22 -> 7  // Virgo
        m == 9 && d >= 23 || m == 10 && d <= 23 -> 8  // Libra
        m == 10 && d >= 24 || m == 11 && d <= 22 -> 9  // Scorpio
        m == 11 && d >= 23 || m == 12 && d <= 21 -> 10 // Sagittarius
        else -> 11 // Capricorn
    }
    if (context != null) {
        val resIds = intArrayOf(
            R.string.constellation_aquarius, R.string.constellation_pisces,
            R.string.constellation_aries, R.string.constellation_taurus,
            R.string.constellation_gemini, R.string.constellation_cancer,
            R.string.constellation_leo, R.string.constellation_virgo,
            R.string.constellation_libra, R.string.constellation_scorpio,
            R.string.constellation_sagittarius, R.string.constellation_capricorn
        )
        return context.getString(resIds[index])
    }
    val names = listOf(
        "水瓶座", "双鱼座", "白羊座", "金牛座", "双子座", "巨蟹座",
        "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座"
    )
    return names[index]
}

/** 生肖（属相），由农历年；无库时返回 null */
fun zodiacAnimalFromDate(date: LocalDate): String? {
    return try {
        val clazz = Class.forName("com.nlf.calendar.Solar")
        val ctor = clazz.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val solar = ctor.newInstance(date.year, date.monthValue, date.dayOfMonth)
        val getLunar = clazz.getMethod("getLunar")
        val lunar = getLunar.invoke(solar) ?: return null
        val m = lunar.javaClass.getMethod("getYearShengXiao")
        m.invoke(lunar) as? String
    } catch (_: Throwable) {
        null
    }
}

/** 生辰八字（年月日三柱，未设置时辰则无时柱）；无库时返回 null */
fun baziFromDate(date: LocalDate): String? {
    return try {
        val clazz = Class.forName("com.nlf.calendar.Solar")
        val ctor = clazz.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val solar = ctor.newInstance(date.year, date.monthValue, date.dayOfMonth)
        val getLunar = clazz.getMethod("getLunar")
        val lunar = getLunar.invoke(solar) ?: return null
        val l = lunar.javaClass
        val yearGanZhi = l.getMethod("getYearInGanZhi").invoke(lunar) as? String ?: return null
        val monthGanZhi = l.getMethod("getMonthInGanZhi").invoke(lunar) as? String ?: return null
        val dayGanZhi = l.getMethod("getDayInGanZhi").invoke(lunar) as? String ?: return null
        "$yearGanZhi $monthGanZhi $dayGanZhi"
    } catch (_: Throwable) {
        null
    }
}

/** 正五行（结合地支藏干）；无库时返回 null */
fun wuxingFromDate(date: LocalDate): String? {
    val bazi = baziFromDate(date) ?: return null
    // bazi format: "年干支 月干支 日干支" e.g., "甲子 丙寅 戊戌"
    val tianGanWuXing = mapOf(
        '甲' to "木", '乙' to "木",
        '丙' to "火", '丁' to "火",
        '戊' to "土", '己' to "土",
        '庚' to "金", '辛' to "金",
        '壬' to "水", '癸' to "水"
    )

    val diZhiCangGanWuXing = mapOf(
        '寅' to listOf("木", "火", "土"),
        '卯' to listOf("木"),
        '辰' to listOf("土", "木", "水"),
        '巳' to listOf("火", "金", "土"),
        '午' to listOf("火", "土"),
        '未' to listOf("土", "火", "木"),
        '申' to listOf("金", "水", "土"),
        '酉' to listOf("金"),
        '戌' to listOf("土", "金", "火"),
        '亥' to listOf("水", "木"),
        '子' to listOf("水"),
        '丑' to listOf("土", "水", "金")
    )

    val wuxingSet = mutableSetOf<String>()
    for (char in bazi) {
        if (tianGanWuXing.containsKey(char)) {
            wuxingSet.add(tianGanWuXing[char]!!)
        } else if (diZhiCangGanWuXing.containsKey(char)) {
            wuxingSet.addAll(diZhiCangGanWuXing[char]!!)
        }
    }

    val allWuxing = listOf("金", "木", "水", "火", "土")
    val missing = allWuxing.filter { !wuxingSet.contains(it) }
    val missingStr = if (missing.isEmpty()) "五行俱全" else "缺${missing.joinToString("")}"

    val pillars = bazi.split(" ")
    val dayMaster = if (pillars.size >= 3 && pillars[2].isNotEmpty()) pillars[2][0] else null
    val masterWuxing = dayMaster?.let { tianGanWuXing[it] }

    return if (masterWuxing != null) {
        "属$masterWuxing ($missingStr)"
    } else {
        val presentStr = allWuxing.filter { wuxingSet.contains(it) }.joinToString("")
        "$presentStr ($missingStr)"
    }
}
