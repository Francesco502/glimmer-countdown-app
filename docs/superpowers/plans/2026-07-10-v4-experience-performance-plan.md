# Glimmer 4.0 Experience, Accessibility, and Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the calendar and controls readable/reachable at dark theme and enlarged fonts, expose correct accessibility actions, and remove share/startup work from the main thread.

**Architecture:** Use Material semantic colors and one outer calendar scroll container, preserve 36dp visual marks inside 48dp interaction containers, and model wheel adjustment as a pure index operation. Move bitmap work into explicit Default/IO stages and mirror locale state into synchronous SharedPreferences for startup while keeping DataStore authoritative.

**Tech Stack:** Jetpack Compose/Material 3, Compose semantics and UI tests, Kotlin coroutines, Android Bitmap/MediaStore/FileProvider, SharedPreferences/DataStore, JUnit 4.

## Global Constraints

- Keep `versionName=4.0` and `versionCode=23` while this release remains unpublished.
- Preserve the Song/Glimmer visual language; do not redesign navigation.
- All named touch targets are at least 48dp.
- Selected calendar text/background reaches WCAG AA 4.5:1 in light and dark themes.
- Calendar actions remain reachable at 150% and 200% system font scale.
- No `runBlocking` in `MainActivity` or `CountdownAppWidgetProvider`.
- Keep production credentials outside Git and Room at version 10.

---

## File Map

- `app/src/main/java/com/example/timeapk/ui/theme/SongComponents.kt`: calendar selected colors and color-swatch hit target.
- `app/src/test/java/com/example/timeapk/ui/theme/ColorContrastGuardrailTest.kt`: AA selected-cell regression.
- `app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt`: outer calendar scroll, search/tabs/empty add target.
- `app/src/test/java/com/example/timeapk/ui/home/HomeInteractionPolicyTest.kt`: source layout and 48dp contracts.
- `app/src/main/java/com/example/timeapk/ui/settings/SettingsComponents.kt`: one-node radio semantics and 48dp toggle.
- `app/src/main/java/com/example/timeapk/ui/settings/WidgetSettingsContent.kt`: merged switch-row semantics.
- `app/src/main/java/com/example/timeapk/ui/components/SnapWheelPicker.kt`: 48dp rows and adjustable semantics.
- `app/src/test/java/com/example/timeapk/ui/components/SnapWheelPickerTest.kt`: increment/decrement policy.
- `app/src/androidTest/java/com/example/timeapk/ui/settings/SettingsAccessibilityTest.kt`: single-node radio/switch assertions.
- `app/src/androidTest/java/com/example/timeapk/ui/components/SnapWheelPickerAccessibilityTest.kt`: adjustable wheel assertions.
- `app/src/main/java/com/example/timeapk/ui/detail/ShareImageCoordinator.kt`: Default render and IO compression/cache/save.
- `app/src/main/java/com/example/timeapk/ui/detail/ShareImageStore.kt`: delete partial files on failure.
- `app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt`: busy state and background coordinator calls.
- `app/src/test/java/com/example/timeapk/ui/detail/ShareImageArchitectureTest.kt`: dispatcher/source contract.
- `app/src/main/java/com/example/timeapk/LocalePreferenceMirror.kt`: synchronous locale mirror.
- `app/src/main/java/com/example/timeapk/MainActivity.kt`: non-blocking startup/migration.
- `app/src/main/java/com/example/timeapk/data/UserPreferencesRepository.kt`: dual-write language changes.
- `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`: startup source contract.
- `app/src/main/res/values*/strings.xml`: accessibility and failure labels.

### Task 1: Fix dark selected-calendar contrast

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/ui/theme/ColorContrastGuardrailTest.kt`
- Modify: `app/src/test/java/com/example/timeapk/ui/theme/SongUiSourceConsistencyTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/theme/SongComponents.kt`

**Interfaces:**
- Produces: selected calendar uses `colorScheme.primary` background and `colorScheme.onPrimary` content.

- [ ] **Step 1: Add light/dark selected-pair contrast tests**

```kotlin
// ColorContrastGuardrailTest.kt
@Test
fun selectedCalendarPair_meetsAaInLightAndDarkPalettes() {
    val light = ColorContrastGuardrail.contrastRatio(Color.White, SongLightPrimary)
    val dark = ColorContrastGuardrail.contrastRatio(Color(0xFF1F1F1F), SongDarkPrimary)
    assertTrue(light >= ColorContrastGuardrail.AaNormalText)
    assertTrue(dark >= ColorContrastGuardrail.AaNormalText)
}

// SongUiSourceConsistencyTest.kt
@Test
fun selectedCalendarCell_usesMaterialOnPrimaryPair() {
    val source = readSource("ui/theme/SongComponents.kt")
    val cell = source.substringAfter("fun SongCalendarCell(")
        .substringBefore("fun SongDivider")
    assertTrue(cell.contains("selected -> MaterialTheme.colorScheme.primary"))
    assertTrue(cell.contains("selected -> MaterialTheme.colorScheme.onPrimary"))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.theme.ColorContrastGuardrailTest --tests com.example.timeapk.ui.theme.SongUiSourceConsistencyTest`

Expected: the source contract fails because selected uses `PaperDeep` with primary text.

- [ ] **Step 3: Apply the semantic selected pair to every selected-cell label**

```kotlin
val backgroundColor = when {
    selected -> MaterialTheme.colorScheme.primary
    hasEvents -> SongPalette.PaperWarm.copy(alpha = 0.45f)
    else -> Color.Transparent
}
val dayColor = when {
    selected -> MaterialTheme.colorScheme.onPrimary
    hasEvents -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurface
}
val indicatorColor = if (selected) {
    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
} else {
    MaterialTheme.colorScheme.primary
}
```

Use `dayColor` for the day number and `indicatorColor` for event count/marker. Keep the today seal marker and border so today/selected/event states are not color-only.

- [ ] **Step 4: Run contrast/theme regressions**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests 'com.example.timeapk.ui.theme.*'`

Expected: PASS.

- [ ] **Step 5: Commit calendar contrast**

```bash
git add app/src/main/java/com/example/timeapk/ui/theme/SongComponents.kt app/src/test/java/com/example/timeapk/ui/theme/ColorContrastGuardrailTest.kt app/src/test/java/com/example/timeapk/ui/theme/SongUiSourceConsistencyTest.kt
git commit -m "fix: restore selected calendar contrast"
```

### Task 2: Make the entire month calendar scroll and reflow

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/ui/home/HomeInteractionPolicyTest.kt`
- Modify: `app/src/test/java/com/example/timeapk/ui/theme/SongUiSourceConsistencyTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt`

**Interfaces:**
- Produces: one outer `verticalScroll` and no nested selected-event `LazyColumn`.

- [ ] **Step 1: Replace fixed-height assertions with outer-scroll assertions**

```kotlin
@Test
fun monthCalendarScrollsAsOneSurfaceAtLargeFontScale() {
    val source = mainSource("ui/home/HomeScreen.kt").readText()
    val month = source.substringAfter("private fun MonthCalendarView(")
        .substringBefore("private fun CalendarOccurrenceRow(")
    assertTrue(month.contains(".verticalScroll(rememberScrollState())"))
    assertFalse(month.contains("LazyColumn("))
    assertFalse(month.contains(".weight(1f, fill = true)"))
    assertTrue(month.contains(".heightIn(min = 48.dp)"))
    assertFalse(month.contains("max = 72.dp"))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.home.HomeInteractionPolicyTest --tests com.example.timeapk.ui.theme.SongUiSourceConsistencyTest`

Expected: FAIL against the inner weighted `LazyColumn` and capped cells.

- [ ] **Step 3: Use one outer scroll and content-sized event rows**

```kotlin
// Outer Column modifier replacement:
modifier = modifier
    .fillMaxSize()
    .verticalScroll(rememberScrollState())
    .padding(horizontal = 16.dp, vertical = 8.dp)

// Day and placeholder cell modifier replacement:
Modifier.weight(1f).heightIn(min = 48.dp)

// Selected-event container replacement:
val selectedEvents = eventsByDate[pickedDate].orEmpty()
if (selectedEvents.isEmpty()) {
    Text(
        text = stringResource(R.string.calendar_no_events),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
} else {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        selectedEvents.forEach { occurrence ->
            key("${occurrence.eventState.event.id}-${occurrence.date}") {
                CalendarOccurrenceRow(occurrence, onEventClick, onEventLongClick)
            }
        }
    }
}
Spacer(Modifier.height(88.dp))
```

Move the existing selected-date and lunar labels immediately before the selected event block. Remove both `max = 72.dp` caps so text can reflow.

- [ ] **Step 4: Run source tests and compile Compose**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.home.HomeInteractionPolicyTest --tests com.example.timeapk.ui.theme.SongUiSourceConsistencyTest compileDirectDebugKotlin`

Expected: PASS.

- [ ] **Step 5: Capture 150% and 200% font screenshots**

Run before each capture:

```bash
adb shell settings put system font_scale 1.5
adb shell settings put system font_scale 2.0
```

Expected: after scrolling, selected-day rows and the persistent add action are reachable; no day text is clipped.

- [ ] **Step 6: Commit calendar reflow**

```bash
git add app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt app/src/test/java/com/example/timeapk/ui/home/HomeInteractionPolicyTest.kt app/src/test/java/com/example/timeapk/ui/theme/SongUiSourceConsistencyTest.kt
git commit -m "fix: make month calendar reflow and scroll"
```

### Task 3: Give named controls 48dp targets and make empty-state add actionable

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/ui/home/HomeInteractionPolicyTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/theme/SongComponents.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/event/EventEntryScreen.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt`

**Interfaces:**
- Produces: `EmptyState(onAddEvent: () -> Unit, modifier: Modifier = Modifier)`.
- Produces: 48dp outer target with 36dp visual swatch.

- [ ] **Step 1: Add failing target/source contracts**

```kotlin
@Test
fun homeNamedControlsAndEmptyAddMeetTouchTargetContract() {
    val home = mainSource("ui/home/HomeScreen.kt").readText()
    val empty = home.substringAfter("private fun EmptyState(")
        .substringBefore("fun EventCard(")
    val search = home.substringAfter("private fun HomeOverflowSearchField(")
        .substringBefore("private fun EmptyState(")
    assertTrue(empty.contains("onAddEvent: () -> Unit"))
    assertTrue(empty.contains("role = Role.Button"))
    assertTrue(empty.contains(".sizeIn(minWidth = 48.dp, minHeight = 48.dp)"))
    assertTrue(search.contains(".size(48.dp)"))
}

@Test
fun colorSwatchKeepsVisualSizeInside48DpTarget() {
    val swatch = mainSource("ui/theme/SongComponents.kt").readText()
        .substringAfter("fun SongColorSwatch(")
        .substringBefore("fun SongHexColorField(")
    assertTrue(swatch.contains(".sizeIn(minWidth = 48.dp, minHeight = 48.dp)"))
    assertTrue(swatch.contains(".size(size)"))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.home.HomeInteractionPolicyTest`

Expected: FAIL because search clear is 26dp and empty-state add is decorative.

- [ ] **Step 3: Make the empty add mark a real button**

```kotlin
@Composable
private fun EmptyState(
    onAddEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addLabel = stringResource(R.string.cd_add_event)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(role = Role.Button, onClick = onAddEvent)
                .semantics { contentDescription = addLabel }
                .padding(16.dp)
        ) {
            SongLineIcon(kind = SongLineIconKind.Add, contentDescription = null, size = 64.dp)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.home_empty_title))
        }
    }
}
```

Pass `navigateToItemEntry` to every `EmptyState` call.

- [ ] **Step 4: Enlarge clear/tab containers without enlarging icons**

```kotlin
val borderWidth = if (selected) 1.dp else SongDesignTokens.BorderWidth.dp
val sealTint = if (color.luminance() > 0.5f) {
    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
} else {
    MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
}
Box(
    modifier = Modifier
        .size(48.dp)
        .clickable(role = Role.Button) { onSearchQueryChange("") },
    contentAlignment = Alignment.Center
) {
    SongLineIcon(SongLineIconKind.Close, stringResource(R.string.date_picker_cancel), size = 18.dp)
}
```

Apply `.heightIn(min = 48.dp)` to each home mode tab and `.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` to every icon-only named action touched by this audit.

- [ ] **Step 5: Refactor color swatch into outer target and inner mark**

```kotlin
Box(
    modifier = modifier
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .then(if (contentDescription != null) Modifier.semantics {
            this.contentDescription = contentDescription
        } else Modifier)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    contentAlignment = Alignment.Center
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color, shape)
            .border(BorderStroke(borderWidth, borderColor), shape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) SongLineIcon(SongLineIconKind.Seal, null, size = 16.dp, tint = sealTint)
    }
}
```

- [ ] **Step 6: Run home/theme/event source regressions**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.home.HomeInteractionPolicyTest --tests com.example.timeapk.ui.theme.SongUiSourceConsistencyTest --tests com.example.timeapk.ui.event.EventEntryInputFocusTest`

Expected: PASS.

- [ ] **Step 7: Commit touch-target fixes**

```bash
git add app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt app/src/main/java/com/example/timeapk/ui/theme/SongComponents.kt app/src/main/java/com/example/timeapk/ui/event/EventEntryScreen.kt app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt app/src/test/java/com/example/timeapk/ui/home/HomeInteractionPolicyTest.kt
git commit -m "fix: enlarge and clarify interactive targets"
```

### Task 4: Expose single-node switch/radio and adjustable wheel semantics

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/ui/components/SnapWheelPickerTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/components/SnapWheelPicker.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/settings/SettingsComponents.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/settings/WidgetSettingsContent.kt`
- Create: `app/src/androidTest/java/com/example/timeapk/ui/settings/SettingsAccessibilityTest.kt`
- Create: `app/src/androidTest/java/com/example/timeapk/ui/components/SnapWheelPickerAccessibilityTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Interfaces:**
- Produces: `adjustedWheelIndex(selectedIndex, itemCount, delta): Int`.
- Produces: radio rows with `selectable` parent and `RadioButton(onClick = null)`.

- [ ] **Step 1: Add wheel adjustment boundary tests**

```kotlin
@Test
fun adjustedWheelIndex_incrementsDecrementsAndClamps() {
    assertEquals(3, adjustedWheelIndex(2, 5, 1))
    assertEquals(1, adjustedWheelIndex(2, 5, -1))
    assertEquals(0, adjustedWheelIndex(0, 5, -1))
    assertEquals(4, adjustedWheelIndex(4, 5, 1))
}
```

- [ ] **Step 2: Add instrumentation semantics expectations**

```kotlin
@Test
fun radioAndSwitchRowsExposeOneInteractiveNodeEach() {
    composeRule.setContent {
        Column {
            SettingsRadioRow("Option A", selected = true, onClick = {})
            WidgetSwitchRow("Show lunar date", checked = true, onCheckedChange = {})
        }
    }
    composeRule.onAllNodes(hasClickAction() and hasText("Option A")).assertCountEquals(1)
    composeRule.onAllNodes(hasClickAction() and hasText("Show lunar date")).assertCountEquals(1)
}

@Test
fun wheelExposesCurrentValueAndAdjustmentAction() {
    var selected by mutableStateOf(2)
    composeRule.setContent {
        SnapWheelPicker(
            items = (1..4).toList(),
            selectedItem = selected,
            onItemSelected = { selected = it },
            itemLabel = Int::toString
        )
    }
    val matcher = SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription,
        "2"
    )
    composeRule.onNode(matcher).performSemanticsAction(SemanticsActions.SetProgress) { action ->
        assertTrue(action(2f))
    }
    composeRule.runOnIdle { assertEquals(3, selected) }
}
```

Place the first test in package `com.example.timeapk.ui.settings` and the second in `com.example.timeapk.ui.components`, each with `createComposeRule()` and the standard Compose test imports.

- [ ] **Step 3: Run JVM tests and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.components.SnapWheelPickerTest`

Expected: compilation fails because `adjustedWheelIndex` is missing.

- [ ] **Step 4: Add wheel index policy and 48dp default**

```kotlin
internal fun adjustedWheelIndex(selectedIndex: Int, itemCount: Int, delta: Int): Int {
    if (itemCount <= 0) return 0
    return (selectedIndex + delta).coerceIn(0, itemCount - 1)
}

// SnapWheelPicker parameters:
itemHeight: Dp = 48.dp,
incrementLabel: String? = null,
decrementLabel: String? = null,
itemLabel: (T) -> String

// Inside SnapWheelPicker:
val resolvedIncrementLabel = incrementLabel ?: stringResource(R.string.wheel_increment)
val resolvedDecrementLabel = decrementLabel ?: stringResource(R.string.wheel_decrement)
```

Add the labels in the same task:

```xml
<!-- values/strings.xml and values-zh/strings.xml -->
<string name="wheel_increment">增加</string>
<string name="wheel_decrement">减少</string>
<!-- values-en/strings.xml -->
<string name="wheel_increment">Increase</string>
<string name="wheel_decrement">Decrease</string>
```

- [ ] **Step 5: Add adjustable semantics to the wheel surface**

```kotlin
val currentLabel = items.getOrNull(selectedIndex)?.let(itemLabel).orEmpty()
val adjustableModifier = Modifier.semantics {
    stateDescription = currentLabel
    progressBarRangeInfo = ProgressBarRangeInfo(
        current = selectedIndex.toFloat(),
        range = 0f..items.lastIndex.coerceAtLeast(0).toFloat(),
        steps = (items.size - 2).coerceAtLeast(0)
    )
    setProgress { requested ->
        if (items.isEmpty()) {
            false
        } else {
            val target = requested.toInt().coerceIn(items.indices)
            onItemSelected(items[target])
            true
        }
    }
    customActions = listOf(
        CustomAccessibilityAction(resolvedIncrementLabel) {
            items.getOrNull(adjustedWheelIndex(selectedIndex, items.size, 1))
                ?.let(onItemSelected) != null
        },
        CustomAccessibilityAction(resolvedDecrementLabel) {
            items.getOrNull(adjustedWheelIndex(selectedIndex, items.size, -1))
                ?.let(onItemSelected) != null
        }
    )
}

// Replace the outer Box modifier with this exact chain:
modifier = modifier
    .then(adjustableModifier)
    .height(itemHeight * safeVisibleCount)
```

Do not change the LazyColumn item rendering or selection-overlay drawing in this step; apply `modifier.then(adjustableModifier).height(itemHeight * safeVisibleCount)` to the outer Box.

- [ ] **Step 6: Merge radio and widget switch interaction nodes**

```kotlin
Row(
    modifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .semantics(mergeDescendants = true) {}
) {
    RadioButton(
        selected = selected,
        onClick = null,
        modifier = Modifier.clearAndSetSemantics {}
    )
    Column(
        modifier = Modifier.weight(1f).padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = labelStyle, color = MaterialTheme.colorScheme.onSurface)
        supportingContent?.invoke(this) ?: supportingText?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun WidgetSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val stateDescriptionText = stringResource(if (checked) R.string.toggle_on else R.string.toggle_off)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics(mergeDescendants = true) { stateDescription = stateDescriptionText }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        SongToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            interactive = false,
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}
```

Add `interactive: Boolean = true` to `SongToggle`; apply its `clickable` modifier only when `interactive` is true.

- [ ] **Step 7: Run JVM and connected semantics tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.components.SnapWheelPickerTest --tests com.example.timeapk.ui.settings.SettingsStructureTest connectedDirectDebugAndroidTest`

Expected: PASS with one interactive node per radio/switch row and an adjustable wheel node.

- [ ] **Step 8: Commit semantics fixes**

```bash
git add app/src/main/java/com/example/timeapk/ui/components/SnapWheelPicker.kt app/src/main/java/com/example/timeapk/ui/settings/SettingsComponents.kt app/src/main/java/com/example/timeapk/ui/settings/WidgetSettingsContent.kt app/src/main/res/values app/src/main/res/values-zh app/src/main/res/values-en app/src/test/java/com/example/timeapk/ui/components/SnapWheelPickerTest.kt app/src/androidTest/java/com/example/timeapk/ui/settings/SettingsAccessibilityTest.kt app/src/androidTest/java/com/example/timeapk/ui/components/SnapWheelPickerAccessibilityTest.kt
git commit -m "fix: expose adjustable and single-node semantics"
```

### Task 5: Move share rendering and compression off the UI dispatcher

**Files:**
- Create: `app/src/test/java/com/example/timeapk/ui/detail/ShareImageArchitectureTest.kt`
- Create: `app/src/main/java/com/example/timeapk/ui/detail/ShareImageCoordinator.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/detail/ShareImageStore.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt`

**Interfaces:**
- Produces: `suspend fun renderAndSaveShareImage(context, data, displayName): Uri?`.
- Produces: `suspend fun renderAndCacheShareImage(context, data, displayName): Uri`.

- [ ] **Step 1: Add a failing dispatcher and busy-state contract**

```kotlin
@Test
fun sharePipelineSeparatesCpuAndIoAndDisablesRepeatedActions() {
    val coordinator = mainSource("ui/detail/ShareImageCoordinator.kt")
    assertTrue(coordinator.exists())
    val source = coordinator.readText()
    assertTrue(source.contains("withContext(Dispatchers.Default)"))
    assertTrue(source.contains("withContext(Dispatchers.IO)"))
    val detail = mainSource("ui/detail/DetailScreen.kt").readText()
    assertTrue(detail.contains("var shareInProgress"))
    assertTrue(detail.contains("enabled = !shareInProgress"))
    assertFalse(detail.contains("EventShareImageRenderer().render(shareData)"))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.detail.ShareImageArchitectureTest`

Expected: FAIL because the coordinator is absent and Detail renders on its UI scope.

- [ ] **Step 3: Add explicit Default/IO coordinator functions**

```kotlin
suspend fun renderAndSaveShareImage(
    context: Context,
    data: EventShareCardData,
    displayName: String
): Uri? {
    val bitmap = withContext(Dispatchers.Default) { EventShareImageRenderer().render(data) }
    return try {
        withContext(Dispatchers.IO) { ShareImageStore.saveShareImage(context, bitmap, displayName) }
    } finally {
        bitmap.recycle()
    }
}

suspend fun renderAndCacheShareImage(
    context: Context,
    data: EventShareCardData,
    displayName: String
): Uri {
    val bitmap = withContext(Dispatchers.Default) { EventShareImageRenderer().render(data) }
    return try {
        withContext(Dispatchers.IO) { ShareImageStore.cacheShareImage(context, bitmap, displayName) }
    } finally {
        bitmap.recycle()
    }
}
```

- [ ] **Step 4: Delete partial cache files on compression failure**

```kotlin
return try {
    FileOutputStream(imageFile).use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            "PNG compression failed"
        }
    }
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
} catch (t: Throwable) {
    imageFile.delete()
    throw t
}
```

- [ ] **Step 5: Gate both actions with one busy state**

```kotlin
var shareInProgress by remember { mutableStateOf(false) }

SongDialogButton(
    text = stringResource(R.string.share_save_image),
    enabled = !shareInProgress,
    onClick = {
        scope.launch {
            shareInProgress = true
            try {
                val saved = runCatching {
                    renderAndSaveShareImage(context, shareData, shareImageName)
                }.getOrNull()
                snackbarHostState.showSnackbar(
                    context.getString(if (saved != null) R.string.share_image_saved else R.string.share_image_failed)
                )
            } finally {
                shareInProgress = false
            }
        }
    }
)
```

Implement the send action explicitly:

```kotlin
SongDialogButton(
    text = stringResource(R.string.share_send_image),
    enabled = !shareInProgress,
    onClick = {
        scope.launch {
            shareInProgress = true
            try {
                val uri = runCatching {
                    renderAndCacheShareImage(context, shareData, shareImageName)
                }.getOrNull()
                if (uri != null) {
                    ShareImageStore.shareImage(
                        context,
                        uri,
                        context.getString(R.string.share_chooser_title)
                    )
                    showShareDialog = false
                } else {
                    snackbarHostState.showSnackbar(context.getString(R.string.share_image_failed))
                }
            } finally {
                shareInProgress = false
            }
        }
    }
)
```

- [ ] **Step 6: Run share tests and compile**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests 'com.example.timeapk.ui.detail.*' compileDirectDebugKotlin`

Expected: PASS.

- [ ] **Step 7: Commit background share work**

```bash
git add app/src/main/java/com/example/timeapk/ui/detail/ShareImageCoordinator.kt app/src/main/java/com/example/timeapk/ui/detail/ShareImageStore.kt app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt app/src/test/java/com/example/timeapk/ui/detail/ShareImageArchitectureTest.kt
git commit -m "perf: move share image work off main thread"
```

### Task 6: Remove startup `runBlocking` with a locale mirror

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`
- Create: `app/src/main/java/com/example/timeapk/LocalePreferenceMirror.kt`
- Modify: `app/src/main/java/com/example/timeapk/MainActivity.kt`
- Modify: `app/src/main/java/com/example/timeapk/data/UserPreferencesRepository.kt`

**Interfaces:**
- Produces: `LocalePreferenceMirror.read(context): Int?` and `write(context, mode)`.
- Produces: one-time asynchronous DataStore-to-mirror migration.

- [ ] **Step 1: Add a failing non-blocking startup contract**

```kotlin
@Test
fun mainActivityUsesLocaleMirrorWithoutRunBlocking() {
    val main = mainSource("MainActivity.kt").readText()
    assertFalse(main.contains("runBlocking"))
    assertTrue(main.contains("LocalePreferenceMirror.read(newBase)"))
    assertTrue(main.contains("migrateLocaleMirror"))
    val prefs = mainSource("data/UserPreferencesRepository.kt").readText()
    val setter = prefs.substringAfter("suspend fun setLanguageMode")
        .substringBefore("suspend fun setDateFormatMode")
    assertTrue(setter.contains("LocalePreferenceMirror.write"))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest.mainActivityUsesLocaleMirrorWithoutRunBlocking`

Expected: FAIL because `attachBaseContext` blocks on DataStore.

- [ ] **Step 3: Add the synchronous private mirror**

```kotlin
object LocalePreferenceMirror {
    private const val FILE = "locale_mirror"
    private const val KEY = "language_mode"

    fun read(context: Context): Int? = context
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)
        .takeIf { it.contains(KEY) }
        ?.getInt(KEY, LANG_ZH)

    fun write(context: Context, mode: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY, mode)
            .apply()
    }
}
```

- [ ] **Step 4: Use mirror synchronously and migrate asynchronously once**

```kotlin
override fun attachBaseContext(newBase: Context) {
    val mode = LocalePreferenceMirror.read(newBase) ?: LANG_ZH
    super.attachBaseContext(LocaleUtils.wrapContext(newBase, mode))
}

private fun migrateLocaleMirror() {
    val app = applicationContext as TimeApplication
    lifecycleScope.launch {
        val stored = app.userPrefs.languageModeFlow.first()
        val mirrored = LocalePreferenceMirror.read(this@MainActivity)
        if (mirrored == null || mirrored != stored) {
            LocalePreferenceMirror.write(this@MainActivity, stored)
            if (mirrored != null || stored != LANG_ZH) recreate()
        }
    }
}
```

Call `migrateLocaleMirror()` once from `onCreate` before `setContent`. The mirror write happens before recreation, preventing a loop.

- [ ] **Step 5: Dual-write language changes**

```kotlin
suspend fun setLanguageMode(mode: Int) {
    context.dataStore.edit { it[LANGUAGE_MODE] = mode }
    LocalePreferenceMirror.write(context, mode)
}
```

- [ ] **Step 6: Run source tests and a cold-start locale smoke**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest compileDirectDebugKotlin`

Expected: PASS and no `runBlocking` import in MainActivity.

On emulator: select English, force-stop, launch, select Chinese, force-stop, launch. Expected: first frame uses the stored language in each case, and an upgraded DataStore-only install recreates at most once.

- [ ] **Step 7: Commit non-blocking locale startup**

```bash
git add app/src/main/java/com/example/timeapk/LocalePreferenceMirror.kt app/src/main/java/com/example/timeapk/MainActivity.kt app/src/main/java/com/example/timeapk/data/UserPreferencesRepository.kt app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt
git commit -m "perf: avoid blocking startup for locale"
```

### Task 7: Verify experience and performance acceptance

**Files:**
- Modify: `docs/RELEASE_CHECKLIST.md`

- [ ] **Step 1: Run all affected JVM and connected tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest connectedDirectDebugAndroidTest`

Expected: PASS.

- [ ] **Step 2: Capture dark selected-calendar and 150%/200% screenshots**

Expected: selected day is readable; selected-day content and add action remain reachable by scrolling.

- [ ] **Step 3: Run TalkBack smoke**

Expected: one focus stop per radio/switch row; wheel announces current value and offers increase/decrease; empty add and search clear announce as buttons; no duplicate swatch node.

- [ ] **Step 4: Trace cold start and share action**

Expected: no DataStore wait in `attachBaseContext`; bitmap creation appears on Default worker and PNG compression/file I/O on IO worker; repeat share taps are disabled until completion.

- [ ] **Step 5: Record device/API/font/trace evidence and commit**

```bash
git add docs/RELEASE_CHECKLIST.md
git commit -m "docs: record accessibility and performance verification"
```
