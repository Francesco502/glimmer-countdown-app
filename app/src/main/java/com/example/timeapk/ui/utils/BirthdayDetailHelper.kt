package com.example.timeapk.ui.utils

import android.content.Context
import com.example.timeapk.R
import com.nlf.calendar.Solar
import java.time.LocalDate
import java.time.Period

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
        Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
            .lunar
            .yearShengXiao
    } catch (_: Throwable) {
        null
    }
}
