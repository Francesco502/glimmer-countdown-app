package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class HomeEmptyStatePolicyTest {
    @Test
    fun resolveHomeEmptyStateKind_usesFirstEventOnlyWhenTheUnfilteredCalendarIsEmpty() {
        assertEquals(HomeEmptyStateKind.FirstEvent, resolveHomeEmptyStateKind(isCalendarEmpty = true))
        assertEquals(HomeEmptyStateKind.NoMatches, resolveHomeEmptyStateKind(isCalendarEmpty = false))
    }

    @Test
    fun calendarMode_keepsFilteredEventsWhileUnfilteredEventsResolveNoMatches() {
        val allEvents = listOf(
            EventUiState(
                event = Event(
                    id = 1,
                    title = "Birthday",
                    date = 0L,
                    category = CATEGORY_BIRTHDAY
                ),
                daysRemaining = 0L,
                isPast = false,
                nextOccurrenceDate = LocalDate.of(2026, 7, 17)
            )
        )
        val calendarEvents = buildHomeVisibleList(
            all = allEvents,
            filterType = FilterType.Other,
            sortType = SortType.Custom,
            query = "",
            customEventOrderIds = emptyList(),
            pinnedEventIds = emptyList()
        )

        val viewModelSource = readSource("ui/home/HomeViewModel.kt")
        val homeScreenSource = readSource("ui/home/HomeScreen.kt")
        assertFalse(allEvents.isEmpty())
        assertTrue(calendarEvents.isEmpty())
        assertEquals(HomeEmptyStateKind.NoMatches, resolveHomeEmptyStateKind(allEvents.isEmpty()))
        assertTrue(viewModelSource.contains("val calendarUiState: StateFlow<List<EventUiState>> = homeUiState"))
        assertTrue(viewModelSource.contains("val unfilteredCalendarUiState: StateFlow<List<EventUiState>> = allHomeUiState"))
        assertTrue(homeScreenSource.contains("val unfilteredCalendarUiState by viewModel.unfilteredCalendarUiState.collectAsState()"))
        assertTrue(homeScreenSource.contains("resolveHomeEmptyStateKind(unfilteredCalendarUiState.isEmpty())"))
    }

    @Test
    fun noMatchesStateUsesSearchIconAndAConstraintClearingAction() {
        val source = readSource("ui/home/HomeScreen.kt")
        val emptyState = source.substringAfter("private fun EmptyState(")
            .substringBefore("@OptIn(ExperimentalFoundationApi::class)")
        val noMatches = emptyState.substringAfter("HomeEmptyStateKind.NoMatches ->")

        assertTrue(noMatches.contains("onClick = onClearConstraints"))
        assertTrue(noMatches.contains("kind = SongLineIconKind.Search"))
        assertTrue(noMatches.contains("R.string.home_empty_no_matches"))
        assertTrue(noMatches.contains("R.string.home_empty_clear_constraints"))
    }

    @Test
    fun homeEmptyStateStringsExistInChineseDefaultAndEnglishResources() {
        val expectedNames = listOf(
            "home_empty_first_event_cta",
            "home_empty_no_matches",
            "home_empty_clear_constraints"
        )

        listOf(
            "values/strings.xml",
            "values-zh/strings.xml",
            "values-en/strings.xml"
        ).map(::resourceFile).forEach { file ->
            val content = file.readText(Charsets.UTF_8)
            expectedNames.forEach { name ->
                assertTrue("${file.path} is missing $name", content.contains("name=\"$name\""))
            }
        }
    }

    private fun readSource(relative: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relative")
        if (direct.exists()) return direct.readText(Charsets.UTF_8)
        val fromRoot = File("app/src/main/java/com/example/timeapk/$relative")
        require(fromRoot.exists()) { "Missing source file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }

    private fun resourceFile(relative: String): File {
        val direct = File("src/main/res/$relative")
        if (direct.exists()) return direct
        val fromRoot = File("app/src/main/res/$relative")
        require(fromRoot.exists()) { "Missing resource file: $relative" }
        return fromRoot
    }
}
