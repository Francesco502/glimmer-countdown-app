package com.example.timeapk.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeCalendarAccessibilityContractTest {
    @Test
    fun monthCalendarBuildsLocalizedFullDateAndStateDescriptionForEachDay() {
        val homeSource = projectFile("app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt")
        val baseStrings = projectFile("app/src/main/res/values/strings.xml")
        val englishStrings = projectFile("app/src/main/res/values-en/strings.xml")
        val chineseStrings = projectFile("app/src/main/res/values-zh/strings.xml")
        val calendar = homeSource.substringAfter("private fun MonthCalendarView(")
            .substringBefore("private fun CalendarOccurrenceRow(")

        assertTrue(calendar.contains("calendar_day_accessibility"))
        assertTrue(calendar.contains("calendar_day_today"))
        assertTrue(calendar.contains("calendar_day_selected"))
        assertTrue(calendar.contains("calendar_day_event_count"))
        assertTrue(calendar.contains("date.format(selectedDateFormatter)"))
        assertTrue(calendar.contains("contentDescription = dayAccessibilityLabel"))
        listOf(baseStrings, englishStrings, chineseStrings).forEach { strings ->
            assertTrue(strings.contains("name=\"calendar_day_accessibility\""))
            assertTrue(strings.contains("name=\"calendar_day_today\""))
            assertTrue(strings.contains("name=\"calendar_day_selected\""))
            assertTrue(strings.contains("name=\"calendar_day_event_count\""))
        }
    }

    private fun projectFile(path: String): String {
        val direct = File(path)
        if (direct.exists()) return direct.readText(Charsets.UTF_8)
        return File("../$path").readText(Charsets.UTF_8)
    }
}
