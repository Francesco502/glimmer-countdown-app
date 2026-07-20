package com.example.timeapk.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AccessibilitySemanticsSourceContractTest {
    @Test
    fun widgetSwitchRowOwnsTheOnlySwitchAction() {
        val widgetSource = projectFile("app/src/main/java/com/example/timeapk/ui/settings/WidgetSettingsContent.kt")
        val componentSource = projectFile("app/src/main/java/com/example/timeapk/ui/settings/SettingsComponents.kt")
        val row = widgetSource.substringAfter("fun WidgetSwitchRow(")
            .substringBefore("private fun widgetConfigSummary(")
        val toggle = componentSource.substringAfter("fun SongToggle(")
            .substringBefore("@Composable\nfun SettingsRadioRow")

        assertTrue(row.contains(".toggleable("))
        assertTrue(row.contains("role = Role.Switch"))
        assertTrue(row.contains(".semantics(mergeDescendants = true)"))
        assertTrue(row.contains("onCheckedChange = null"))
        assertFalse(row.contains(".clickable"))
        assertTrue(toggle.contains("onCheckedChange: ((Boolean) -> Unit)?"))
        assertTrue(toggle.contains(".clearAndSetSemantics"))
    }

    @Test
    fun detailTimeDisplayOwnsLocalizedValueModeAndCycleActionSemantics() {
        val detailSource = projectFile("app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt")
        val hero = detailSource.substringAfter("private fun DetailHeroCard(")
            .substringBefore("private fun DetailSupplementSections(")
        val baseStrings = projectFile("app/src/main/res/values/strings.xml")
        val englishStrings = projectFile("app/src/main/res/values-en/strings.xml")
        val chineseStrings = projectFile("app/src/main/res/values-zh/strings.xml")

        assertTrue(hero.contains(".clearAndSetSemantics"))
        assertTrue(hero.contains("contentDescription = timeDisplayDescription"))
        assertTrue(hero.contains("stateDescription = timeDisplayModeDescription"))
        assertTrue(hero.contains("role = Role.Button"))
        assertTrue(hero.contains("onClick(label = cycleTimeDisplayDescription)"))
        listOf(baseStrings, englishStrings, chineseStrings).forEach { strings ->
            assertTrue(strings.contains("name=\"detail_time_mode_past_days\""))
            assertTrue(strings.contains("name=\"detail_time_mode_past_ymd\""))
            assertTrue(strings.contains("name=\"detail_time_mode_until_days\""))
            assertTrue(strings.contains("name=\"detail_time_mode_until_ymd\""))
            assertTrue(strings.contains("name=\"detail_time_mode_milestone\""))
        }
    }

    private fun projectFile(path: String): String {
        val direct = File(path)
        if (direct.exists()) return direct.readText(Charsets.UTF_8)
        return File("../$path").readText(Charsets.UTF_8)
    }
}
