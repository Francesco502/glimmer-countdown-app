# Glimmer 4.0 Home Ordering and Widget Freshness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the home list and widgets share complete, deterministic ordering and refresh correctly when sort preferences or the civil date changes.

**Architecture:** Centralize home filtering/sorting and visible-subset reorder merging in pure policies. Keep widget content loading suspendable, use the application coroutine scope for non-blocking provider work, and deliver civil-time broadcasts through explicit manifest actions.

**Tech Stack:** Kotlin, StateFlow, Room, DataStore, Compose LazyColumn, AppWidgetProvider/RemoteViewsService, JUnit 4.

## Global Constraints

- Keep `versionName=4.0` and `versionCode=23` while this release remains unpublished.
- Keep the existing Song/Glimmer visual language.
- Keep production credentials outside Git.
- Preserve Room database version 10.
- Direct and Play remain separate channels.
- Treat the existing uncommitted widget fixture and follow-home implementation as candidate work to preserve and verify.
- Every new behavior change follows red-green-refactor.

---

## File Map

- `app/src/main/java/com/example/timeapk/ui/home/HomeFilterModels.kt`: shared filter/sort and subset merge policies.
- `app/src/main/java/com/example/timeapk/ui/home/HomeViewModel.kt`: complete event stream, preference persistence, widget refresh.
- `app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt`: send visible-before and visible-after IDs to ViewModel.
- `app/src/test/java/com/example/timeapk/ui/home/HomeSortBehaviorTest.kt`: >150 event filtering and merge invariants.
- `app/src/main/java/com/example/timeapk/widget/WidgetContentResolver.kt`: suspend content load using the same `applyHomeSort` policy.
- `app/src/test/java/com/example/timeapk/widget/WidgetContentResolverTest.kt`: exported-device ordering fixture.
- `app/src/test/resources/widget_sort_3_17_export_anonymized.json`: anonymized real-device event set.
- `app/src/main/java/com/example/timeapk/widget/CountdownAppWidgetProvider.kt`: asynchronous refresh and civil-time broadcasts.
- `app/src/main/java/com/example/timeapk/widget/CountdownWidgetService.kt`: synchronous RemoteViews factory bridge on IO.
- `app/src/main/AndroidManifest.xml`: date/time/timezone receiver actions.
- `app/src/test/java/com/example/timeapk/widget/WidgetOrderingRefreshArchitectureTest.kt`: preference and provider source contracts.

### Task 1: Verify and checkpoint the approved follow-home widget ordering work

**Files:**
- Modify only if a focused test exposes a defect: `app/src/main/java/com/example/timeapk/widget/WidgetContentResolver.kt`
- Existing candidate: `app/src/main/java/com/example/timeapk/ui/home/HomeViewModel.kt`
- Existing candidate: `app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt`
- Existing candidate: `app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt`
- Existing candidate: `app/src/test/java/com/example/timeapk/widget/WidgetContentResolverTest.kt`
- Existing candidate: `app/src/test/java/com/example/timeapk/widget/WidgetOrderingRefreshArchitectureTest.kt`
- Existing candidate: `app/src/test/resources/widget_sort_3_17_export_anonymized.json`

**Interfaces:**
- Consumes: `applyHomeSort(list, sortType, customEventOrderIds, pinnedEventIds)`.
- Produces: `WidgetContentResolver.filterAndSortStates(states, config, pinnedEventIds, customEventOrder, homeSortType)` matching home for `SORT_HOME`.

- [ ] **Step 1: Inspect the candidate diff before touching it**

Run: `git diff -- app/src/main/java/com/example/timeapk/ui/home/HomeViewModel.kt app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt app/src/main/java/com/example/timeapk/widget/WidgetContentResolver.kt app/src/test/java/com/example/timeapk/widget/WidgetContentResolverTest.kt app/src/test/java/com/example/timeapk/widget/WidgetOrderingRefreshArchitectureTest.kt app/src/test/resources/widget_sort_3_17_export_anonymized.json`

Expected: follow-home reads `sortTypeFlow`, the exported fixture contains 22 anonymized rows, and preference mutations refresh widgets after persistence.

- [ ] **Step 2: Run the authoritative fixture and refresh tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.widget.WidgetContentResolverTest --tests com.example.timeapk.widget.WidgetOrderingRefreshArchitectureTest --tests com.example.timeapk.ui.home.HomeSortBehaviorTest`

Expected: all tests pass; if any fail, make only the minimal correction consistent with this interface and rerun.

- [ ] **Step 3: Commit only the verified follow-home slice**

```bash
git add app/src/main/java/com/example/timeapk/ui/home/HomeViewModel.kt app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt app/src/main/java/com/example/timeapk/widget/WidgetContentResolver.kt app/src/test/java/com/example/timeapk/widget/WidgetContentResolverTest.kt app/src/test/java/com/example/timeapk/widget/WidgetOrderingRefreshArchitectureTest.kt app/src/test/resources/widget_sort_3_17_export_anonymized.json
git commit -m "fix: make widgets follow home ordering"
```

Do not stage unrelated 4.0 documentation, Gradle, or release-script edits in this commit.

### Task 2: Filter and search the complete Room event set

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/ui/home/HomeSortBehaviorTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/home/HomeFilterModels.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/home/HomeViewModel.kt`

**Interfaces:**
- Produces: `buildHomeVisibleList(all, filterType, sortType, query, customEventOrderIds, pinnedEventIds): List<EventUiState>`.

- [ ] **Step 1: Add a failing >150-event search test**

```kotlin
@Test
fun buildHomeVisibleList_searchesEventsBeyondFormerCaps() {
    val items = (1..151).map { id ->
        eventState(
            id = id,
            daysRemaining = id.toLong(),
            createdAt = id.toLong()
        ).let { state ->
            if (id == 151) state.copy(event = state.event.copy(title = "needle")) else state
        }
    }

    val result = buildHomeVisibleList(
        all = items,
        filterType = FilterType.All,
        sortType = SortType.ByDays,
        query = "needle",
        customEventOrderIds = emptyList(),
        pinnedEventIds = emptyList()
    )

    assertEquals(listOf(151), result.map { it.event.id })
}

@Test
fun buildHomeVisibleList_withoutSearchReturnsEveryEvent() {
    val items = (1..175).map { eventState(it, it.toLong(), it.toLong()) }
    assertEquals(
        175,
        buildHomeVisibleList(
            items, FilterType.All, SortType.ByDays, "", emptyList(), emptyList()
        ).size
    )
}
```

- [ ] **Step 2: Run the focused tests and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.home.HomeSortBehaviorTest`

Expected: compilation fails because `buildHomeVisibleList` is missing.

- [ ] **Step 3: Add the complete-list policy**

```kotlin
internal fun buildHomeVisibleList(
    all: List<EventUiState>,
    filterType: FilterType,
    sortType: SortType,
    query: String,
    customEventOrderIds: List<Int>,
    pinnedEventIds: List<Int>
): List<EventUiState> {
    val categoryFiltered = when (filterType) {
        FilterType.All -> all
        FilterType.Birthday -> all.filter { it.event.category == CATEGORY_BIRTHDAY }
        FilterType.Anniversary -> all.filter { it.event.category == CATEGORY_ANNIVERSARY }
        FilterType.Other -> all.filter { it.event.category == CATEGORY_OTHER }
    }
    val normalizedQuery = query.trim().lowercase()
    val searched = if (normalizedQuery.isEmpty()) categoryFiltered else categoryFiltered.filter { state ->
        state.event.title.lowercase().contains(normalizedQuery) ||
            state.event.note.lowercase().contains(normalizedQuery) ||
            state.event.category.lowercase().contains(normalizedQuery)
    }
    return applyHomeSort(searched, sortType, customEventOrderIds, pinnedEventIds)
}
```

- [ ] **Step 4: Remove both caps and route home/calendar through the policy**

```kotlin
// Delete MAX_UPCOMING_ITEMS, MAX_PAST_ITEMS and baseHomeUiState.
val homeUiState = combine(allHomeUiState, filterInputFlow, orderInputFlow) { all, filter, order ->
    buildHomeVisibleList(
        all, filter.filterType, filter.sortType, filter.query,
        order.customEventOrderIds, order.pinnedEventIds
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

val calendarUiState = homeUiState
```

If the calendar needs a separate sharing policy, use a second `combine` calling the same pure function; do not restore truncation.

- [ ] **Step 5: Run sorting, calendar, and timeline regressions**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.home.HomeSortBehaviorTest --tests com.example.timeapk.ui.home.HomeCalendarModelTest --tests com.example.timeapk.ui.home.HomeTimelineModelsTest`

Expected: PASS.

- [ ] **Step 6: Commit complete-list filtering**

```bash
git add app/src/main/java/com/example/timeapk/ui/home/HomeFilterModels.kt app/src/main/java/com/example/timeapk/ui/home/HomeViewModel.kt app/src/test/java/com/example/timeapk/ui/home/HomeSortBehaviorTest.kt
git commit -m "fix: search and filter the full event list"
```

### Task 3: Merge visible reorders into the global custom order

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/ui/home/HomeSortBehaviorTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/home/HomeFilterModels.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt`

**Interfaces:**
- Produces: `mergeVisibleOrderIntoGlobalOrder(globalIds, visibleIds, reorderedVisibleIds): List<Int>`.
- Produces: `HomeViewModel.updateCustomEventOrder(visibleIds, reorderedVisibleIds)`.

- [ ] **Step 1: Add hidden-slot and new-ID tests**

```kotlin
@Test
fun mergeVisibleOrderIntoGlobalOrder_preservesHiddenSlots() {
    assertEquals(
        listOf(3, 2, 1, 4, 5),
        mergeVisibleOrderIntoGlobalOrder(
            globalIds = listOf(1, 2, 3, 4, 5),
            visibleIds = listOf(1, 3, 5),
            reorderedVisibleIds = listOf(3, 1, 5)
        )
    )
}

@Test
fun mergeVisibleOrderIntoGlobalOrder_appendsUnstoredIdsExactlyOnce() {
    assertEquals(
        listOf(1, 2, 3, 4),
        mergeVisibleOrderIntoGlobalOrder(
            globalIds = listOf(1, 2),
            visibleIds = listOf(1, 3, 4),
            reorderedVisibleIds = listOf(1, 3, 4)
        )
    )
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.home.HomeSortBehaviorTest`

Expected: compilation fails because the merge function is missing.

- [ ] **Step 3: Implement slot-preserving merge**

```kotlin
internal fun mergeVisibleOrderIntoGlobalOrder(
    globalIds: List<Int>,
    visibleIds: List<Int>,
    reorderedVisibleIds: List<Int>
): List<Int> {
    val visibleSet = visibleIds.toSet()
    val reordered = reorderedVisibleIds.filter { it in visibleSet }.distinct()
    require(reordered.toSet() == visibleSet) { "Visible reorder must contain every visible ID once" }
    val completeGlobal = (globalIds + visibleIds).distinct()
    val replacements = reordered.iterator()
    return completeGlobal.map { id ->
        if (id in visibleSet) replacements.next() else id
    }
}
```

- [ ] **Step 4: Persist a complete canonical order in ViewModel**

```kotlin
fun updateCustomEventOrder(visibleIds: List<Int>, reorderedVisibleIds: List<Int>) {
    viewModelScope.launch {
        val allEvents = repository.getAllEventsSnapshot()
        val stored = userPrefs.customEventOrderFlow.first()
        val defaultIds = allEvents.sortedByDescending { it.createdAt }.map { it.id }
        val globalIds = (stored + defaultIds).filter { id -> allEvents.any { it.id == id } }.distinct()
        val merged = mergeVisibleOrderIntoGlobalOrder(globalIds, visibleIds, reorderedVisibleIds)
        userPrefs.setCustomEventOrder(merged)
        WidgetUpdater.refreshCountdownWidgets(application)
    }
}
```

- [ ] **Step 5: Pass before/after visible IDs from drag end**

```kotlin
val visibleIdsBeforeDrag = displayedList.map { it.event.id }
// onDragEnd:
if (dragEnabled) {
    viewModel.updateCustomEventOrder(
        visibleIds = visibleIdsBeforeDrag,
        reorderedVisibleIds = orderedList.map { it.event.id }
    )
}
```

Remove direct `prefs.setCustomEventOrder(orderedList.map { it.event.id })` from `HomeScreen`.

- [ ] **Step 6: Run merge and source-contract tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.home.HomeSortBehaviorTest --tests com.example.timeapk.widget.WidgetOrderingRefreshArchitectureTest`

Expected: PASS after updating the architecture assertion to require `viewModel.updateCustomEventOrder` and a post-persistence widget refresh in ViewModel.

- [ ] **Step 7: Commit global reorder merging**

```bash
git add app/src/main/java/com/example/timeapk/ui/home/HomeFilterModels.kt app/src/main/java/com/example/timeapk/ui/home/HomeViewModel.kt app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt app/src/test/java/com/example/timeapk/ui/home/HomeSortBehaviorTest.kt app/src/test/java/com/example/timeapk/widget/WidgetOrderingRefreshArchitectureTest.kt
git commit -m "fix: preserve hidden events during reorder"
```

### Task 4: Remove blocking work from widget provider callbacks

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/widget/WidgetOrderingRefreshArchitectureTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/widget/WidgetContentResolver.kt`
- Modify: `app/src/main/java/com/example/timeapk/widget/CountdownWidgetService.kt`
- Modify: `app/src/main/java/com/example/timeapk/widget/CountdownAppWidgetProvider.kt`

**Interfaces:**
- Produces: `suspend fun WidgetContentResolver.load(context, appWidgetId, sizeBucket)`.
- Produces: `suspend fun refreshWidgets(context, appWidgetManager, appWidgetIds)` and non-blocking `refreshAllWidgets(context)`.

- [ ] **Step 1: Add a failing no-provider-runBlocking contract**

```kotlin
@Test
fun providerUsesSuspendLoadingAndAlwaysFinishesPendingResult() {
    val provider = mainSource("widget/CountdownAppWidgetProvider.kt").readText()
    val resolver = mainSource("widget/WidgetContentResolver.kt").readText()
    assertFalse(provider.contains("runBlocking"))
    assertFalse(resolver.contains("runBlocking"))
    assertTrue(provider.contains("goAsync()"))
    assertTrue(provider.contains("pendingResult.finish()"))
    assertTrue(provider.contains("finally"))
    assertTrue(resolver.contains("suspend fun load("))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.widget.WidgetOrderingRefreshArchitectureTest`

Expected: FAIL because provider/resolver contain `runBlocking` and no `goAsync` path.

- [ ] **Step 3: Make content loading suspend directly**

```kotlin
internal object WidgetContentResolver {
    suspend fun load(context: Context, sizeBucket: Int): WidgetContentSnapshot =
        load(context, null, sizeBucket)

    suspend fun load(
        context: Context,
        appWidgetId: Int?,
        sizeBucket: Int
    ): WidgetContentSnapshot {
        val app = context.applicationContext as? TimeApplication
            ?: return WidgetContentSnapshot(
                items = emptyList(),
                textStyle = WidgetStylePolicy.resolve(sizeBucket, 1f)
            )
        val prefs = app.userPrefs
        val milestones = prefs.customMilestonesFlow.first()
        val pinnedEventIds = prefs.pinnedEventIdsFlow.first()
        val customEventOrder = prefs.customEventOrderFlow.first()
        val homeSortType = SortType.entries.getOrNull(prefs.sortTypeFlow.first()) ?: SortType.Custom
        val preferredMode = prefs.dateDeltaDisplayModeFlow.first()
        val perEventModes = prefs.perEventDateDeltaDisplayModesFlow.first()
        val showMilestone = prefs.showMilestoneFlow.first()
        val smartMilestonesEnabled = prefs.smartMilestonesEnabledFlow.first()
        val configRepository = WidgetConfigRepository(context)
        val config = if (appWidgetId != null) {
            configRepository.getConfigForWidget(appWidgetId)
        } else {
            configRepository.getDefaultConfig()
        }.sanitize()
        val textStyle = WidgetStylePolicy.resolve(sizeBucket, config.fontScale, config.densityMode)
        val renderStyle = WidgetRenderPolicy.resolve(config, WidgetThemeResolver.resolve(context))
        val ordered = app.repository.getAllEventsSnapshot()
            .map { it.toEventUiState(milestones, smartMilestonesEnabled) }
            .let { filterAndSortStates(it, config, pinnedEventIds, customEventOrder, homeSortType) }
        val renderedItems = ordered.map { state ->
            buildRenderedItem(
                context = context,
                state = state,
                sizeBucket = sizeBucket,
                preferredMode = perEventModes[state.event.id] ?: preferredMode,
                showMilestone = showMilestone,
                showLunarPrefix = config.showLunarPrefix,
                textStyle = textStyle
            )
        }
        return WidgetContentSnapshot(renderedItems, textStyle, renderStyle)
    }
}
```

- [ ] **Step 4: Bridge the synchronous RemoteViews factory only on IO**

```kotlin
override fun onDataSetChanged() {
    val identityToken = Binder.clearCallingIdentity()
    try {
        val snapshot = runBlocking(Dispatchers.IO) {
            WidgetContentResolver.load(
                context,
                appWidgetId.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID },
                widgetSizeBucket
            )
        }
        items.clear()
        items.addAll(snapshot.items)
        textStyle = snapshot.textStyle
        renderStyle = snapshot.renderStyle
    } finally {
        Binder.restoreCallingIdentity(identityToken)
    }
}
```

`RemoteViewsFactory.onDataSetChanged` is a synchronous binder callback; this is the only allowed widget blocking bridge and it runs off the broadcast/UI thread.

- [ ] **Step 5: Launch provider refresh through the application scope and finish broadcasts**

```kotlin
private fun launchRefresh(
    context: Context,
    appWidgetManager: AppWidgetManager,
    ids: IntArray,
    pendingResult: PendingResult
) {
    val app = context.applicationContext as TimeApplication
    app.launchAppTask {
        try {
            withContext(Dispatchers.IO) { refreshWidgets(context, appWidgetManager, ids) }
        } finally {
            pendingResult.finish()
        }
    }
}

override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
    launchRefresh(context, manager, ids, goAsync())
}
```

Make `updateSingleWidget` suspend so it can call `WidgetConfigRepository.getConfigForWidget` directly. `refreshAllWidgets(context)` should call `app.launchAppTask { refreshWidgets(context, appWidgetManager, getAppWidgetIds(context, appWidgetManager)) }` rather than block its caller.

- [ ] **Step 6: Run widget unit tests and compile both channels**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests 'com.example.timeapk.widget.*' compileDirectDebugKotlin compilePlayDebugKotlin`

Expected: PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit non-blocking provider work**

```bash
git add app/src/main/java/com/example/timeapk/widget/WidgetContentResolver.kt app/src/main/java/com/example/timeapk/widget/CountdownWidgetService.kt app/src/main/java/com/example/timeapk/widget/CountdownAppWidgetProvider.kt app/src/test/java/com/example/timeapk/widget/WidgetOrderingRefreshArchitectureTest.kt
git commit -m "perf: move widget refresh off broadcast thread"
```

### Task 5: Refresh every widget at civil-date boundaries

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/widget/WidgetOrderingRefreshArchitectureTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/widget/CountdownAppWidgetProvider.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `launchRefresh(context, appWidgetManager, ids, pendingResult)`.
- Produces: refresh handling for `ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED`, and `ACTION_TIMEZONE_CHANGED`.

- [ ] **Step 1: Add failing manifest/action tests**

```kotlin
@Test
fun widgetRefreshesForCivilDateAndClockChanges() {
    val manifest = existingFile("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml").readText()
    val provider = mainSource("widget/CountdownAppWidgetProvider.kt").readText()
    listOf(
        "android.intent.action.DATE_CHANGED",
        "android.intent.action.TIME_SET",
        "android.intent.action.TIMEZONE_CHANGED"
    ).forEach { assertTrue(manifest.contains(it)) }
    assertTrue(provider.contains("Intent.ACTION_DATE_CHANGED"))
    assertTrue(provider.contains("Intent.ACTION_TIME_CHANGED"))
    assertTrue(provider.contains("Intent.ACTION_TIMEZONE_CHANGED"))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.widget.WidgetOrderingRefreshArchitectureTest`

Expected: FAIL because the civil-time actions are missing.

- [ ] **Step 3: Add manifest actions**

```xml
<action android:name="android.intent.action.DATE_CHANGED" />
<action android:name="android.intent.action.TIME_SET" />
<action android:name="android.intent.action.TIMEZONE_CHANGED" />
```

Keep `APPWIDGET_UPDATE` and `CONFIGURATION_CHANGED` in the same provider filter.

- [ ] **Step 4: Route actions to all-widget refresh**

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val refreshActions = setOf(
        Intent.ACTION_CONFIGURATION_CHANGED,
        Intent.ACTION_DATE_CHANGED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED
    )
    if (intent.action in refreshActions) {
        val manager = AppWidgetManager.getInstance(context)
        launchRefresh(context, manager, getAppWidgetIds(context, manager), goAsync())
        return
    }
    super.onReceive(context, intent)
}
```

- [ ] **Step 5: Run the architecture test and manifest merge**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.widget.WidgetOrderingRefreshArchitectureTest processDirectDebugMainManifest`

Expected: PASS.

- [ ] **Step 6: Run emulator broadcast smoke**

Run with at least one configured widget:

```bash
adb shell am broadcast -a android.intent.action.DATE_CHANGED
adb shell am broadcast -a android.intent.action.TIMEZONE_CHANGED
```

Expected: each configured widget list reloads and values reflect the new `LocalDate.now()` calculation; multiple widget configurations remain isolated.

- [ ] **Step 7: Commit civil-date refresh**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/example/timeapk/widget/CountdownAppWidgetProvider.kt app/src/test/java/com/example/timeapk/widget/WidgetOrderingRefreshArchitectureTest.kt
git commit -m "fix: refresh widgets at date boundaries"
```

### Task 6: Verify home and widget behavior end to end

**Files:**
- Modify: `docs/RELEASE_CHECKLIST.md`

- [ ] **Step 1: Run all home/widget JVM tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests 'com.example.timeapk.ui.home.*' --tests 'com.example.timeapk.widget.*'`

Expected: PASS, including the anonymized 3.17 export fixture.

- [ ] **Step 2: Exercise all three home sort modes with pinned rows**

Expected on emulator: `Custom`, `ByDays`, and `ByDate` produce identical relative order in a `SORT_HOME` widget; pinned IDs remain at the top in pinned preference order.

- [ ] **Step 3: Exercise a filtered reorder**

Expected: reorder three visible birthday rows, clear the filter, and confirm every hidden row remains in its original global slot.

- [ ] **Step 4: Record launcher, multi-instance, sort, and midnight evidence**

```markdown
- [x] 3.17 export ordering fixture
- [x] Home Custom/ByDays/ByDate parity with SORT_HOME
- [x] Pinned rows remain first
- [x] Filtered reorder preserves hidden slots
- [x] Two widget instances retain separate configuration
- [x] DATE_CHANGED/TIME_SET/TIMEZONE_CHANGED refresh smoke
```

- [ ] **Step 5: Commit verification evidence**

```bash
git add docs/RELEASE_CHECKLIST.md
git commit -m "docs: record home and widget verification"
```
