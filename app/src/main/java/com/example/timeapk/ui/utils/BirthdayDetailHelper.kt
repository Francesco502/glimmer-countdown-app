package com.example.timeapk.ui.utils

import android.content.Context
import com.example.timeapk.R
import java.time.LocalDate
import java.time.Period
import java.util.Locale

fun ageInYears(birthDate: LocalDate, today: LocalDate): Int {
    var age = today.year - birthDate.year
    if (today.monthValue < birthDate.monthValue || (today.monthValue == birthDate.monthValue && today.dayOfMonth < birthDate.dayOfMonth)) {
        age--
    }
    return kotlin.math.max(0, age)
}

fun agePeriod(birthDate: LocalDate, today: LocalDate): Period {
    val safeToday = if (birthDate.isAfter(today)) birthDate else today
    return Period.between(birthDate, safeToday)
}

fun constellationFromDate(date: LocalDate, context: Context? = null): String {
    val m = date.monthValue
    val d = date.dayOfMonth
    val index = when {
        m == 1 && d >= 20 || m == 2 && d <= 18 -> 0
        m == 2 && d >= 19 || m == 3 && d <= 20 -> 1
        m == 3 && d >= 21 || m == 4 && d <= 19 -> 2
        m == 4 && d >= 20 || m == 5 && d <= 20 -> 3
        m == 5 && d >= 21 || m == 6 && d <= 21 -> 4
        m == 6 && d >= 22 || m == 7 && d <= 22 -> 5
        m == 7 && d >= 23 || m == 8 && d <= 22 -> 6
        m == 8 && d >= 23 || m == 9 && d <= 22 -> 7
        m == 9 && d >= 23 || m == 10 && d <= 23 -> 8
        m == 10 && d >= 24 || m == 11 && d <= 22 -> 9
        m == 11 && d >= 23 || m == 12 && d <= 21 -> 10
        else -> 11
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
        "Aquarius", "Pisces", "Aries", "Taurus", "Gemini", "Cancer",
        "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn"
    )
    return names[index]
}

fun zodiacAnimalFromDate(date: LocalDate): String? {
    return try {
        val clazz = Class.forName("com.nlf.calendar.Solar")
        val ctor = clazz.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val solar = ctor.newInstance(date.year, date.monthValue, date.dayOfMonth)
        val getLunar = clazz.getMethod("getLunar")
        val lunar = getLunar.invoke(solar) ?: return null
        val method = lunar.javaClass.getMethod("getYearShengXiao")
        method.invoke(lunar) as? String
    } catch (_: Throwable) {
        null
    }
}

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

private enum class WuXingElement {
    METAL, WOOD, WATER, FIRE, EARTH
}

private fun WuXingElement.displayName(context: Context?): String {
    return when (this) {
        WuXingElement.METAL -> context?.getString(R.string.wuxing_element_metal) ?: "Metal"
        WuXingElement.WOOD -> context?.getString(R.string.wuxing_element_wood) ?: "Wood"
        WuXingElement.WATER -> context?.getString(R.string.wuxing_element_water) ?: "Water"
        WuXingElement.FIRE -> context?.getString(R.string.wuxing_element_fire) ?: "Fire"
        WuXingElement.EARTH -> context?.getString(R.string.wuxing_element_earth) ?: "Earth"
    }
}

fun wuxingFromDate(date: LocalDate, context: Context? = null): String? {
    val bazi = baziFromDate(date) ?: return null

    val tianGanWuXing = mapOf(
        '\u7532' to WuXingElement.WOOD, '\u4e59' to WuXingElement.WOOD,
        '\u4e19' to WuXingElement.FIRE, '\u4e01' to WuXingElement.FIRE,
        '\u620a' to WuXingElement.EARTH, '\u5df1' to WuXingElement.EARTH,
        '\u5e9a' to WuXingElement.METAL, '\u8f9b' to WuXingElement.METAL,
        '\u58ec' to WuXingElement.WATER, '\u7678' to WuXingElement.WATER
    )

    val diZhiCangGanWuXing = mapOf(
        '\u5bc5' to listOf(WuXingElement.WOOD, WuXingElement.FIRE, WuXingElement.EARTH),
        '\u536f' to listOf(WuXingElement.WOOD),
        '\u8fb0' to listOf(WuXingElement.EARTH, WuXingElement.WOOD, WuXingElement.WATER),
        '\u5df3' to listOf(WuXingElement.FIRE, WuXingElement.METAL, WuXingElement.EARTH),
        '\u5348' to listOf(WuXingElement.FIRE, WuXingElement.EARTH),
        '\u672a' to listOf(WuXingElement.EARTH, WuXingElement.FIRE, WuXingElement.WOOD),
        '\u7533' to listOf(WuXingElement.METAL, WuXingElement.WATER, WuXingElement.EARTH),
        '\u9149' to listOf(WuXingElement.METAL),
        '\u620c' to listOf(WuXingElement.EARTH, WuXingElement.METAL, WuXingElement.FIRE),
        '\u4ea5' to listOf(WuXingElement.WATER, WuXingElement.WOOD),
        '\u5b50' to listOf(WuXingElement.WATER),
        '\u4e11' to listOf(WuXingElement.EARTH, WuXingElement.WATER, WuXingElement.METAL)
    )

    val wuxingSet = mutableSetOf<WuXingElement>()
    for (char in bazi) {
        tianGanWuXing[char]?.let(wuxingSet::add)
        diZhiCangGanWuXing[char]?.let(wuxingSet::addAll)
    }

    val allWuxing = listOf(
        WuXingElement.METAL,
        WuXingElement.WOOD,
        WuXingElement.WATER,
        WuXingElement.FIRE,
        WuXingElement.EARTH
    )

    val locale = context?.resources?.configuration?.locales?.get(0) ?: Locale.getDefault()
    val joiner = if (locale.language.equals("zh", ignoreCase = true)) "" else ", "
    val missing = allWuxing.filter { it !in wuxingSet }
    val missingText = if (missing.isEmpty()) {
        context?.getString(R.string.wuxing_complete) ?: "all five present"
    } else {
        val missingNames = missing.joinToString(joiner) { it.displayName(context) }
        context?.getString(R.string.wuxing_missing_format, missingNames) ?: "missing $missingNames"
    }

    val pillars = bazi.split(" ")
    val dayMaster = if (pillars.size >= 3 && pillars[2].isNotEmpty()) pillars[2][0] else null
    val masterWuxing = dayMaster?.let { tianGanWuXing[it] }

    return if (masterWuxing != null) {
        context?.getString(
            R.string.wuxing_master_format,
            masterWuxing.displayName(context),
            missingText
        ) ?: "${masterWuxing.displayName(context)} ($missingText)"
    } else {
        val presentStr = allWuxing.filter { it in wuxingSet }.joinToString(joiner) { it.displayName(context) }
        context?.getString(R.string.wuxing_summary_format, presentStr, missingText)
            ?: "$presentStr ($missingText)"
    }
}
