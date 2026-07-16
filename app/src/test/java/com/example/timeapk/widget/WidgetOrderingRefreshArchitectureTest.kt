package com.example.timeapk.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WidgetOrderingRefreshArchitectureTest {
    @Test
    fun widgetRefreshesForCivilDateAndClockChanges() {
        val manifest = manifestSource().readText(Charsets.UTF_8)
        val provider = mainSource("widget/CountdownAppWidgetProvider.kt").readText(Charsets.UTF_8)
        val widgetReceiver = manifest.substringAfter("android:name=\".widget.CountdownAppWidgetProvider\"")
            .substringBefore("</receiver>")

        listOf(
            "android.intent.action.DATE_CHANGED",
            "android.intent.action.TIME_SET",
            "android.intent.action.TIMEZONE_CHANGED"
        ).forEach { action ->
            assertTrue("Widget receiver is missing $action", widgetReceiver.contains(action))
        }
        listOf(
            "Intent.ACTION_CONFIGURATION_CHANGED",
            "Intent.ACTION_DATE_CHANGED",
            "Intent.ACTION_TIME_CHANGED",
            "Intent.ACTION_TIMEZONE_CHANGED"
        ).forEach { action ->
            assertTrue("Widget provider does not handle $action", provider.contains(action))
        }

        val receive = provider.substringAfter("override fun onReceive(").substringBefore("override fun onUpdate(")
        assertTrue(receive.contains("getAppWidgetIds(context, appWidgetManager)"))
        assertTrue(receive.contains("launchRefresh("))
        assertTrue(receive.contains("goAsync()"))
        assertTrue(receive.contains("if (appWidgetIds.isEmpty()) return"))
        assertEquals(1, receive.windowed("super.onReceive(context, intent)".length)
            .count { it == "super.onReceive(context, intent)" })
    }

    @Test
    fun providerUsesSuspendLoadingAndAlwaysFinishesPendingResult() {
        val provider = mainSource("widget/CountdownAppWidgetProvider.kt").readText(Charsets.UTF_8)
        val resolver = mainSource("widget/WidgetContentResolver.kt").readText(Charsets.UTF_8)
        val service = mainSource("widget/CountdownWidgetService.kt").readText(Charsets.UTF_8)

        assertFalse(provider.contains("runBlocking"))
        assertFalse(resolver.contains("runBlocking"))
        assertEquals(3, provider.windowed("goAsync()".length).count { it == "goAsync()" })
        val launchRefresh = provider.substringAfter("private fun launchRefresh(")
            .substringBefore("internal fun buildWidgetRemoteViews(")
        val nullApplicationPath = launchRefresh.substringAfter("if (app == null) {").substringBefore("app.launchAppTask")
        val activeApplicationPath = launchRefresh.substringAfter("app.launchAppTask")
        assertTrue(nullApplicationPath.contains("pendingResult.finish()"))
        assertTrue(nullApplicationPath.contains("return"))
        assertTrue(activeApplicationPath.contains("try {"))
        assertTrue(activeApplicationPath.substringAfter("finally {").contains("pendingResult.finish()"))
        assertTrue(resolver.contains("suspend fun load("))
        assertTrue(service.contains("runBlocking(Dispatchers.IO)"))
        val identitySection = service.substringAfter("val identityToken = Binder.clearCallingIdentity()")
        assertTrue(identitySection.contains("finally {"))
        assertTrue(identitySection.substringAfter("finally {").contains("Binder.restoreCallingIdentity(identityToken)"))
    }

    @Test
    fun everyProviderRefreshPathUsesOneSharedCoordinator() {
        val provider = mainSource("widget/CountdownAppWidgetProvider.kt").readText(Charsets.UTF_8)
        val updater = mainSource("widget/WidgetUpdater.kt").readText(Charsets.UTF_8)
        val coordinator = mainSource("widget/WidgetRefreshCoordinator.kt").readText(Charsets.UTF_8)

        val refreshAll = provider.substringAfter("fun refreshAllWidgets(").substringBefore("fun getAppWidgetIds(")
        val coordinated = provider.substringAfter("private suspend fun runCoordinatedRefresh(")
            .substringBefore("private suspend fun refreshWidgets(")
        val launchRefresh = provider.substringAfter("private fun launchRefresh(").substringBefore("internal fun buildWidgetRemoteViews(")
        assertTrue(refreshAll.contains("runCoordinatedRefresh(app"))
        assertTrue(launchRefresh.contains("runCoordinatedRefresh(app"))
        assertTrue(coordinated.contains("WidgetRefreshCoordinator.runLatestSnapshot"))
        assertTrue(coordinated.contains("refreshWidgets(app"))
        assertEquals(1, provider.windowed("refreshWidgets(app".length).count { it == "refreshWidgets(app" })
        assertTrue(updater.contains("CountdownAppWidgetProvider.refreshAllWidgets(context)"))
        assertTrue(coordinator.contains("private val refreshMutex = Mutex()"))
        assertTrue(coordinator.contains("refreshMutex.withLock"))
        assertTrue(coordinator.contains("The refresh body reads"))
    }

    @Test
    fun allProviderCallbacksLaunchAsyncRefreshes() {
        val provider = mainSource("widget/CountdownAppWidgetProvider.kt").readText(Charsets.UTF_8)
        val configuration = provider.substringAfter("override fun onReceive(").substringBefore("override fun onUpdate(")
        val update = provider.substringAfter("override fun onUpdate(").substringBefore("override fun onAppWidgetOptionsChanged(")
        val options = provider.substringAfter("override fun onAppWidgetOptionsChanged(").substringBefore("override fun onDeleted(")

        listOf(configuration, update, options).forEach { callback ->
            assertTrue(callback.contains("launchRefresh("))
            assertTrue(callback.contains("goAsync()"))
        }
    }

    @Test
    fun widgetServiceIsTheOnlyWidgetBlockingBridge() {
        val widgetSources = mainSource("widget").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        val blockingSources = widgetSources.filter { it.readText(Charsets.UTF_8).contains("runBlocking") }

        assertEquals(listOf("CountdownWidgetService.kt"), blockingSources.map(File::getName))
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

    private fun manifestSource(): File {
        return listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        ).firstOrNull(File::exists) ?: error("Missing AndroidManifest.xml")
    }
}
