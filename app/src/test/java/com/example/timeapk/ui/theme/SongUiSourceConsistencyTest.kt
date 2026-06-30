package com.example.timeapk.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SongUiSourceConsistencyTest {
    @Test
    fun homeScreen_usesSongWrappedControlsAndAnimationSpecs() {
        val source = readSource("ui/home/HomeScreen.kt")

        assertFalse(source.contains("SingleChoiceSegmentedButtonRow"))
        assertFalse(source.contains("SegmentedButton("))
        assertFalse(Regex("(?m)^\\s*FilterChip\\(").containsMatchIn(source))
        assertFalse(source.contains("spring(dampingRatio = 0.7f"))
        assertFalse(source.contains("tween(durationMillis = 350"))
    }

    @Test
    fun homeDisplayModes_useLightweightSongTabs() {
        val homeSource = readSource("ui/home/HomeScreen.kt")
        val componentSource = readSource("ui/theme/SongComponents.kt")
        val displayModeBlock = homeSource.substringAfter("private fun HomeDisplayModeSegmentedControl")
            .substringBefore("@Composable\nprivate fun InlineActionIconButton")

        assertTrue(displayModeBlock.contains("SongModeTabRow("))
        assertFalse(displayModeBlock.contains("SongSegmentedControl("))
        assertTrue(componentSource.contains("fun <T> SongModeTabRow("))
        assertTrue(componentSource.contains(".height(32.dp)"))
        assertTrue(componentSource.contains(".height(1.dp)"))
    }

    @Test
    fun homeListMode_usesBookListRhythmInsteadOfCardRhythm() {
        val source = readSource("ui/home/HomeScreen.kt")
        val listBlock = source.substringAfter("private fun EventListItem(")
            .substringBefore("private fun eventTitleForCalendar")

        assertTrue(listBlock.contains(".heightIn(min = 64.dp)"))
        assertTrue(listBlock.contains(".width(2.dp)"))
        assertTrue(listBlock.contains(".height(18.dp)"))
        assertTrue(listBlock.contains("HorizontalDivider("))
        assertFalse(listBlock.contains(".heightIn(min = 88.dp)"))
        assertFalse(listBlock.contains(".height(40.dp)"))
    }

    @Test
    fun monthCalendar_usesLighterBookCalendarTreatment() {
        val homeSource = readSource("ui/home/HomeScreen.kt")
        val componentSource = readSource("ui/theme/SongComponents.kt")
        val monthBlock = homeSource.substringAfter("private fun MonthCalendarView(")

        assertTrue(monthBlock.contains(".height(60.dp)"))
        assertTrue(monthBlock.contains("CalendarOccurrenceRow("))
        assertFalse(monthBlock.contains("SongPaperSurface("))
        assertTrue(componentSource.contains("Color.Transparent"))
        assertTrue(componentSource.contains("selected -> SongPalette.PaperDeep.copy(alpha = 0.54f)"))
        assertTrue(componentSource.contains("hasEvents -> SongPalette.PaperWarm.copy(alpha = 0.45f)"))
    }

    private fun readSource(relative: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relative")
        if (direct.exists()) {
            return direct.readText(Charsets.UTF_8)
        }
        val fromRoot = File("app/src/main/java/com/example/timeapk/$relative")
        require(fromRoot.exists()) { "Missing source file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }
}
