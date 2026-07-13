# Glimmer 4.0 Data Correctness and Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make lunar recurrence, Android backup, JSON import, and system-calendar cleanup preserve correct dates and user data without creating hidden side effects.

**Architecture:** Keep recurrence and import decisions in pure functions with focused JVM tests. Represent calendar cleanup as an explicit sealed outcome and make save/delete flows retain provider IDs whenever cleanup cannot be proven successful.

**Tech Stack:** Kotlin 2.2, Java time, 6tail lunar 1.7.7, Room 2.8.4, Android CalendarContract, Android DataStore, JUnit 4.

## Global Constraints

- Keep `versionName=4.0` and `versionCode=23` while this release remains unpublished.
- Keep the existing Song/Glimmer visual language.
- Keep the production signing key and credentials outside Git.
- Preserve the Room database at version 10; this plan adds no schema migration.
- Direct and Play remain separate application IDs and update channels.
- Preserve existing uncommitted 4.0 widget-sort changes.
- Every behavior change follows red-green-refactor.

---

## File Map

- `app/src/main/java/com/example/timeapk/ui/utils/LunarEventUtils.kt`: single source of lunar yearly occurrence calculation.
- `app/src/test/java/com/example/timeapk/ui/utils/LunarEventUtilsTest.kt`: Gregorian/Lunar New Year boundary regressions.
- `app/src/main/res/xml/backup_rules.xml`: pre-Android-12 backup domains.
- `app/src/main/res/xml/data_extraction_rules.xml`: Android 12+ cloud backup and device-transfer domains.
- `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`: source contract for equivalent backup domains.
- `app/src/main/java/com/example/timeapk/data/EventJson.kt`: strict JSON row decoding and legacy defaults.
- `app/src/main/java/com/example/timeapk/data/BackupImportParser.kt`: duplicate identity and in-file de-duplication.
- `app/src/test/java/com/example/timeapk/data/EventJsonTest.kt`: malformed, legacy, and duplicate import regressions.
- `app/src/main/java/com/example/timeapk/notifications/CalendarCleanupResult.kt`: explicit cleanup result and user-safe error text.
- `app/src/main/java/com/example/timeapk/notifications/ScheduleSyncManager.kt`: CalendarContract deletion and permission/provider result mapping.
- `app/src/main/java/com/example/timeapk/ui/event/EventEntryViewModel.kt`: preserve IDs and error state after failed disable-sync cleanup.
- `app/src/main/java/com/example/timeapk/ui/home/HomeViewModel.kt`: await cleanup before local deletion.
- `app/src/main/java/com/example/timeapk/TimeApp.kt`: keep detail open and show a cleanup error when deletion is blocked.
- `app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt`: await deletion result before closing.
- `app/src/main/res/values/strings.xml`, `values-zh/strings.xml`, `values-en/strings.xml`: cleanup failure copy.

### Task 1: Fix lunar-year anchoring

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/ui/utils/LunarEventUtilsTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/utils/LunarEventUtils.kt`

**Interfaces:**
- Consumes: `buildLunarSolarDateForYear(year: Int, lunarMonth: Int, lunarDay: Int): LocalDate?`.
- Produces: unchanged `getNextLunarOccurrence(originSolarDate: LocalDate, today: LocalDate): LocalDate` and `getPreviousLunarOccurrence(originSolarDate: LocalDate, today: LocalDate): LocalDate`.

- [ ] **Step 1: Add failing Lunar New Year boundary tests**

```kotlin
@Test
fun nextLunarOccurrence_beforeLunarNewYear_searchesCurrentLunarYear() {
    val origin = LocalDate.of(1996, 1, 25) // lunar 1995-12-06
    val pivot = LocalDate.of(2026, 1, 20)  // still lunar year 2025

    val actual = getNextLunarOccurrence(origin, pivot)

    assertEquals(LocalDate.of(2026, 1, 24), actual)
}

@Test
fun previousLunarOccurrence_beforeLunarNewYear_usesCurrentLunarYear() {
    val origin = LocalDate.of(1996, 1, 25)
    val pivot = LocalDate.of(2026, 1, 20)

    assertEquals(LocalDate.of(2025, 1, 5), getPreviousLunarOccurrence(origin, pivot))
}

@Test
fun nextLunarOccurrence_onOccurrenceDay_returnsPivotDay() {
    val origin = LocalDate.of(1996, 1, 25)
    val pivot = LocalDate.of(2026, 1, 24)

    assertEquals(pivot, getNextLunarOccurrence(origin, pivot))
}
```

- [ ] **Step 2: Run the focused test and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.utils.LunarEventUtilsTest`

Expected: the before-Lunar-New-Year cases fail because the search starts at Gregorian `today.year`.

- [ ] **Step 3: Anchor both searches to the pivot's lunar year**

```kotlin
private fun lunarYearOf(date: LocalDate): Int =
    Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar.year

fun getNextLunarOccurrence(originSolarDate: LocalDate, today: LocalDate): LocalDate {
    val originLunar = Solar.fromYmd(
        originSolarDate.year,
        originSolarDate.monthValue,
        originSolarDate.dayOfMonth
    ).lunar
    val startLunarYear = lunarYearOf(today)
    for (year in startLunarYear..startLunarYear + 8) {
        val candidate = buildLunarSolarDateForYear(year, originLunar.month, originLunar.day)
        if (candidate != null && !candidate.isBefore(today)) return candidate
    }
    return originSolarDate
}

fun getPreviousLunarOccurrence(originSolarDate: LocalDate, today: LocalDate): LocalDate {
    val originLunar = Solar.fromYmd(
        originSolarDate.year,
        originSolarDate.monthValue,
        originSolarDate.dayOfMonth
    ).lunar
    val startLunarYear = lunarYearOf(today)
    for (year in startLunarYear downTo startLunarYear - 8) {
        val candidate = buildLunarSolarDateForYear(year, originLunar.month, originLunar.day)
        if (candidate != null && !candidate.isAfter(today)) return candidate
    }
    return originSolarDate
}
```

Keep the existing leap-month handling and fallback behavior when transplanting this loop into the file; only replace the Gregorian anchor.

- [ ] **Step 4: Run recurrence consumers and record GREEN**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.utils.LunarEventUtilsTest --tests com.example.timeapk.notifications.ReminderDateCalculatorTest --tests com.example.timeapk.widget.WidgetContentResolverTest`

Expected: all selected tests pass, including existing leap-month cases.

- [ ] **Step 5: Commit the lunar fix**

```bash
git add app/src/main/java/com/example/timeapk/ui/utils/LunarEventUtils.kt app/src/test/java/com/example/timeapk/ui/utils/LunarEventUtilsTest.kt
git commit -m "fix: anchor lunar recurrence to lunar year"
```

### Task 2: Include Room and DataStore in backup and transfer

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`
- Modify: `app/src/main/res/xml/backup_rules.xml`
- Modify: `app/src/main/res/xml/data_extraction_rules.xml`

**Interfaces:**
- Produces: equivalent `database` and `file` inclusions in both Android backup formats.

- [ ] **Step 1: Add a failing backup-domain contract test**

```kotlin
@Test
fun backupRulesIncludeRoomAndBothDataStoresForCloudAndTransfer() {
    val legacy = existingFile(
        "src/main/res/xml/backup_rules.xml",
        "app/src/main/res/xml/backup_rules.xml"
    ).readText()
    val modern = existingFile(
        "src/main/res/xml/data_extraction_rules.xml",
        "app/src/main/res/xml/data_extraction_rules.xml"
    ).readText()
    listOf(legacy, modern).forEach { rules ->
        assertTrue(rules.contains("domain=\"database\" path=\"event_database\""))
        assertTrue(rules.contains("domain=\"database\" path=\"event_database-wal\""))
        assertTrue(rules.contains("domain=\"database\" path=\"event_database-shm\""))
        assertTrue(rules.contains("domain=\"file\" path=\"datastore/\""))
    }
    assertTrue(modern.contains("<cloud-backup>"))
    assertTrue(modern.contains("<device-transfer>"))
}
```

- [ ] **Step 2: Run the contract and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest.backupRulesIncludeRoomAndBothDataStoresForCloudAndTransfer`

Expected: FAIL because both files currently include only the `root` domain.

- [ ] **Step 3: Replace the legacy backup rules**

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="database" path="event_database" />
    <include domain="database" path="event_database-wal" />
    <include domain="database" path="event_database-shm" />
    <include domain="file" path="datastore/" />
</full-backup-content>
```

- [ ] **Step 4: Replace Android 12+ extraction rules**

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup disableIfNoEncryptionCapabilities="false">
        <include domain="database" path="event_database" />
        <include domain="database" path="event_database-wal" />
        <include domain="database" path="event_database-shm" />
        <include domain="file" path="datastore/" />
    </cloud-backup>
    <device-transfer>
        <include domain="database" path="event_database" />
        <include domain="database" path="event_database-wal" />
        <include domain="database" path="event_database-shm" />
        <include domain="file" path="datastore/" />
    </device-transfer>
</data-extraction-rules>
```

- [ ] **Step 5: Run the test and Android resource processing**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest.backupRulesIncludeRoomAndBothDataStoresForCloudAndTransfer processDirectDebugResources`

Expected: PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit backup coverage**

```bash
git add app/src/main/res/xml/backup_rules.xml app/src/main/res/xml/data_extraction_rules.xml app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt
git commit -m "fix: include app data in Android backup"
```

### Task 3: Enforce strict JSON validation and complete duplicate identity

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/data/EventJsonTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/data/EventJson.kt`
- Modify: `app/src/main/java/com/example/timeapk/data/BackupImportParser.kt`

**Interfaces:**
- Produces: `parseEventsFromJson(json: String): ParseResult` with strict required fields.
- Produces: `Event.importDuplicateKey()` using all normalized functional fields except IDs and `createdAt`.
- Produces: `filterExistingDuplicateEvents(events, existingEvents)` that removes database and in-file duplicates.

- [ ] **Step 1: Replace permissive-default tests with strict validation tests**

```kotlin
@Test
fun parseEventsFromJson_missingRequiredFields_countsInvalidRows() {
    val json = """[
        {"title":"No date","category":"other"},
        {"title":"No category","date":1700000000000},
        {"title":"   ","date":1700000000000,"category":"other"}
    ]"""

    val result = parseEventsFromJson(json)

    assertTrue(result.events.isEmpty())
    assertEquals(3, result.errorCount)
}

@Test
fun parseEventsFromJson_legacyMissingSideEffectFlags_defaultsBothFalse() {
    val result = parseEventsFromJson(
        """[{"title":"Legacy","date":1700000000000,"category":"other"}]"""
    )

    assertFalse(result.events.single().remindEnabled)
    assertFalse(result.events.single().syncToScheduleEnabled)
}

@Test
fun parseEventsFromJson_rejectsPre1900DateAndUnknownCategory() {
    val json = """[
        {"title":"Old","date":-2209075200001,"category":"other"},
        {"title":"Bad category","date":1700000000000,"category":"holiday"}
    ]"""
    assertEquals(2, parseEventsFromJson(json).errorCount)
}
```

- [ ] **Step 2: Add duplicate-identity and same-file tests**

```kotlin
@Test
fun filterExistingDuplicateEvents_skipsDatabaseAndSameFileDuplicates() {
    val base = Event(
        title = " Birthday ", date = 1700000000000, category = CATEGORY_BIRTHDAY,
        note = "family", repeatType = REPEAT_YEARLY, remindDaysBefore = 3,
        reminderTimeMinutesOfDay = 540, remindEnabled = true,
        syncToScheduleEnabled = false, createdAt = 1
    )
    val result = filterExistingDuplicateEvents(
        events = listOf(base.copy(createdAt = 2), base.copy(createdAt = 3), base.copy(note = "friends")),
        existingEvents = listOf(base)
    )

    assertEquals(listOf("friends"), result.importableEvents.map { it.note })
    assertEquals(2, result.existingDuplicateCount)
}

@Test
fun filterExistingDuplicateEvents_withoutDatabaseStillDropsSameFileDuplicates() {
    val event = Event(title = "Trip", date = 1700000000000, category = CATEGORY_OTHER)
    val result = filterExistingDuplicateEvents(listOf(event, event.copy(createdAt = 99)), emptyList())

    assertEquals(1, result.importableEvents.size)
    assertEquals(1, result.existingDuplicateCount)
}
```

- [ ] **Step 3: Run import tests and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.data.EventJsonTest`

Expected: strict-field/default tests and in-file duplicate tests fail.

- [ ] **Step 4: Implement required-field accessors and validation**

```kotlin
private const val MIN_EVENT_DATE_MILLIS = -2208988800000L
private val VALID_CATEGORIES = setOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER)

private fun JSONObject.requiredString(name: String): String =
    (get(name) as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw IllegalArgumentException("Expected non-empty string for $name")

private fun JSONObject.requiredLong(name: String): Long =
    (get(name) as? Number)?.toLong()
        ?: throw IllegalArgumentException("Expected number for $name")

// Inside the row loop:
val title = o.requiredString("title")
val date = o.requiredLong("date").also {
    require(it >= MIN_EVENT_DATE_MILLIS) { "Date before 1900" }
}
val category = o.requiredString("category").also {
    require(it in VALID_CATEGORIES) { "Unknown category" }
}
// Construct Event with title/date/category and:
syncToScheduleEnabled = o.optionalBoolean("syncToScheduleEnabled", false)
```

Retain strict type checks for every optional field; do not coerce strings to numbers or booleans.

- [ ] **Step 5: Implement the functional duplicate key and mutable seen set**

```kotlin
private data class ImportDuplicateKey(
    val title: String,
    val date: Long,
    val category: String,
    val note: String,
    val colorHex: String?,
    val repeatType: String,
    val remindDaysBefore: Int,
    val reminderTimeMinutesOfDay: Int,
    val remindEnabled: Boolean,
    val syncToScheduleEnabled: Boolean,
    val isLunar: Boolean
)

private fun Event.importDuplicateKey() = ImportDuplicateKey(
    title = title.trim(),
    date = date,
    category = category,
    note = note.trim(),
    colorHex = colorHex?.trim()?.uppercase(),
    repeatType = repeatType,
    remindDaysBefore = remindDaysBefore,
    reminderTimeMinutesOfDay = reminderTimeMinutesOfDay,
    remindEnabled = remindEnabled,
    syncToScheduleEnabled = syncToScheduleEnabled,
    isLunar = isLunar
)

fun filterExistingDuplicateEvents(
    events: List<Event>,
    existingEvents: List<Event>
): ExistingDuplicateFilterResult {
    val seen = existingEvents.mapTo(mutableSetOf()) { it.importDuplicateKey() }
    val importable = mutableListOf<Event>()
    var skipped = 0
    events.forEach { event ->
        if (seen.add(event.importDuplicateKey())) importable += event else skipped++
    }
    return ExistingDuplicateFilterResult(importable, skipped)
}
```

- [ ] **Step 6: Run import tests and record GREEN**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.data.EventJsonTest`

Expected: all import, legacy Realm, malformed JSON, and duplicate tests pass.

- [ ] **Step 7: Commit import safety**

```bash
git add app/src/main/java/com/example/timeapk/data/EventJson.kt app/src/main/java/com/example/timeapk/data/BackupImportParser.kt app/src/test/java/com/example/timeapk/data/EventJsonTest.kt
git commit -m "fix: validate and deduplicate imported events"
```

### Task 4: Return explicit CalendarContract cleanup outcomes

**Files:**
- Create: `app/src/main/java/com/example/timeapk/notifications/CalendarCleanupResult.kt`
- Create: `app/src/test/java/com/example/timeapk/notifications/CalendarCleanupResultTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/notifications/ScheduleSyncManager.kt`
- Modify: `app/src/test/java/com/example/timeapk/notifications/ScheduleSyncManagerTest.kt`

**Interfaces:**
- Produces: `sealed interface CalendarCleanupResult`.
- Produces: `ScheduleSyncManager.removeManagedCalendarEntries(context, eventId, calendarEventId): CalendarCleanupResult` for reminder-series and milestone entries.

- [ ] **Step 1: Add failing result-policy tests**

```kotlin
@Test
fun cleanupFailureMessage_keepsPermissionAndProviderFailuresDistinct() {
    assertEquals("Calendar permission required", CalendarCleanupResult.PermissionRequired.message)
    assertEquals(
        "provider down",
        CalendarCleanupResult.ProviderFailure("provider down").message
    )
}

@Test
fun cleanupSuccess_isOnlyRemovedOrNotPresent() {
    assertTrue(CalendarCleanupResult.RemovedOrNotPresent.isSuccess)
    assertFalse(CalendarCleanupResult.PermissionRequired.isSuccess)
    assertFalse(CalendarCleanupResult.ProviderFailure("x").isSuccess)
}
```

- [ ] **Step 2: Run the new test and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.notifications.CalendarCleanupResultTest`

Expected: compilation fails because `CalendarCleanupResult` does not exist.

- [ ] **Step 3: Add the result type**

```kotlin
package com.example.timeapk.notifications

sealed interface CalendarCleanupResult {
    val isSuccess: Boolean
    val message: String?

    data object RemovedOrNotPresent : CalendarCleanupResult {
        override val isSuccess = true
        override val message: String? = null
    }

    data object PermissionRequired : CalendarCleanupResult {
        override val isSuccess = false
        override val message = "Calendar permission required"
    }

    data class ProviderFailure(override val message: String) : CalendarCleanupResult {
        override val isSuccess = false
    }
}
```

- [ ] **Step 4: Implement one combined, non-swallowing cleanup entry point**

```kotlin
fun removeManagedCalendarEntries(
    context: Context,
    eventId: Int,
    calendarEventId: Long?
): CalendarCleanupResult {
    if (!hasCalendarReadAccess(context) || !hasCalendarWriteAccess(context)) {
        return CalendarCleanupResult.PermissionRequired
    }
    return try {
        val ids = linkedSetOf<Long>()
        calendarEventId?.let(ids::add)
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.DESCRIPTION} LIKE ? OR ${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf(
                "$REMINDER_MARKER_PREFIX:$eventId%",
                "$MILESTONE_MARKER_PREFIX:$eventId%"
            ),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            while (cursor.moveToNext()) ids += cursor.getLong(idIndex)
        }
        ids.forEach { id ->
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
            context.contentResolver.delete(uri, null, null)
        }
        CalendarCleanupResult.RemovedOrNotPresent
    } catch (_: SecurityException) {
        CalendarCleanupResult.PermissionRequired
    } catch (t: Exception) {
        CalendarCleanupResult.ProviderFailure(
            (t.message ?: "Calendar provider cleanup failed").take(180)
        )
    }
}
```

Keep legacy call sites compiling by making `removeScheduleReminder` and `removeScheduleReminderByEventId` delegate to explicit result-returning helpers; no helper may catch and discard `SecurityException` or provider exceptions.

- [ ] **Step 5: Add a source contract preventing swallowed cleanup errors**

```kotlin
@Test
fun scheduleCleanup_returnsExplicitOutcomeAndDoesNotSwallowExceptions() {
    val source = mainSource("notifications/ScheduleSyncManager.kt").readText()
    val cleanup = source.substringAfter("fun removeManagedCalendarEntries(")
        .substringBefore("private fun buildExpectedReminderEntries")
    assertTrue(cleanup.contains("CalendarCleanupResult.PermissionRequired"))
    assertTrue(cleanup.contains("CalendarCleanupResult.ProviderFailure"))
    assertFalse(cleanup.contains("catch (_: SecurityException) {\n        }"))
}
```

- [ ] **Step 6: Run result and schedule tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.notifications.CalendarCleanupResultTest --tests com.example.timeapk.notifications.ScheduleSyncManagerTest`

Expected: PASS.

- [ ] **Step 7: Commit explicit cleanup outcomes**

```bash
git add app/src/main/java/com/example/timeapk/notifications/CalendarCleanupResult.kt app/src/main/java/com/example/timeapk/notifications/ScheduleSyncManager.kt app/src/test/java/com/example/timeapk/notifications/CalendarCleanupResultTest.kt app/src/test/java/com/example/timeapk/notifications/ScheduleSyncManagerTest.kt
git commit -m "fix: expose calendar cleanup outcomes"
```

### Task 5: Preserve recoverability when sync disable or deletion cleanup fails

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/ui/event/EventEntryValidationTest.kt`
- Create: `app/src/test/java/com/example/timeapk/ui/home/EventDeletionPolicyTest.kt`
- Create: `app/src/main/java/com/example/timeapk/ui/home/EventDeletionPolicy.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/event/EventEntryViewModel.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/example/timeapk/TimeApp.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Interfaces:**
- Produces: `eventAfterCleanupAttempt(event, result, nowMillis): Event`.
- Produces: `calendarCleanupRequired(event: Event): Boolean` so events that were never synchronized are not blocked by missing calendar permission.
- Produces: `sealed interface DeleteEventResult { data object Deleted; data class Blocked(val message: String) }`.
- Produces: `HomeViewModel.suspend fun deleteEvent(event: Event): DeleteEventResult`.

- [ ] **Step 1: Add failing pure policy tests**

```kotlin
@Test
fun failedCleanup_keepsCalendarIdsAndRecordsRetryableError() {
    val event = Event(
        id = 7, title = "Trip", date = 1, category = CATEGORY_OTHER,
        syncToScheduleEnabled = false, scheduleEventId = 88,
        targetCalendarId = 9, lastScheduleSyncError = null
    )
    val actual = eventAfterCleanupAttempt(
        event,
        CalendarCleanupResult.PermissionRequired,
        nowMillis = 123
    )
    assertEquals(88L, actual.scheduleEventId)
    assertEquals(9L, actual.targetCalendarId)
    assertEquals("Calendar permission required", actual.lastScheduleSyncError)
    assertEquals(123L, actual.lastScheduleSyncAt)
}

@Test
fun successfulCleanup_clearsCalendarIdsAndError() {
    val event = testEvent(
        syncToScheduleEnabled = true,
        scheduleEventId = 88,
        targetCalendarId = 9
    )
    val actual = eventAfterCleanupAttempt(
        event,
        CalendarCleanupResult.RemovedOrNotPresent,
        nowMillis = 456
    )
    assertNull(actual.scheduleEventId)
    assertNull(actual.targetCalendarId)
    assertNull(actual.lastScheduleSyncError)
}

@Test
fun cleanupIsRequiredOnlyWhenTheEventCouldOwnCalendarData() {
    assertFalse(calendarCleanupRequired(testEvent(syncToScheduleEnabled = false)))
    assertTrue(calendarCleanupRequired(testEvent(syncToScheduleEnabled = true)))
    assertTrue(calendarCleanupRequired(testEvent(syncToScheduleEnabled = false, scheduleEventId = 88)))
}

private fun testEvent(
    syncToScheduleEnabled: Boolean,
    scheduleEventId: Long? = null,
    targetCalendarId: Long? = null
) = Event(
    title = "Trip",
    date = 1,
    category = CATEGORY_OTHER,
    syncToScheduleEnabled = syncToScheduleEnabled,
    scheduleEventId = scheduleEventId,
    targetCalendarId = targetCalendarId
)
```

- [ ] **Step 2: Run the policy tests and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.home.EventDeletionPolicyTest --tests com.example.timeapk.ui.event.EventEntryValidationTest`

Expected: compilation fails because the policy and delete result are missing.

- [ ] **Step 3: Add the cleanup state policy**

```kotlin
internal fun eventAfterCleanupAttempt(
    event: Event,
    result: CalendarCleanupResult,
    nowMillis: Long
): Event = when (result) {
    CalendarCleanupResult.RemovedOrNotPresent -> event.copy(
        scheduleEventId = null,
        targetCalendarId = null,
        lastScheduleSyncAt = nowMillis,
        lastScheduleSyncError = null
    )
    else -> event.copy(
        lastScheduleSyncAt = nowMillis,
        lastScheduleSyncError = result.message
    )
}

internal fun calendarCleanupRequired(event: Event): Boolean =
    event.syncToScheduleEnabled || event.scheduleEventId != null || event.targetCalendarId != null

sealed interface DeleteEventResult {
    data object Deleted : DeleteEventResult
    data class Blocked(val message: String) : DeleteEventResult
}
```

- [ ] **Step 4: Use the result when saving with schedule sync disabled**

```kotlin
val cleanup = if (calendarCleanupRequired(persistedEvent)) {
    ScheduleSyncManager.removeManagedCalendarEntries(
        context = application,
        eventId = persistedEvent.id,
        calendarEventId = persistedEvent.scheduleEventId
    )
} else {
    CalendarCleanupResult.RemovedOrNotPresent
}
updatedEvent = eventAfterCleanupAttempt(
    event = persistedEvent,
    result = cleanup,
    nowMillis = System.currentTimeMillis()
)
scheduleSyncError = cleanup.message
```

Do not clear `scheduleEventId` or `targetCalendarId` anywhere else in the disabled branch.

- [ ] **Step 5: Make deletion await cleanup before mutating Room**

```kotlin
suspend fun deleteEvent(event: Event): DeleteEventResult {
    val cleanup = if (calendarCleanupRequired(event)) {
        ScheduleSyncManager.removeManagedCalendarEntries(
            context = application,
            eventId = event.id,
            calendarEventId = event.scheduleEventId
        )
    } else {
        CalendarCleanupResult.RemovedOrNotPresent
    }
    if (!cleanup.isSuccess) {
        repository.updateEvent(event.copy(lastScheduleSyncError = cleanup.message))
        return DeleteEventResult.Blocked(cleanup.message ?: "Calendar cleanup failed")
    }
    cancelReminder(application, event.id)
    cancelMilestoneReminders(application, event.id)
    repository.deleteEvent(event)
    WidgetUpdater.refreshCountdownWidgets(application)
    return DeleteEventResult.Deleted
}
```

- [ ] **Step 6: Await deletion in the detail flow and keep the screen open on failure**

```kotlin
// DetailScreen signature
onDeleteClick: suspend () -> Boolean

// Confirm action
scope.launch {
    if (onDeleteClick()) {
        showDeleteConfirm = false
        onNavigateBack()
    }
}

// TimeApp callback
onDeleteClick = {
    when (val result = homeViewModel.deleteEvent(deletedEvent)) {
        DeleteEventResult.Deleted -> {
            scope.launch { showUndoSnackbar(deletedEvent) }
            true
        }
        is DeleteEventResult.Blocked -> {
            homeSnackbarHostState.showSnackbar(calendarCleanupBlockedMessage)
            false
        }
    }
}
```

Add localized cleanup guidance:

```xml
<!-- values/strings.xml and values-zh/strings.xml -->
<string name="calendar_cleanup_blocked">无法清理系统日历事件。请恢复日历权限后重试。</string>
<!-- values-en/strings.xml -->
<string name="calendar_cleanup_blocked">Could not clean up the system calendar event. Restore calendar permission and try again.</string>
```

- [ ] **Step 7: Run ViewModel/policy/source tests and record GREEN**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.event.EventEntryValidationTest --tests com.example.timeapk.ui.home.EventDeletionPolicyTest --tests com.example.timeapk.notifications.ScheduleSyncManagerTest`

Expected: PASS.

- [ ] **Step 8: Compile both channels**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew compileDirectDebugKotlin compilePlayDebugKotlin`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit recoverable calendar cleanup**

```bash
git add app/src/main/java/com/example/timeapk/notifications app/src/main/java/com/example/timeapk/ui/event/EventEntryViewModel.kt app/src/main/java/com/example/timeapk/ui/home app/src/main/java/com/example/timeapk/TimeApp.kt app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt app/src/main/res/values app/src/main/res/values-zh app/src/main/res/values-en app/src/test/java/com/example/timeapk
git commit -m "fix: block destructive calendar cleanup failures"
```

### Task 6: Verify the data-correctness subsystem

**Files:**
- Modify: `docs/RELEASE_CHECKLIST.md`

- [ ] **Step 1: Run all Direct and Play JVM tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest testPlayDebugUnitTest`

Expected: both tasks pass with no failed tests.

- [ ] **Step 2: Run an emulator permission-revocation smoke**

Run after installing Direct debug:

```bash
adb shell pm revoke com.example.timeapk android.permission.READ_CALENDAR
adb shell pm revoke com.example.timeapk android.permission.WRITE_CALENDAR
```

Expected: disabling schedule sync retains provider IDs and shows a warning; deleting the event is blocked until permission is restored; after restoring permission, cleanup and deletion succeed.

- [ ] **Step 3: Run an available backup/restore smoke**

Run: `adb shell bmgr backupnow com.example.timeapk`

Expected: transport output includes the package. After clearing/reinstalling and restoring on a test device with a working backup transport, Room events, home sort preferences, and per-widget configuration reappear.

- [ ] **Step 4: Record evidence without claiming unavailable transport coverage**

```markdown
- [x] Lunar recurrence unit regressions: command + report path
- [x] Import validation and duplicate regressions: command + report path
- [x] Calendar permission-revocation smoke: device/API + result
- [ ] Backup/restore smoke: leave unchecked with transport limitation if the device has no backup transport
```

- [ ] **Step 5: Commit verification evidence**

```bash
git add docs/RELEASE_CHECKLIST.md
git commit -m "docs: record data recovery verification"
```
