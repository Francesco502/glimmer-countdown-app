package com.example.timeapk.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeEmptyStatePolicyTest {
    @Test
    fun resolveHomeEmptyStateKind_usesFirstEventOnlyWhenTheUnfilteredCalendarIsEmpty() {
        assertEquals(HomeEmptyStateKind.FirstEvent, resolveHomeEmptyStateKind(isCalendarEmpty = true))
        assertEquals(HomeEmptyStateKind.NoMatches, resolveHomeEmptyStateKind(isCalendarEmpty = false))

        val viewModelSource = readSource("ui/home/HomeViewModel.kt")
        assertTrue(viewModelSource.contains("val calendarUiState: StateFlow<List<EventUiState>> = allHomeUiState"))
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
