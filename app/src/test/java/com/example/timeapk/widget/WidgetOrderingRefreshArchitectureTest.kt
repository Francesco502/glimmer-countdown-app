package com.example.timeapk.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WidgetOrderingRefreshArchitectureTest {
    @Test
    fun providerUsesSuspendLoadingAndAlwaysFinishesPendingResult() {
        val provider = mainSource("widget/CountdownAppWidgetProvider.kt").readText(Charsets.UTF_8)
        val resolver = mainSource("widget/WidgetContentResolver.kt").readText(Charsets.UTF_8)

        assertFalse(provider.contains("runBlocking"))
        assertFalse(resolver.contains("runBlocking"))
        assertTrue(provider.contains("goAsync()"))
        assertTrue(provider.contains("pendingResult.finish()"))
        assertTrue(provider.contains("finally"))
        assertTrue(resolver.contains("suspend fun load("))
    }

    @Test
    fun homeDragCallbacksReadCurrentStateAndCaptureIdsAtFirstMove() {
        val source = mainSource("ui/home/HomeScreen.kt").readText(Charsets.UTF_8)
        val reorderSetup = source.substringAfter("val reorderState = rememberReorderableLazyListState(")
            .substringBefore("AnimatedContent(")
        val move = reorderSetup.substringAfter("onMove =").substringBefore("onDragEnd =")
        val dragEnd = reorderSetup.substringAfter("onDragEnd =")

        assertTrue(source.contains("val latestDragEnabled by rememberUpdatedState(dragEnabled)"))
        assertTrue(source.contains("val latestDisplayedIds by rememberUpdatedState("))
        assertTrue(source.contains("val latestViewModel by rememberUpdatedState(viewModel)"))
        assertTrue(source.contains("var visibleIdsAtDragStart by remember { mutableStateOf<List<Int>?>(null) }"))
        assertTrue(move.contains("if (visibleIdsAtDragStart == null)"))
        assertTrue(move.contains("visibleIdsAtDragStart = latestDisplayedIds"))
        assertTrue(move.indexOf("visibleIdsAtDragStart = latestDisplayedIds") < move.indexOf("orderedList.removeAt"))
        assertTrue(dragEnd.contains("val visibleIds = visibleIdsAtDragStart"))
        assertTrue(dragEnd.contains("visibleIdsAtDragStart = null"))
        assertTrue(dragEnd.contains("visibleIds = visibleIds"))
        assertTrue(!source.contains("val visibleIdsBeforeDrag = displayedList.map"))
    }

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
        assertTrue(dragEnd.contains("latestViewModel.updateCustomEventOrder("))
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
