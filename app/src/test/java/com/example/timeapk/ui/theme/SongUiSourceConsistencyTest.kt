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
            .substringBefore("private fun CompactEventTime")

        assertTrue(listBlock.contains(".heightIn(min = 52.dp)"))
        assertTrue(listBlock.contains(".width(2.dp)"))
        assertTrue(listBlock.contains(".height(18.dp)"))
        assertTrue(listBlock.contains("HorizontalDivider("))
        assertFalse(listBlock.contains(".heightIn(min = 64.dp)"))
        assertFalse(listBlock.contains(".heightIn(min = 88.dp)"))
        assertFalse(listBlock.contains(".height(40.dp)"))
    }

    @Test
    fun monthCalendar_usesLighterBookCalendarTreatment() {
        val homeSource = readSource("ui/home/HomeScreen.kt")
        val componentSource = readSource("ui/theme/SongComponents.kt")
        val monthBlock = homeSource.substringAfter("private fun MonthCalendarView(")

        assertTrue(monthBlock.contains("var showMonthPicker by remember"))
        assertTrue(monthBlock.contains("SongDateWheelPickerDialog("))
        assertTrue(monthBlock.contains("initialDateMillis = monthPickerInitialDate"))
        assertTrue(monthBlock.contains("title = stringResource(R.string.field_date)"))
        assertTrue(monthBlock.contains("val pickedMonth = YearMonth.from(pickedLocalDate)"))
        assertTrue(monthBlock.contains("currentMonth = pickedMonth"))
        assertTrue(monthBlock.contains("pickedDate = pickedMonth.atDay"))
        assertTrue(monthBlock.contains("role = Role.Button"))
        assertTrue(monthBlock.contains("this.contentDescription = currentMonthTitle"))
        assertTrue(monthBlock.contains(".heightIn(min = 48.dp, max = 72.dp"))
        assertFalse(monthBlock.contains(".height(60.dp)"))
        assertTrue(monthBlock.contains("CalendarOccurrenceRow("))
        assertFalse(monthBlock.contains("SongPaperSurface("))
        assertTrue(componentSource.contains("Color.Transparent"))
        assertTrue(componentSource.contains("selected -> SongPalette.PaperDeep.copy(alpha = 0.54f)"))
        assertTrue(componentSource.contains("hasEvents -> SongPalette.PaperWarm.copy(alpha = 0.45f)"))
    }

    @Test
    fun eventEntryUsesInkSectionsAndSongColorSpectrum() {
        val source = readSource("ui/event/EventEntryScreen.kt")

        assertTrue(source.contains("SongInkSection("))
        assertTrue(source.contains("SongInkTextField("))
        assertTrue(source.contains("SongColorSpectrumDialog("))
        assertTrue(source.contains("songNamedColors"))
        assertFalse(source.contains("Slider(value = r"))
        assertFalse(source.contains("thumbColor = Color.Red"))
        assertFalse(source.contains("thumbColor = Color.Green"))
        assertFalse(source.contains("thumbColor = Color.Blue"))
    }

    @Test
    fun eventEntryUsesCategoryAsTheOnlyTemplateSelector() {
        val source = readSource("ui/event/EventEntryScreen.kt")
        val formBlock = source.substringAfter("fun EventInputForm(")
            .substringBefore("private fun formatMinutesOfDay(")

        assertFalse(formBlock.contains("title = stringResource(R.string.event_template_title)"))
        assertFalse(formBlock.contains("eventEntryTemplates.forEach"))
        assertTrue(formBlock.contains("SongInkChoiceRow(\n                label = stringResource(R.string.field_category)"))
        assertTrue(formBlock.contains("applyTemplateForCategory(eventDetails, value)"))
    }

    @Test
    fun eventEntryPlacesDateFirstAndMovesRepeatIntoReminderSection() {
        val source = readSource("ui/event/EventEntryScreen.kt")
        val formBlock = source.substringAfter("fun EventInputForm(")
            .substringBefore("private fun formatMinutesOfDay(")
        val dateSection = "SongInkSection(\n            title = stringResource(R.string.event_entry_section_time)"
        val contentSection = "SongInkSection(\n            title = stringResource(R.string.event_entry_section_content)"
        val reminderSection = "SongInkSection(\n            title = stringResource(R.string.event_entry_section_reminder)"

        assertTrue(formBlock.indexOf(dateSection) < formBlock.indexOf(contentSection))
        assertTrue(formBlock.indexOf(contentSection) < formBlock.indexOf(reminderSection))

        val dateBlock = formBlock.substringAfter(dateSection).substringBefore(contentSection)
        assertTrue(dateBlock.contains("SongInkDateRow("))
        assertTrue(dateBlock.contains("value = dateString"))
        assertTrue(dateBlock.contains("contentDescription = stringResource(R.string.field_date)"))
        assertFalse(dateBlock.contains("label = stringResource(R.string.field_date)"))
        assertFalse(dateBlock.contains("label = stringResource(R.string.field_repeat)"))

        val dateRow = source.substringAfter("private fun SongInkDateRow(")
            .substringBefore("@Composable\nprivate fun SongInkChoiceRow(")
        assertTrue(source.contains("private fun SongInkDateRow("))
        assertTrue(dateRow.contains("style = MaterialTheme.typography.titleMedium"))
        assertFalse(dateRow.contains("style = MaterialTheme.typography.bodyMedium"))

        val reminderBlock = formBlock.substringAfter(reminderSection)
            .substringBefore("SongInkSection(\n            title = stringResource(R.string.event_entry_section_appearance)")
        assertTrue(reminderBlock.contains("label = stringResource(R.string.field_repeat)"))
        assertTrue(reminderBlock.contains("label = stringResource(R.string.custom_remind_days_label)"))
        assertTrue(reminderBlock.contains("label = stringResource(R.string.custom_reminder_time_label)"))
    }

    @Test
    fun wheelPickersUseUnifiedSongDialogSurface() {
        val eventEntrySource = readSource("ui/event/EventEntryScreen.kt")
        val datePickerSource = readSource("ui/components/BottomSheetDatePicker.kt")
        val dialogSource = readSource("ui/components/SongWheelPickerDialog.kt")
        val wheelSource = readSource("ui/components/SnapWheelPicker.kt")

        val eventPickerBlock = eventEntrySource.substringAfter("if (showCustomRepeatPicker)")
            .substringBefore("val baseDate = eventDateToLocalDate")

        assertTrue(dialogSource.contains("fun SongWheelPickerDialog("))
        assertTrue(dialogSource.contains("SongFormDialog("))

        assertFalse(eventEntrySource.contains("BottomSheetDatePicker("))
        assertFalse(eventPickerBlock.contains("AlertDialog("))
        assertTrue(eventPickerBlock.countOccurrences("SongWheelPickerDialog(") >= 3)

        assertFalse(datePickerSource.contains("ModalBottomSheet("))
        assertFalse(datePickerSource.contains("rememberModalBottomSheetState("))
        assertFalse(datePickerSource.contains("AssistChip("))
        assertFalse(datePickerSource.contains("private fun <T> WheelColumn("))
        assertFalse(datePickerSource.contains("private fun <T> WheelSyncEffect("))
        assertTrue(datePickerSource.countOccurrences("SnapWheelPicker(") >= 3)
        assertTrue(datePickerSource.contains("SongFilterChip("))
        assertTrue(datePickerSource.contains("SongWheelPickerDialog("))

        assertTrue(wheelSource.contains("AnimationSpecs.handscrollTween()"))
        assertTrue(wheelSource.contains("animateScrollBy("))
    }

    @Test
    fun generalDialogsUseUnifiedSongScaffold() {
        val dialogSource = readSource("ui/components/SongDialogScaffold.kt")
        val permissionSource = readSource("ui/components/PermissionActionDialog.kt")
        val eventEntrySource = readSource("ui/event/EventEntryScreen.kt")
        val detailSource = readSource("ui/detail/DetailScreen.kt")
        val settingsSource = readSource("ui/settings/SettingsSubScreens.kt")
        val mainUiSource = readMainSources("ui")

        assertTrue(dialogSource.contains("fun SongDialogScaffold("))
        assertTrue(dialogSource.contains("fun SongConfirmDialog("))
        assertTrue(dialogSource.contains("fun SongFormDialog("))
        assertTrue(dialogSource.contains("SongPaperSurface("))
        assertTrue(dialogSource.contains("AnimationSpecs.mistDissolveTween()"))

        assertTrue(permissionSource.contains("SongConfirmDialog("))
        assertTrue(eventEntrySource.contains("SongConfirmDialog("))
        assertTrue(eventEntrySource.contains("SongFormDialog("))
        assertTrue(detailSource.contains("SongConfirmDialog("))
        assertTrue(settingsSource.contains("SongFormDialog("))
        assertFalse(mainUiSource.contains("AlertDialog("))
    }

    @Test
    fun colorsFeedbackAndLegacyDatePickerUseUnifiedSongComponents() {
        val eventEntrySource = readSource("ui/event/EventEntryScreen.kt")
        val settingsSource = readSource("ui/settings/SettingsSubScreens.kt")
        val componentSource = readSource("ui/theme/SongComponents.kt")

        assertTrue(componentSource.contains("fun SongColorSwatch("))
        assertTrue(componentSource.contains("fun SongHexColorField("))
        assertTrue(eventEntrySource.contains("SongColorSwatch("))
        assertTrue(settingsSource.contains("SongColorSwatch("))
        assertTrue(eventEntrySource.contains("SongHexColorField("))
        assertTrue(settingsSource.contains("SongHexColorField("))

        assertFalse(eventEntrySource.contains("Toast.makeText"))
        assertFalse(eventEntrySource.contains("android.widget.Toast"))
        assertFalse(eventEntrySource.contains("MonthQuickSelector("))
        assertFalse(eventEntrySource.contains("DatePickerState"))
        assertFalse(eventEntrySource.contains("AssistChip("))
    }

    @Test
    fun homeTopBarKeepsOverflowAndSettingsWhileSearchLivesInsideOverflow() {
        val source = readSource("ui/home/HomeScreen.kt")
        val actionsBlock = source.substringAfter("actions = {")
            .substringBefore("colors = TopAppBarDefaults")
        val menuBlock = source.substringAfter("private fun HomeOverflowPanel(")
            .substringBefore("@Composable\nprivate fun TimelineActionTileGrid")

        assertTrue(actionsBlock.contains("InlineActionIconButton("))
        assertTrue(actionsBlock.contains("HomeOverflowActionMenu("))
        assertTrue(actionsBlock.contains("contentDescription = stringResource(R.string.settings_title)"))
        assertTrue(actionsBlock.contains("navigateToSettings()"))
        assertFalse(actionsBlock.contains("icon = SongLineIconKind.Search"))
        assertFalse(actionsBlock.contains("HomeTimelineActionMenu("))
        assertFalse(actionsBlock.contains("icon = Icons.Outlined.Tune"))
        assertFalse(actionsBlock.contains("icon = Icons.AutoMirrored.Outlined.Sort"))
        assertFalse(actionsBlock.contains("icon = Icons.Outlined.Settings"))

        assertTrue(menuBlock.contains("HomeOverflowSearchField("))
        assertTrue(menuBlock.contains("searchQuery = searchQuery"))
        assertTrue(menuBlock.contains("onSearchQueryChange = onSearchQueryChange"))
        assertFalse(menuBlock.contains("label = stringResource(R.string.settings_title)"))
    }

    @Test
    fun homeOverflowSearchSitsAtBottomAndMatchesActionItemHeight() {
        val source = readSource("ui/home/HomeScreen.kt")
        val menuBlock = source.substringAfter("private fun HomeOverflowPanel(")
            .substringBefore("@Composable\nprivate fun HomeOverflowSearchField")
        val searchBlock = source.substringAfter("private fun HomeOverflowSearchField(")
            .substringBefore("private data class TimelineActionTileSpec")
        val optionBlock = source.substringAfter("private fun SongActionOptionTile(")
            .substringBefore("@Composable\nprivate fun EmptyState")
        val slipBlock = source.substringAfter("private fun SongActionSlip(")
            .substringBefore("@Composable\nprivate fun SongActionSlipFoldDecoration")

        val searchIndex = menuBlock.indexOf("HomeOverflowSearchField(")
        val sortIndex = menuBlock.indexOf("HomeMenuSectionLabel(text = stringResource(R.string.sort_menu))")

        assertTrue(searchIndex > sortIndex)
        assertTrue(searchBlock.contains(".heightIn(min = HomeOverflowActionItemHeight)"))
        assertTrue(optionBlock.contains(".heightIn(min = HomeOverflowActionItemHeight)"))
        assertTrue(menuBlock.contains("footer = {"))
        assertTrue(slipBlock.contains("footer: (@Composable ColumnScope.() -> Unit)? = null"))
        assertTrue(slipBlock.contains(".weight(1f, fill = false)"))
        assertFalse(searchBlock.contains("OutlinedTextField("))
    }

    @Test
    fun keySurfacesUseSongLineIconsInsteadOfMaterialToolGlyphs() {
        val iconSource = readSource("ui/theme/SongLineIcons.kt")
        val homeSource = readSource("ui/home/HomeScreen.kt")
        val detailSource = readSource("ui/detail/DetailScreen.kt")
        val settingsSource = readSource("ui/settings/SettingsComponents.kt")
        val eventEntrySource = readSource("ui/event/EventEntryScreen.kt")
        val settingsSubScreensSource = readSource("ui/settings/SettingsSubScreens.kt")
        val widgetConfigSource = readSource("widget/WidgetConfigActivity.kt")

        assertTrue(iconSource.contains("enum class SongLineIconKind"))
        assertTrue(iconSource.contains("fun SongLineIcon("))
        assertTrue(iconSource.contains("fun SongSettingMark("))

        listOf(homeSource, detailSource, settingsSource, eventEntrySource).forEach { source ->
            assertTrue(source.contains("SongLineIcon(") || source.contains("SongSettingMark("))
        }

        listOf(homeSource, detailSource, settingsSource, eventEntrySource, settingsSubScreensSource, widgetConfigSource).forEach { source ->
            assertFalse(source.contains("Icons.Outlined.Search"))
            assertFalse(source.contains("Icons.Outlined.MoreVert"))
            assertFalse(source.contains("Icons.Outlined.Add"))
            assertFalse(source.contains("Icons.Outlined.Edit"))
            assertFalse(source.contains("Icons.Outlined.Delete"))
            assertFalse(source.contains("Icons.Default.Close"))
            assertFalse(source.contains("Icons.AutoMirrored.Filled.ArrowBack"))
            assertFalse(source.contains("Icons.AutoMirrored.Outlined.ArrowBack"))
            assertFalse(source.contains("Icons.AutoMirrored.Filled.ArrowForward"))
        }

        val sourceRoot = File("src/main/java/com/example/timeapk").takeIf { it.exists() }
            ?: File("app/src/main/java/com/example/timeapk")
        val allMainSources = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText(Charsets.UTF_8) }
        assertFalse(allMainSources.contains("androidx.compose.material.icons"))
    }

    @Test
    fun homeOverflowUsesPaperSlipInsteadOfSystemDropdownMenu() {
        val source = readSource("ui/home/HomeScreen.kt")
        val menuBlock = source.substringAfter("private fun HomeOverflowPanel(")
            .substringBefore("@Composable\nprivate fun TimelineActionTileGrid")

        assertTrue(source.contains("HomeOverflowPanel("))
        assertTrue(menuBlock.contains("SongActionSlip("))
        assertTrue(menuBlock.contains("HomeOverflowSearchField("))
        assertTrue(menuBlock.contains("TimelineActionTileGrid("))
        assertTrue(source.contains("SongActionSlipFoldDecoration("))
        assertFalse(menuBlock.contains("SongActionSlipItem("))
        assertFalse(menuBlock.contains("DropdownMenu("))
        assertFalse(menuBlock.contains("DropdownMenuItem("))
    }

    @Test
    fun eventEntryUsesLightInkSectionsAndSaveSlip() {
        val source = readSource("ui/event/EventEntryScreen.kt")

        assertTrue(source.contains("SongInkSection("))
        assertTrue(source.contains("SongSaveSlip("))
        assertFalse(source.contains("SongWritingSection("))
        assertFalse(Regex("(?m)^\\s*Button\\(").containsMatchIn(source))
    }

    @Test
    fun visibleInstructionalCopyIsNotRenderedOnSongSurfaces() {
        val homeSource = readSource("ui/home/HomeScreen.kt")
        val eventEntrySource = readSource("ui/event/EventEntryScreen.kt")
        val settingsSource = readSource("ui/settings/SettingsComponents.kt")

        assertFalse(homeSource.contains("home_custom_sort_hint"))
        assertFalse(homeSource.contains("home_empty_subtitle"))
        assertFalse(eventEntrySource.contains("event_entry_template_summary"))
        assertFalse(eventEntrySource.contains("event_entry_section_content_summary"))
        assertFalse(eventEntrySource.contains("event_entry_section_time_summary"))
        assertFalse(eventEntrySource.contains("event_entry_section_appearance_summary"))
        assertFalse(eventEntrySource.contains("event_entry_song_color_palette_hint"))
        assertFalse(eventEntrySource.contains("sync_to_schedule_summary"))
        assertFalse(settingsSource.contains("stringResource(category.descriptionRes)"))
        assertFalse(settingsSource.contains("text = summary"))
    }

    @Test
    fun songThemeProvidesPaperTextureAndHandscrollMotionHooks() {
        val componentSource = readSource("ui/theme/SongComponents.kt")
        val motionSource = readSource("ui/theme/AnimationSpecs.kt")
        val appSource = readSource("TimeApp.kt")

        assertTrue(componentSource.contains("fun SongPaperTextureOverlay("))
        assertTrue(componentSource.contains("paperTextureAlpha"))
        assertTrue(componentSource.contains("crackColor"))
        assertTrue(componentSource.contains("shortFiberColor"))
        assertTrue(motionSource.contains("DurationHandscrollMs"))
        assertTrue(motionSource.contains("handscrollTween"))
        assertTrue(motionSource.contains("mistDissolveTween"))
        assertTrue(appSource.contains("AnimationSpecs.mistDissolveTween()"))
    }

    @Test
    fun songSoundscapeProvidesOptionalLowVolumeChimeForKeyActions() {
        val soundSource = readOptionalSource("ui/sound/SongSoundscape.kt")
        val prefsSource = readSource("data/UserPreferencesRepository.kt")
        val settingsSource = readSource("ui/settings/SettingsSubScreens.kt")
        val homeSource = readSource("ui/home/HomeScreen.kt")
        val eventEntrySource = readSource("ui/event/EventEntryScreen.kt")

        assertTrue(soundSource.contains("object SongSoundscape"))
        assertTrue(soundSource.contains("enum class SongSoundEffect"))
        assertTrue(soundSource.contains("AudioTrack"))
        assertTrue(soundSource.contains("USAGE_ASSISTANCE_SONIFICATION"))
        assertTrue(soundSource.contains("volume = 0.12f"))
        assertTrue(soundSource.contains("Action(durationMs"))
        assertTrue(soundSource.contains("Commit(durationMs"))

        assertTrue(prefsSource.contains("SONG_SOUND_ENABLED"))
        assertTrue(prefsSource.contains("songSoundEnabledFlow"))
        assertTrue(prefsSource.contains("setSongSoundEnabled"))

        assertTrue(settingsSource.contains("settings_song_sound_title"))
        assertTrue(settingsSource.contains("songSoundEnabledFlow"))
        assertTrue(settingsSource.contains("setSongSoundEnabled"))

        assertTrue(homeSource.contains("rememberSongSoundscape()"))
        assertTrue(homeSource.contains("SongSoundEffect.Action"))
        assertTrue(eventEntrySource.contains("rememberSongSoundscape()"))
        assertTrue(eventEntrySource.contains("SongSoundEffect.Commit"))
    }

    @Test
    fun calendarSelectedEventsRenderDetailedDayNotes() {
        val source = readSource("ui/home/HomeScreen.kt")
        val rowBlock = source.substringAfter("private fun CalendarOccurrenceRow(")

        assertTrue(rowBlock.contains("val calendarMetaLine = buildList"))
        assertTrue(rowBlock.contains("val calendarTimeLine"))
        assertTrue(rowBlock.contains("occurrence.eventState.event.repeatType"))
        assertTrue(rowBlock.contains("occurrence.eventState.event.remindEnabled"))
        assertTrue(rowBlock.contains("formatLunarDateString(occurrence.date"))
    }

    @Test
    fun songMotionAvoidsObviousElasticScaling() {
        val pressableSource = readSource("ui/components/Pressable.kt")
        val eventEntrySource = readSource("ui/event/EventEntryScreen.kt")

        assertTrue(pressableSource.contains("PressScaleSubtle"))
        assertFalse(pressableSource.contains("scaleDown = 0.96f"))
        assertFalse(pressableSource.contains("scaleDown = 0.9f"))
        assertFalse(eventEntrySource.contains("if (isPressed) 0.9f else 1f"))
    }

    @Test
    fun splashUsesQuietSongTypographyInsteadOfPosterTracking() {
        val source = readSource("ui/splash/SplashScreen.kt")

        assertTrue(source.contains("SongSplashSeal("))
        assertFalse(source.contains("letterSpacing = 1.sp"))
    }

    @Test
    fun widgetConfigUsesPreviewFirstSectionsAndSongSaveBar() {
        val settingsSource = readSource("ui/settings/WidgetSettingsContent.kt")
        val routeSource = readSource("widget/WidgetConfigActivity.kt")

        assertTrue(settingsSource.contains("WidgetConfigSection("))
        assertTrue(settingsSource.contains("initiallyExpanded = true"))
        assertTrue(routeSource.contains("WidgetConfigSaveBar("))
        assertFalse(routeSource.contains("Button("))
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

    private fun readOptionalSource(relative: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relative")
        if (direct.exists()) {
            return direct.readText(Charsets.UTF_8)
        }
        val fromRoot = File("app/src/main/java/com/example/timeapk/$relative")
        return if (fromRoot.exists()) fromRoot.readText(Charsets.UTF_8) else ""
    }

    private fun readMainSources(relativeDir: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relativeDir")
        val sourceRoot = if (direct.exists()) {
            direct
        } else {
            File("app/src/main/java/com/example/timeapk/$relativeDir")
        }
        require(sourceRoot.exists()) { "Missing source dir: $relativeDir" }
        return sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText(Charsets.UTF_8) }
    }

    private fun String.countOccurrences(needle: String): Int {
        return Regex(Regex.escape(needle)).findAll(this).count()
    }
}
