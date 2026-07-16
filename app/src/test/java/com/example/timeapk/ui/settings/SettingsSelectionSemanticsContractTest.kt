package com.example.timeapk.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsSelectionSemanticsContractTest {
    @Test
    fun filterChipsExposeRadioSelectionWhileCustomActionRemainsAButton() {
        val componentSource = readProjectFile("app/src/main/java/com/example/timeapk/ui/theme/SongComponents.kt")
        val eventEntrySource = readProjectFile("app/src/main/java/com/example/timeapk/ui/event/EventEntryScreen.kt")
        val datePickerSource = readProjectFile("app/src/main/java/com/example/timeapk/ui/components/BottomSheetDatePicker.kt")
        val settingsSource = readProjectFile("app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt")

        val chip = componentSource.substringAfter("fun SongFilterChip(")
            .substringBefore("@Composable\nfun SongColorSwatch")

        assertTrue(chip.contains("selectionRole: Role? = Role.RadioButton"))
        assertTrue(chip.contains(".selectable("))
        assertTrue(chip.contains("selected = selected"))
        assertTrue(chip.contains("role = selectionRole"))
        assertTrue(eventEntrySource.contains("selectionRole = null"))

        val inkChoiceRow = eventEntrySource.substringAfter("private fun SongInkChoiceRow(")
            .substringBefore("@Composable\nfun SongColorSpectrumDialog")
        val reminderPresetRow = eventEntrySource.substringAfter("private fun <T> ReminderPresetChipRow(")
            .substringBefore("private fun formatRemindDaysBefore")
        val datePicker = datePickerSource.substringAfter("fun SongDateWheelPickerDialog(")
            .substringBefore("private fun DatePartField")
        val legacySettings = settingsSource.substringAfter("fun LegacyDisplaySettingsContent(")
            .substringBefore("@Composable\nfun DisplaySettingsContent")
        val reminderLeadTimeRow = settingsSource.substringAfter("private fun ReminderLeadTimePresetRow(")
            .substringBefore("@Composable\nfun DataSettingsContent")

        assertTrue(inkChoiceRow.contains(".selectableGroup()"))
        assertTrue(reminderPresetRow.contains(".selectableGroup()"))
        assertTrue(datePicker.contains(".selectableGroup()"))
        assertTrue(legacySettings.countOccurrences(".selectableGroup()") >= 2)
        assertTrue(reminderLeadTimeRow.contains(".selectableGroup()"))
    }

    @Test
    fun settingsSwitchesExposeCheckedStateThroughToggleableSemantics() {
        val componentSource = readProjectFile("app/src/main/java/com/example/timeapk/ui/settings/SettingsComponents.kt")
        val settingsSource = readProjectFile("app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt")

        val songToggle = componentSource.substringAfter("fun SongToggle(")
            .substringBefore("@Composable\nfun SettingsRadioRow")
        val classicalToggle = settingsSource.substringAfter("fun ClassicalToggle(")
            .substringBefore("private val PRESET_COLOR_HEX")

        listOf(songToggle, classicalToggle).forEach { toggle ->
            assertTrue(toggle.contains(".toggleable("))
            assertTrue(toggle.contains("value = checked"))
            assertTrue(toggle.contains("role = Role.Switch"))
            assertTrue(toggle.contains("onValueChange = onCheckedChange"))
            assertFalse(toggle.contains(".clickable("))
        }
    }

    @Test
    fun settingsRadioRowsExposeOneMergedSelectableTargetInsideExplicitGroups() {
        val componentSource = readProjectFile("app/src/main/java/com/example/timeapk/ui/settings/SettingsComponents.kt")
        val settingsSource = readProjectFile("app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt")
        val radioRow = componentSource.substringAfter("fun SettingsRadioRow(")
            .substringBefore("@Composable\nfun SettingsValueRow")

        assertTrue(radioRow.contains(".heightIn(min = 48.dp)"))
        assertTrue(radioRow.contains(".selectable("))
        assertTrue(radioRow.contains("selected = selected"))
        assertTrue(radioRow.contains("role = Role.RadioButton"))
        assertTrue(radioRow.contains(".semantics(mergeDescendants = true)"))
        assertTrue(radioRow.contains("onClick = null"))
        assertTrue(radioRow.contains(".clearAndSetSemantics"))
        assertFalse(radioRow.contains(".clickable("))

        val radioRowCalls = settingsSource.countOccurrences("SettingsRadioRow(")
        val explicitGroups = settingsSource.countOccurrences("SettingsRadioGroup")
        assertTrue(radioRowCalls == 12)
        assertTrue(explicitGroups == 9)
        assertTrue(componentSource.contains("fun SettingsRadioGroup("))
        assertTrue(componentSource.contains(".selectableGroup()"))
    }

    @Test
    fun reminderLeadTimePresetsAreGroupedAndCustomInputIsUnambiguous() {
        val settingsSource = readProjectFile("app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt")
        val presetRow = settingsSource.substringAfter("private fun ReminderLeadTimePresetRow(")
            .substringBefore("@Composable\nfun DataSettingsContent")
        val baseStrings = readProjectFile("app/src/main/res/values/strings.xml")
        val englishStrings = readProjectFile("app/src/main/res/values-en/strings.xml")

        assertTrue(presetRow.contains(".selectableGroup()"))
        assertTrue(settingsSource.countOccurrences("R.string.settings_reminder_custom_days_label") >= 3)
        assertTrue(settingsSource.countOccurrences("R.string.settings_reminder_custom_days_hint") >= 3)
        assertTrue(baseStrings.contains("name=\"settings_reminder_custom_days_label\""))
        assertTrue(baseStrings.contains("输入 0–3650 天"))
        assertTrue(englishStrings.contains("name=\"settings_reminder_custom_days_label\""))
        assertTrue(englishStrings.contains("Enter 0–3650 days"))
    }

    private fun String.countOccurrences(needle: String): Int =
        Regex(Regex.escape(needle)).findAll(this).count()

    private fun readProjectFile(path: String): String {
        val direct = File(path)
        if (direct.exists()) return direct.readText(Charsets.UTF_8)
        val fromApp = File("../$path")
        require(fromApp.exists()) { "Missing project file: $path" }
        return fromApp.readText(Charsets.UTF_8)
    }
}
