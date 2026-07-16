package com.example.timeapk.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeInteractionPolicyTest {
    @Test
    fun emptyStateAddIconIsDecorativeBecauseTheFabOwnsTheAddAction() {
        val source = readSource("ui/home/HomeScreen.kt")
        val emptyState = source.substringAfter("private fun EmptyState(")
            .substringBefore("@OptIn(ExperimentalFoundationApi::class)")

        assertTrue(emptyState.contains("kind = SongLineIconKind.Add"))
        assertTrue(emptyState.contains("contentDescription = null"))
        assertFalse(emptyState.contains("R.string.cd_add_event"))
    }


    @Test
    fun homeCardLongPressEditEnabled_isDisabledWhenCustomSorting() {
        assertFalse(homeCardLongPressEditEnabled(SortType.Custom))
    }

    @Test
    fun homeCardLongPressEditEnabled_isDisabledAcrossHomeSortModes() {
        assertFalse(homeCardLongPressEditEnabled(SortType.ByDays))
        assertFalse(homeCardLongPressEditEnabled(SortType.ByDate))
    }

    @Test
    fun homeCardUsesTapOnlyInteraction_isDisabledAcrossHomeSortModes() {
        assertFalse(homeCardUsesTapOnlyInteraction(SortType.Custom))
        assertFalse(homeCardUsesTapOnlyInteraction(SortType.ByDays))
        assertFalse(homeCardUsesTapOnlyInteraction(SortType.ByDate))
    }

    @Test
    fun homeCardTapNavigationEnabled_isEnabledAcrossHomeSortModes() {
        assertTrue(homeCardTapNavigationEnabled(SortType.Custom))
        assertTrue(homeCardTapNavigationEnabled(SortType.ByDays))
        assertTrue(homeCardTapNavigationEnabled(SortType.ByDate))
    }

    @Test
    fun homeCardDragSortEnabled_isEnabledOnlyForCustomSort() {
        assertTrue(homeCardDragSortEnabled(SortType.Custom))
        assertFalse(homeCardDragSortEnabled(SortType.ByDays))
        assertFalse(homeCardDragSortEnabled(SortType.ByDate))
    }

    @Test
    fun homeUsesListLevelReorderDetection_isDisabledBecauseCardsUseDedicatedHandles() {
        assertFalse(homeUsesListLevelReorderDetection(SortType.Custom))
        assertFalse(homeUsesListLevelReorderDetection(SortType.ByDays))
        assertFalse(homeUsesListLevelReorderDetection(SortType.ByDate))
    }

    @Test
    fun homeScreenKeepsTimelineDigestInActionMenuAndRemovesMonthHighlights() {
        val source = readSource("ui/home/HomeScreen.kt")

        assertTrue(source.contains("HomeOverflowActionMenu("))
        assertFalse(source.contains("HomeTimelineDigestRow("))
        assertFalse(source.contains("MonthHighlightsSection("))
        assertTrue(source.contains("buildHomeTimelineDigest("))
        assertTrue(source.contains("filterEventsForTimelineBucket("))
        assertFalse(source.contains("monthHighlightsForOccurrences("))
    }

    @Test
    fun homeListItemUsesCompactWidgetStyle() {
        val source = readSource("ui/home/HomeScreen.kt")
        val listItemSource = source
            .substringAfter("private fun EventListItem(")
            .substringBefore("private fun CompactEventTime(")

        assertTrue(source.contains("CompactEventTime"))
        assertFalse(listItemSource.contains("val metaLine = buildList"))
        assertFalse(listItemSource.contains("val supportLine = buildList"))
        assertFalse(listItemSource.contains("dateLine = targetLocalDate.format(dateFormatter)"))
    }

    @Test
    fun homeCardViewKeepsCategoryRepeatAndReminderOnOneAuxiliaryLine() {
        val source = readSource("ui/home/HomeScreen.kt")
        val cardSource = source
            .substringAfter("fun EventCard(")
            .substringBefore("private fun EventListItem(")

        assertTrue(cardSource.contains("val cardAuxiliaryLine = buildList"))
        assertTrue(cardSource.contains("eventState.event.category"))
        assertTrue(cardSource.contains("eventState.event.repeatType"))
        assertTrue(cardSource.contains("eventState.event.remindEnabled"))
        assertTrue(cardSource.contains("R.string.field_remind"))
        assertFalse(cardSource.contains("cardSupportLine"))
    }

    @Test
    fun eventColorTextUsesSharedDarkThemeContrastPolicyInCardAndListModes() {
        val source = readSource("ui/home/HomeScreen.kt")
        val cardSource = source.substringAfter("fun EventCard(")
            .substringBefore("private fun EventListItem(")
        val listSource = source.substringAfter("private fun EventListItem(")
            .substringBefore("private fun CompactEventTime(")

        assertTrue(cardSource.contains("HomeEventColorPolicy.ensureTextContrast("))
        assertTrue(cardSource.contains("if (lightSurface)"))
        assertTrue(cardSource.contains("baseCardColor.copy(alpha = if (isPast) 0.54f else 0.74f)"))
        assertTrue(listSource.contains("HomeEventColorPolicy.ensureTextContrast("))
        assertTrue(cardSource.contains("remember(baseCardColor"))
        assertTrue(listSource.contains("remember(eventColor"))
        assertTrue(cardSource.contains("HomeEventColorPolicy.compositeOver("))
        assertTrue(listSource.contains("HomeEventColorPolicy.compositeOver("))
        assertTrue(listSource.contains("if (isLightSurface)"))
        assertFalse(listSource.contains("lerp(eventColor, MaterialTheme.colorScheme.onBackground, 0.2f)"))
        assertFalse(cardSource.contains("alpha = cardAlpha"))
        assertFalse(listSource.contains("alpha = itemAlpha"))
    }

    @Test
    fun homeTimeToggleTargetsExposeAccessibleButtonSemantics() {
        val source = readSource("ui/home/HomeScreen.kt")
        val cardSource = source.substringAfter("fun EventCard(")
            .substringBefore("private fun EventListItem(")
        val compactTimeSource = source.substringAfter("private fun CompactEventTime(")
            .substringBefore("@OptIn(ExperimentalFoundationApi::class)")

        assertTrue(source.contains("R.string.cd_toggle_date_delta_display"))
        assertTrue(cardSource.contains("role = Role.Button"))
        assertTrue(cardSource.contains("contentDescription = toggleDateDeltaDescription"))
        assertTrue(compactTimeSource.contains("role = Role.Button"))
        assertTrue(compactTimeSource.contains("contentDescription = contentDescription"))
    }

    @Test
    fun monthCalendarLetsSelectedEventListUseRemainingSpace() {
        val source = readSource("ui/home/HomeScreen.kt")
        val monthSource = source.substringAfter("private fun MonthCalendarView(")
            .substringBefore("@OptIn(ExperimentalFoundationApi::class)\n@Composable\nprivate fun CalendarOccurrenceRow")

        assertTrue(monthSource.contains("modifier = Modifier"))
        assertTrue(monthSource.contains(".weight(1f"))
        assertTrue(monthSource.contains(".heightIn(min = 48.dp, max = 72.dp"))
        assertFalse(monthSource.contains(".height(60.dp)"))
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
