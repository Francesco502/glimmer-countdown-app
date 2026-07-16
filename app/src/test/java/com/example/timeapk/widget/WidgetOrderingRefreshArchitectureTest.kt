package com.example.timeapk.widget

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WidgetOrderingRefreshArchitectureTest {
    @Test
    fun widgetResolverReadsAndAppliesThePersistedHomeSortType() {
        val source = mainSource("widget/WidgetContentResolver.kt").readText(Charsets.UTF_8)

        assertTrue(source.contains("prefs.sortTypeFlow.first()"))
        assertTrue(source.contains("filterAndSortStates(it, config, pinnedEventIds, customEventOrder, homeSortType)"))
        assertTrue(source.contains("SORT_HOME -> applyHomeSort("))
        assertTrue(source.contains("sortType = homeSortType"))
    }

    @Test
    fun homeOrderingPreferenceChangesRefreshWidgetsAfterPersistence() {
        val viewModel = mainSource("ui/home/HomeViewModel.kt").readText(Charsets.UTF_8)
        val homeScreen = mainSource("ui/home/HomeScreen.kt").readText(Charsets.UTF_8)
        val detailScreen = mainSource("ui/detail/DetailScreen.kt").readText(Charsets.UTF_8)

        assertPersistedBeforeRefresh(
            source = viewModel.substringAfter("fun updateSortType(type: SortType)").substringBefore("suspend fun deleteEvent"),
            persistence = "userPrefs.setSortType(type.ordinal)"
        )
        assertPersistedBeforeRefresh(
            source = viewModel.substringAfter("fun updateCustomEventOrder(").substringBefore("suspend fun deleteEvent"),
            persistence = "userPrefs.setCustomEventOrder(mergedIds)"
        )
        val dragEnd = homeScreen.substringAfter("onDragEnd =").substringBefore("onDragCancel =")
        assertTrue(dragEnd.contains("viewModel.updateCustomEventOrder("))
        assertTrue(!dragEnd.contains("prefs.setCustomEventOrder("))
        assertPersistedBeforeRefresh(
            source = detailScreen.substringAfter("onPinClick =").substringBefore("onEditClick ="),
            persistence = "prefs.togglePinnedEventId(eventState.event.id)"
        )
    }

    private fun assertPersistedBeforeRefresh(source: String, persistence: String) {
        val persistenceIndex = source.indexOf(persistence)
        val refreshIndex = source.indexOf("WidgetUpdater.refreshCountdownWidgets")
        assertTrue("Missing persistence call: $persistence", persistenceIndex >= 0)
        assertTrue("Widget refresh must follow persistence: $persistence", refreshIndex > persistenceIndex)
    }

    private fun mainSource(relative: String): File {
        return listOf(
            File("src/main/java/com/example/timeapk/$relative"),
            File("app/src/main/java/com/example/timeapk/$relative")
        ).firstOrNull(File::exists) ?: error("Missing source: $relative")
    }
}
