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

    @Test
    fun songSharedUiComponentsProvide315PolishBuildingBlocks() {
        val source = readSource("ui/common/SongUiComponents.kt")

        assertTrue(source.contains("fun SongSectionHeader("))
        assertTrue(source.contains("fun SongReminderStatusStrip("))
        assertTrue(source.contains("fun SongEventPreviewCard("))
        assertTrue(source.contains("fun SongBottomActionBar("))
        assertTrue(source.contains("outlined: Boolean = true"))
        assertTrue(source.contains("fun SongMiniPreviewSurface("))
        assertTrue(source.contains("contentDescription"))
        assertTrue(source.contains("Role.Button"))
    }

    @Test
    fun detailScreenUses315ReminderStatusAndBottomActions() {
        val source = readSource("ui/detail/DetailScreen.kt")

        assertTrue(source.contains("SongReminderStatusStrip("))
        assertTrue(source.contains("buildReminderStatus("))
        assertTrue(source.contains("SongBottomActionBar("))
    }

    @Test
    fun detailScreenRoutesReminderStatusActionsToMatchingDestinations() {
        val source = readSource("ui/detail/DetailScreen.kt")
        val screenBlock = source.substringBetween(
            "fun DetailScreen(",
            "private data class DetailTimeDisplay("
        )
        val supplementBlock = source.substringBetween(
            "private fun DetailSupplementSections(",
            "@Composable\nprivate fun DetailBottomActions("
        )

        assertTrue(screenBlock.contains("detailReminderStatusAction("))
        assertTrue(supplementBlock.contains("onActionClick = onReminderActionClick"))
        assertTrue(source.contains("ReminderStatusAction.OpenNotificationSettings ->"))
        assertTrue(source.contains("context.openAppNotificationSettings()"))
        assertTrue(source.contains("ReminderStatusAction.OpenCalendarSettings ->"))
        assertTrue(source.contains("context.openAppDetailsSettings()"))
        assertFalse(screenBlock.contains("ReminderStatusAction.None }?.let {\n                            { onEditClick() }"))
    }

    @Test
    fun detailScreenSeparatesCoreHeroCardFromSupplementalSections() {
        val source = readSource("ui/detail/DetailScreen.kt")
        val heroBlock = source.substringBetween(
            "private fun DetailHeroCard(",
            "@Composable\nprivate fun DetailSupplementSections("
        )
        val supplementBlock = source.substringBetween(
            "private fun DetailSupplementSections(",
            "@Composable\nprivate fun DetailBottomActions("
        )

        assertTrue(heroBlock.contains("SongPaperSurface("))
        assertTrue(heroBlock.contains("val titleStyle = when"))
        assertTrue(heroBlock.contains("val daysStyle = when"))
        assertFalse(heroBlock.contains("calendarMetaLine"))
        assertFalse(heroBlock.contains("SongDetailNoteBlock("))
        assertFalse(heroBlock.contains("SongReminderStatusStrip("))
        assertFalse(heroBlock.contains("DetailLabelRow("))
        assertFalse(heroBlock.contains("detail_repeat_origin"))
        assertFalse(heroBlock.contains("detail_birthday_age"))

        assertTrue(supplementBlock.contains("SongReminderStatusStrip("))
        assertFalse(supplementBlock.contains("SongDetailNoteBlock("))
        assertTrue(supplementBlock.contains("DetailLabelRow("))
        assertTrue(supplementBlock.contains("calendarMetaLine"))
        assertTrue(supplementBlock.contains("detail_repeat_origin"))
        assertTrue(supplementBlock.contains("DetailLabelRow(stringResource(R.string.field_note)"))
        assertTrue(source.contains("eventState.event.syncToScheduleEnabled"))
        assertFalse(supplementBlock.contains("Text(\n                            text = eventState.event.note"))
    }

    @Test
    fun detailScreenHeroDateAndNodeLabelUseDetailTextScale() {
        val source = readSource("ui/detail/DetailScreen.kt")
        val heroBlock = source.substringBetween(
            "private fun DetailHeroCard(",
            "@Composable\nprivate fun DetailSupplementSections("
        )

        assertTrue(heroBlock.contains("text = dateStr,\n                    style = MaterialTheme.typography.bodyLarge"))
        assertTrue(heroBlock.contains("text = timeDisplay.label,\n                        style = MaterialTheme.typography.bodyLarge"))
        assertFalse(heroBlock.contains("style = MaterialTheme.typography.bodyMedium"))
        assertFalse(heroBlock.contains("style = MaterialTheme.typography.titleLarge"))
    }

    @Test
    fun detailScreenBirthdayRowsUseSpacedAgeAndDescriptiveZodiacText() {
        val source = readSource("ui/detail/DetailScreen.kt")
        val supplementBlock = source.substringBetween(
            "private fun DetailSupplementSections(",
            "@Composable\nprivate fun DetailSupplementTable("
        )

        assertTrue(supplementBlock.contains("R.string.detail_birthday_age_format_ymd"))
        assertTrue(supplementBlock.contains("zodiacDisplayText("))
        assertTrue(supplementBlock.contains("constellationDisplayText("))
        assertFalse(supplementBlock.contains("valueParts = detailValueParts(ageYmd)"))
        assertFalse(supplementBlock.contains("valueParts = detailValueParts(zodiacText)"))
        assertFalse(supplementBlock.contains("valueParts = detailValueParts(constellationText)"))
        assertFalse(source.contains("R.string.zodiac_yang"))
        assertFalse(source.contains("R.string.zodiac_yin"))
        assertFalse(source.contains("constellationRulerResId("))
        assertFalse(source.contains("R.string.constellation_ruler_neptune"))
        assertTrue(source.contains(".removeSuffix(\"座\")"))
        assertTrue(supplementBlock.contains("stringResource(R.string.detail_birthday_zodiac)"))
        assertTrue(supplementBlock.contains("zodiacText"))
        assertTrue(supplementBlock.contains("stringResource(R.string.detail_birthday_constellation)"))
        assertTrue(supplementBlock.contains("constellationText"))
        assertFalse(supplementBlock.contains("DetailLabelRow(stringResource(R.string.detail_birthday_zodiac), zodiac, detailContentColor)"))
        assertFalse(supplementBlock.contains("DetailLabelRow(stringResource(R.string.detail_birthday_constellation), constellation, detailContentColor)"))
    }

    @Test
    fun detailScreenCentersSupplementalContentAndKeepsWarningsBelowDetails() {
        val source = readSource("ui/detail/DetailScreen.kt")
        val supplementBlock = source.substringBetween(
            "private fun DetailSupplementSections(",
            "@Composable\nprivate fun DetailBottomActions("
        )
        val labelRowBlock = source.substringAfter("private fun DetailLabelRow(")

        assertTrue(source.contains("private val DetailSupplementContentMaxWidth = 300.dp"))
        assertTrue(source.contains("private val DetailSupplementLabelWidth ="))
        assertTrue(supplementBlock.contains("horizontalAlignment = Alignment.CenterHorizontally"))
        assertTrue(supplementBlock.countOccurrences("DetailSupplementTable {") == 1)
        assertTrue(supplementBlock.contains(".widthIn(max = DetailSupplementContentMaxWidth)"))
        assertTrue(labelRowBlock.contains("modifier = modifier"))
        assertTrue(labelRowBlock.contains(".widthIn(max = DetailSupplementContentMaxWidth)"))
        assertTrue(labelRowBlock.contains("modifier = Modifier.width(DetailSupplementLabelWidth)"))
        assertFalse(labelRowBlock.contains("valueParts: List<String>? = null"))
        assertFalse(labelRowBlock.contains("DetailSegmentedValue("))
        assertFalse(source.contains("private val DetailValueSegmentWidths ="))
        assertFalse(source.contains("private val DetailValueSegmentGap ="))
        assertFalse(source.contains("private fun detailValueParts("))
        assertTrue(labelRowBlock.contains("textAlign = TextAlign.Start"))
        assertFalse(labelRowBlock.contains(".fillMaxWidth()"))
        assertFalse(labelRowBlock.contains(".weight(1f)"))
        assertFalse(supplementBlock.contains("HorizontalDivider("))
        assertFalse(supplementBlock.contains("SongDetailNoteBlock("))
        assertFalse(supplementBlock.contains("R.string.field_date"))
        assertFalse(supplementBlock.contains("dateStr"))

        val detailsIndex = supplementBlock.indexOf("DetailLabelRow(")
        val noteIndex = supplementBlock.indexOf("SongDetailNoteBlock(")
        val warningIndex = supplementBlock.indexOf("SongReminderStatusStrip(")

        assertTrue(detailsIndex >= 0)
        assertTrue(warningIndex > detailsIndex)
        assertTrue(noteIndex < 0 || warningIndex > noteIndex)
    }

    @Test
    fun detailScreenUsesFourPrimaryBottomActionsWithoutOverflow() {
        val source = readSource("ui/detail/DetailScreen.kt")
        val bottomBlock = source.substringBetween(
            "private fun DetailBottomActions(",
            "private fun detailReminderStatusAction("
        )

        assertTrue(bottomBlock.contains("SongBottomActionBar("))
        assertTrue(bottomBlock.contains("outlined = false"))
        assertTrue(bottomBlock.contains("R.string.button_pin"))
        assertTrue(bottomBlock.contains("R.string.button_edit"))
        assertTrue(bottomBlock.contains("R.string.button_share"))
        assertTrue(bottomBlock.contains("R.string.button_delete"))
        assertTrue(bottomBlock.contains("SongLineIconKind.Share"))
        assertTrue(bottomBlock.contains("SongLineIconKind.Delete"))
        assertTrue(bottomBlock.contains("MaterialTheme.colorScheme.error"))
        assertTrue(bottomBlock.contains("onDeleteClick"))
        assertFalse(source.contains("DetailOverflowActionMenu("))
        assertFalse(source.contains("ResponsiveDetailActionButtons("))
    }

    @Test
    fun detailScreenUsesSongSharePreviewBeforeNativeShare() {
        val source = readSource("ui/detail/DetailScreen.kt")

        assertTrue(source.contains("SongFormDialog("))
        assertTrue(source.contains("EventShareCard("))
        assertTrue(source.contains("EventShareImageRenderer"))
        assertTrue(source.contains("ShareImageStore"))
        assertTrue(source.contains("R.string.share_save_image"))
        assertTrue(source.contains("R.string.share_send_image"))
    }

    @Test
    fun eventEntryUsesCategoryDrivenTemplatesPreviewAndNamedColors() {
        val source = readSource("ui/event/EventEntryScreen.kt")

        assertTrue(source.contains("SongEventPreviewCard("))
        assertTrue(source.contains("songNamedColors"))
        assertTrue(source.contains("applyTemplateForCategory("))
        assertFalse(source.contains("eventEntryTemplates.forEach"))
    }

    @Test
    fun eventEntryReminderLeadTimeUsesWheelPickerRow() {
        val source = readSource("ui/event/EventEntryScreen.kt")
        val eventEntry = source.substringAfter("fun EventInputForm(")
            .substringBefore("private fun formatMinutesOfDay(")

        assertTrue(eventEntry.contains("val reminderDayOptions = remember { (0..3650).toList() }"))
        assertTrue(eventEntry.contains("val reminderDayPresets = remember { listOf(0, 1, 7, 30) }"))
        assertTrue(eventEntry.contains("val reminderHourPresets = remember { listOf(0, 7, 10, 12, 18) }"))
        assertTrue(eventEntry.contains("var showCustomRemindDaysPicker by remember"))
        assertTrue(eventEntry.contains("SongWheelPickerDialog("))
        assertTrue(eventEntry.contains("title = stringResource(R.string.custom_remind_days_label)"))
        assertTrue(eventEntry.contains("items = reminderDayOptions"))
        assertTrue(eventEntry.contains("items = reminderDayPresets"))
        assertTrue(eventEntry.contains("items = reminderHourPresets"))
        assertTrue(eventEntry.contains("ReminderPresetChipRow("))
        assertTrue(eventEntry.contains("draftHour = hour"))
        assertFalse(eventEntry.contains("hour == 24"))
        assertFalse(eventEntry.contains("draftHour = if (hour == 24) 0 else hour"))
        assertTrue(eventEntry.contains("draftMinute = 0"))
        assertTrue(eventEntry.contains("label = stringResource(R.string.custom_remind_days_label)"))
        assertTrue(eventEntry.contains("onClick = { showCustomRemindDaysPicker = true }"))
        assertFalse(eventEntry.contains("EventReminderLeadTimePresetRow("))
        assertFalse(eventEntry.contains("customRemindDaysInput"))
        assertFalse(eventEntry.contains("KeyboardOptions("))
    }

    @Test
    fun settingsScreensExpose315PreviewsAndReminderHealth() {
        val source = readSource("ui/settings/SettingsSubScreens.kt")

        assertTrue(source.contains("SongMiniPreviewSurface("))
        assertTrue(source.contains("SongReminderStatusStrip("))
        assertTrue(source.contains("buildReminderStatus("))
    }

    @Test
    fun classicalToggleHasSwitchSemanticsAndComfortableTouchTarget() {
        val source = readSource("ui/settings/SettingsSubScreens.kt")
        val toggle = source.substringBetween(
            "fun ClassicalToggle(",
            "private val PRESET_COLOR_HEX"
        )

        assertTrue(toggle.contains(".heightIn(min = 44.dp)"))
        assertTrue(toggle.contains("role = Role.Switch"))
        assertTrue(toggle.contains("stateDescription ="))
        assertTrue(toggle.contains("contentAlignment = Alignment.Center"))
    }

    @Test
    fun displaySettingsUseExpandablePaperSections() {
        val source = readSource("ui/settings/SettingsSubScreens.kt")
        val display = source.substringBetween(
            "fun DisplaySettingsContent(",
            "@Composable\nfun MilestoneSettingsContent"
        )

        assertTrue(display.countOccurrences("SettingsExpandableSection(") >= 3)
        assertTrue(display.contains("settings_display_section_language_title"))
        assertTrue(display.contains("settings_display_section_home_title"))
        assertTrue(display.contains("settings_display_section_date_title"))
        assertFalse(display.contains("SettingsGroupHeader(title = stringResource(R.string.settings_category_display_title))"))
    }

    @Test
    fun milestoneReminderLeadTimeUsesPresetChipsAndAccessibleDeleteTargets() {
        val source = readSource("ui/settings/SettingsSubScreens.kt")
        val milestone = source.substringBetween(
            "fun MilestoneSettingsContent(",
            "@Composable\nfun DataSettingsContent"
        )

        assertTrue(milestone.contains("ReminderLeadTimePresetRow("))
        assertTrue(milestone.contains("leadTimePresets"))
        assertFalse(milestone.contains("SnapWheelPicker(\n                items = remindDayOptions"))
        assertTrue(milestone.contains(".size(40.dp)"))
        assertTrue(milestone.contains("cd_delete_custom_milestone"))
        assertFalse(milestone.contains("contentDescription = null"))
    }

    @Test
    fun reminderTimeSettingsUseValueRowsAndUnifiedWheelDialogs() {
        val source = readSource("ui/settings/SettingsSubScreens.kt")
        val milestone = source.substringBetween(
            "fun MilestoneSettingsContent(",
            "@Composable\nfun DataSettingsContent"
        )

        assertTrue(milestone.contains("showDefaultEventReminderTimePicker"))
        assertTrue(milestone.contains("showMilestoneReminderTimePicker"))
        assertTrue(milestone.contains("SettingsValueRow("))
        assertTrue(milestone.countOccurrences("SongWheelPickerDialog(") >= 2)
        assertFalse(milestone.contains("SnapWheelPicker("))
        assertFalse(milestone.contains("items = remindHourOptions"))
        assertFalse(milestone.contains("items = remindMinuteOptions"))
    }

    @Test
    fun settingsUseUnifiedRowsAndSongToggles() {
        val componentSource = readSource("ui/settings/SettingsComponents.kt")
        val settingsSource = readSource("ui/settings/SettingsSubScreens.kt")
        val widgetSource = readSource("ui/settings/WidgetSettingsContent.kt")

        assertTrue(componentSource.contains("fun SongToggle("))
        assertTrue(componentSource.contains("fun SettingsRadioRow("))
        assertTrue(componentSource.contains("fun SettingsValueRow("))
        assertTrue(componentSource.contains("fun SettingsActionRow("))

        assertTrue(settingsSource.contains("SettingsRadioRow("))
        assertTrue(settingsSource.contains("SettingsValueRow("))
        assertTrue(settingsSource.contains("SettingsActionRow("))
        assertFalse(settingsSource.contains("private fun SettingsPickerValueRow("))
        assertFalse(settingsSource.contains("RadioButton("))

        assertTrue(widgetSource.contains("SongToggle("))
        assertFalse(widgetSource.contains("Switch("))
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
