package com.example.timeapk.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsStructureTest {
    @Test
    fun appearanceSettings_useCollapsedSectionsAndFontDialog() {
        val source = readSource("ui/settings/SettingsSubScreens.kt")
        val appearance = source.substringBetween(
            "fun AppearanceSettingsContent(",
            "@Composable\nfun LegacyDisplaySettingsContent"
        )

        assertTrue(appearance.countOccurrences("SettingsExpandableSection(") >= 4)
        assertTrue(appearance.contains("FontPresetPickerDialog("))
        assertTrue(appearance.contains("showFontPresetDialog"))
        assertFalse(appearance.contains("4 to stringResource(R.string.font_slender_gold)"))
    }

    @Test
    fun milestoneSettings_hideDenseDetailsBehindExpandableSections() {
        val source = readSource("ui/settings/SettingsSubScreens.kt")
        val milestone = source.substringBetween(
            "fun MilestoneSettingsContent(",
            "@Composable\nfun DataSettingsContent"
        )

        assertTrue(milestone.countOccurrences("SettingsExpandableSection(") >= 4)
        assertTrue(milestone.contains("settings_section_schedule_sync_title"))
        assertTrue(milestone.contains("settings_section_custom_milestones_title"))
    }

    @Test
    fun milestoneSettings_refreshScheduleSyncOnlyAfterSectionExpands() {
        val source = readSource("ui/settings/SettingsSubScreens.kt")
        val milestone = source.substringBetween(
            "fun MilestoneSettingsContent(",
            "@Composable\nfun DataSettingsContent"
        )

        assertFalse(milestone.contains("LaunchedEffect(Unit) {\n        refreshScheduleSyncStatus()"))
        assertTrue(milestone.contains("scheduleSyncStatusLoaded"))
        assertTrue(milestone.contains("onExpandedChange = { expanded ->"))
        assertTrue(milestone.contains("if (expanded && !scheduleSyncStatusLoaded)"))
    }

    @Test
    fun expandableSettingsSectionsExposeAccessibleToggleSemantics() {
        val source = readSource("ui/settings/SettingsComponents.kt")
        val section = source.substring(source.indexOf("fun SettingsExpandableSection("))

        assertTrue(section.contains("onExpandedChange: ((Boolean) -> Unit)? = null"))
        assertTrue(section.contains("role = Role.Button"))
        assertTrue(section.contains("stateDescription ="))
        assertTrue(section.contains("contentDescription = toggleContentDescription"))
    }

    private fun String.substringBetween(start: String, end: String): String {
        val startIndex = indexOf(start)
        val endIndex = indexOf(end, startIndex + start.length)
        require(startIndex >= 0) { "Missing start marker: $start" }
        require(endIndex > startIndex) { "Missing end marker: $end" }
        return substring(startIndex, endIndex)
    }

    private fun String.countOccurrences(needle: String): Int {
        return Regex(Regex.escape(needle)).findAll(this).count()
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
