package com.example.timeapk

import java.io.File

/**
 * 4.2 split the home screen into HomeScreen / HomeEventCards / HomeMonthCalendar.
 * Source-contract tests read them concatenated in that order, so section anchors
 * (EmptyState -> EventCard -> EventListItem -> CompactEventTime -> MonthCalendarView) still hold.
 */
object HomeSourceFiles {
    private val files = listOf("HomeScreen.kt", "HomeEventCards.kt", "HomeMonthCalendar.kt")

    fun combined(): String = files.joinToString("\n") { name ->
        val relative = "com/example/timeapk/ui/home/$name"
        listOf(
            File("src/main/java/$relative"),
            File("app/src/main/java/$relative")
        ).firstOrNull(File::exists)?.readText(Charsets.UTF_8)
            ?: error("Missing home source: $name")
    }
}
