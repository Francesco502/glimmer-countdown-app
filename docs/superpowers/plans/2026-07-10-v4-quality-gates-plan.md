# Glimmer 4.0 Quality Gates and Maintainability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore executable migration/UI evidence, make the documented Gradle entry point work in Git and CI, and reduce all audited Lint variants to zero warnings.

**Architecture:** Restore the historical Room 7 schema as test data without changing the production database version, upgrade only AndroidX test artifacts needed for API 36/preview compatibility, and run secret-free unit/Lint/debug/connected gates in GitHub Actions. Keep release signing in the separately authorized release pipeline.

**Tech Stack:** Room 2.8.4 migration testing, AndroidX Test ext:junit 1.3.0 and Espresso 3.7.0, Gradle Wrapper, GitHub Actions, Android Emulator API 36, Android Lint.

## Global Constraints

- Keep `versionName=4.0` and `versionCode=23` while this release remains unpublished.
- Preserve Room database version 10 and production migrations 1→10.
- Use stable AndroidX Test `ext:junit:1.3.0` and Espresso `3.7.0`.
- Pull-request CI contains no production signing material and does not package Release artifacts.
- Run Direct/Play unit tests, Direct debug/Direct release/Play release Lint, debug assembly, Android-test compilation, and API 36 connected tests.
- Lint acceptance is zero errors and zero warnings for all three audited variants.
- Preserve existing 4.0 candidate changes and commit quality-gate work independently.

---

## File Map

- `app/schemas/com.example.timeapk.data.AppDatabase/7.json`: reconstructed historical Room schema.
- `app/src/androidTest/java/com/example/timeapk/data/AppDatabaseMigrationTest.kt`: 7→8, 8→9, 9→10, and 7→10 preservation tests.
- `app/src/main/java/com/example/timeapk/data/AppDatabase.kt`: expose existing migration objects to tests only.
- `app/build.gradle.kts`: stable AndroidX Test/Espresso dependencies.
- `gradlew`: executable Git mode.
- `.github/workflows/android-quality.yml`: repeatable secret-free quality workflow.
- `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`: CI and wrapper source contracts.
- `app/src/main/java/com/example/timeapk/ui/settings/WidgetSettingsContent.kt`: plural resource call.
- `app/src/main/res/values/strings.xml`, `values-zh/strings.xml`, `values-en/strings.xml`: widget plurals and removal of five unused strings.
- `docs/RELEASE_CHECKLIST.md`: authoritative run/device/report evidence.

### Task 1: Restore the Room 7 schema fixture

**Files:**
- Create: `app/schemas/com.example.timeapk.data.AppDatabase/7.json`
- Modify: `app/src/androidTest/java/com/example/timeapk/data/AppDatabaseMigrationTest.kt`

**Interfaces:**
- Produces: Room schema version 7 with the `tags TEXT NOT NULL` column expected by migration 7→8.

- [ ] **Step 1: Keep the current 7→8 migration test as the RED test**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDirectDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.timeapk.data.AppDatabaseMigrationTest#migrate7To8_dropsTagsColumnAndPreservesRows'`

Expected: FAIL with missing schema file `7.json`.

- [ ] **Step 2: Add the reconstructed schema header and entity SQL**

```json
{
  "formatVersion": 1,
  "database": {
    "version": 7,
    "identityHash": "98746a913eb0649dd8c272b71a51c93d",
    "entities": [
      {
        "tableName": "events",
        "createSql": "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `date` INTEGER NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `colorHex` TEXT, `repeatType` TEXT NOT NULL, `remindDaysBefore` INTEGER NOT NULL, `reminderTimeMinutesOfDay` INTEGER NOT NULL, `remindEnabled` INTEGER NOT NULL, `syncToScheduleEnabled` INTEGER NOT NULL, `scheduleEventId` INTEGER, `targetCalendarId` INTEGER, `lastScheduleSyncAt` INTEGER, `lastScheduleSyncError` TEXT, `createdAt` INTEGER NOT NULL, `isLunar` INTEGER NOT NULL, `tags` TEXT NOT NULL)",
        "fields": [
          { "fieldPath": "id", "columnName": "id", "affinity": "INTEGER", "notNull": true },
          { "fieldPath": "title", "columnName": "title", "affinity": "TEXT", "notNull": true },
          { "fieldPath": "date", "columnName": "date", "affinity": "INTEGER", "notNull": true },
          { "fieldPath": "category", "columnName": "category", "affinity": "TEXT", "notNull": true },
          { "fieldPath": "note", "columnName": "note", "affinity": "TEXT", "notNull": true },
          { "fieldPath": "colorHex", "columnName": "colorHex", "affinity": "TEXT" },
          { "fieldPath": "repeatType", "columnName": "repeatType", "affinity": "TEXT", "notNull": true },
          { "fieldPath": "remindDaysBefore", "columnName": "remindDaysBefore", "affinity": "INTEGER", "notNull": true },
          { "fieldPath": "reminderTimeMinutesOfDay", "columnName": "reminderTimeMinutesOfDay", "affinity": "INTEGER", "notNull": true },
          { "fieldPath": "remindEnabled", "columnName": "remindEnabled", "affinity": "INTEGER", "notNull": true },
          { "fieldPath": "syncToScheduleEnabled", "columnName": "syncToScheduleEnabled", "affinity": "INTEGER", "notNull": true },
          { "fieldPath": "scheduleEventId", "columnName": "scheduleEventId", "affinity": "INTEGER" },
          { "fieldPath": "targetCalendarId", "columnName": "targetCalendarId", "affinity": "INTEGER" },
          { "fieldPath": "lastScheduleSyncAt", "columnName": "lastScheduleSyncAt", "affinity": "INTEGER" },
          { "fieldPath": "lastScheduleSyncError", "columnName": "lastScheduleSyncError", "affinity": "TEXT" },
          { "fieldPath": "createdAt", "columnName": "createdAt", "affinity": "INTEGER", "notNull": true },
          { "fieldPath": "isLunar", "columnName": "isLunar", "affinity": "INTEGER", "notNull": true },
          { "fieldPath": "tags", "columnName": "tags", "affinity": "TEXT", "notNull": true }
        ],
        "primaryKey": { "autoGenerate": true, "columnNames": ["id"] }
      }
    ],
    "setupQueries": [
      "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
      "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '98746a913eb0649dd8c272b71a51c93d')"
    ]
  }
}
```

The fields exactly reflect migrations 1→7: version 5 added `tags`, version 6→7 added calendar target/sync status, and version 7 has no birth-time fields.

- [ ] **Step 3: Remove redundant table creation from the 7→8 test**

```kotlin
helper.createDatabase(dbName, 7).apply {
    execSQL(
        """
        INSERT INTO events (
            id, title, date, category, note, colorHex, repeatType,
            remindDaysBefore, reminderTimeMinutesOfDay, remindEnabled,
            syncToScheduleEnabled, scheduleEventId, targetCalendarId,
            lastScheduleSyncAt, lastScheduleSyncError, createdAt, isLunar, tags
        ) VALUES (
            1, 'Birthday', 1704067200000, 'birthday', 'note', '#AF4E31', 'yearly',
            7, 480, 1, 1, NULL, NULL, NULL, NULL, 1704067200000, 0, 'family'
        )
        """.trimIndent()
    )
    close()
}
```

- [ ] **Step 4: Rerun 7→8 and record GREEN**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDirectDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.timeapk.data.AppDatabaseMigrationTest#migrate7To8_dropsTagsColumnAndPreservesRows'`

Expected: PASS; `tags` is removed and representative row fields remain.

- [ ] **Step 5: Commit the historical schema fixture**

```bash
git add app/schemas/com.example.timeapk.data.AppDatabase/7.json app/src/androidTest/java/com/example/timeapk/data/AppDatabaseMigrationTest.kt
git commit -m "test: restore Room 7 migration fixture"
```

### Task 2: Cover every supported migration hop and full 7→10 chain

**Files:**
- Modify: `app/src/main/java/com/example/timeapk/data/AppDatabase.kt`
- Modify: `app/src/androidTest/java/com/example/timeapk/data/AppDatabaseMigrationTest.kt`

**Interfaces:**
- Consumes: existing `MIGRATION_7_8_FOR_TEST`, `MIGRATION_8_9_FOR_TEST`, `MIGRATION_9_10_FOR_TEST`.
- Produces: preservation evidence for 8→9 and 7→10.

- [ ] **Step 1: Add an 8→9 column/default preservation test**

```kotlin
@Test
fun migrate8To9_addsNullableBirthTimeColumnsAndPreservesRows() {
    val dbName = "migration-test-v8-to-v9"
    helper.createDatabase(dbName, 8).apply {
        execSQL(
            "INSERT INTO events (id,title,date,category,note,repeatType,remindDaysBefore,reminderTimeMinutesOfDay,remindEnabled,syncToScheduleEnabled,createdAt,isLunar) VALUES (1,'Trip',1704067200000,'other','','none',0,480,0,0,1704067200000,0)"
        )
        close()
    }
    val db = helper.runMigrationsAndValidate(dbName, 9, true, AppDatabase.MIGRATION_8_9_FOR_TEST)
    val columns = columnNamesOf(db, "events")
    assertTrue(columns.contains("birthHour"))
    assertTrue(columns.contains("birthMinute"))
    db.query("SELECT title,birthHour,birthMinute FROM events WHERE id=1").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals("Trip", cursor.getString(0))
        assertTrue(cursor.isNull(1))
        assertTrue(cursor.isNull(2))
    }
    db.close()
}
```

- [ ] **Step 2: Add the full 7→10 preservation test**

```kotlin
@Test
fun migrate7To10_preservesFunctionalEventData() {
    val dbName = "migration-test-v7-to-v10"
    helper.createDatabase(dbName, 7).apply {
        execSQL(
            "INSERT INTO events (id,title,date,category,note,colorHex,repeatType,remindDaysBefore,reminderTimeMinutesOfDay,remindEnabled,syncToScheduleEnabled,scheduleEventId,targetCalendarId,lastScheduleSyncAt,lastScheduleSyncError,createdAt,isLunar,tags) VALUES (7,'Lunar birthday',1704067200000,'birthday','family','#AF4E31','yearly',3,540,1,1,88,9,1704067200000,NULL,1600000000000,1,'legacy')"
        )
        close()
    }
    val db = helper.runMigrationsAndValidate(
        dbName, 10, true,
        AppDatabase.MIGRATION_7_8_FOR_TEST,
        AppDatabase.MIGRATION_8_9_FOR_TEST,
        AppDatabase.MIGRATION_9_10_FOR_TEST
    )
    db.query("SELECT title,note,remindDaysBefore,reminderTimeMinutesOfDay,scheduleEventId,targetCalendarId,isLunar FROM events WHERE id=7").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals("Lunar birthday", cursor.getString(0))
        assertEquals("family", cursor.getString(1))
        assertEquals(3, cursor.getInt(2))
        assertEquals(540, cursor.getInt(3))
        assertEquals(88L, cursor.getLong(4))
        assertEquals(9L, cursor.getLong(5))
        assertEquals(1, cursor.getInt(6))
    }
    db.close()
}
```

- [ ] **Step 3: Run the migration class and record RED/GREEN**

Run before any production migration change: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDirectDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.timeapk.data.AppDatabaseMigrationTest`

Expected: tests compile and pass with the existing three production migrations. If a test fails, fix the migration rather than weakening validation or expected data.

- [ ] **Step 4: Commit migration coverage**

```bash
git add app/src/main/java/com/example/timeapk/data/AppDatabase.kt app/src/androidTest/java/com/example/timeapk/data/AppDatabaseMigrationTest.kt
git commit -m "test: cover supported Room migration chain"
```

### Task 3: Upgrade AndroidX Test for API 36/preview compatibility

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: `androidx.test.ext:junit:1.3.0` and `androidx.test.espresso:espresso-core:3.7.0`.

- [ ] **Step 1: Add a dependency source contract**

```kotlin
@Test
fun androidTestDependenciesSupportApi36() {
    val build = appBuildGradleFile().readText()
    assertTrue(build.contains("androidx.test.ext:junit:1.3.0"))
    assertTrue(build.contains("androidx.test.espresso:espresso-core:3.7.0"))
    assertFalse(build.contains("androidx.test.ext:junit:1.1.5"))
    assertFalse(build.contains("androidx.test.espresso:espresso-core:3.5.1"))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest.androidTestDependenciesSupportApi36`

Expected: FAIL against 1.1.5/3.5.1.

- [ ] **Step 3: Update only the failing Android test artifacts**

```kotlin
androidTestImplementation("androidx.test.ext:junit:1.3.0")
androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
```

Keep Room testing at 2.8.4 and the current Compose BOM until a separate dependency audit authorizes wider upgrades.

- [ ] **Step 4: Compile and run connected tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew compileDirectDebugAndroidTestKotlin connectedDirectDebugAndroidTest`

Expected: all instrumentation tests actually execute; no Espresso reflection failure on the target/API 36 device.

- [ ] **Step 5: Commit the test dependency repair**

```bash
git add app/build.gradle.kts app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt
git commit -m "test: update AndroidX test runtime"
```

### Task 4: Make the Gradle wrapper executable and add CI

**Files:**
- Modify mode: `gradlew` from `100644` to `100755`
- Create: `.github/workflows/android-quality.yml`
- Modify: `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`

**Interfaces:**
- Produces: secret-free `unit-lint-build` and API 36 `instrumentation` jobs.

- [ ] **Step 1: Add a failing workflow source contract**

```kotlin
@Test
fun githubQualityWorkflowRunsAllReleaseGatesWithoutSigningSecrets() {
    val workflow = existingFile(
        ".github/workflows/android-quality.yml",
        "../.github/workflows/android-quality.yml"
    )
    assertTrue(workflow.isFile)
    val yaml = workflow.readText()
    listOf(
        "testDirectDebugUnitTest", "testPlayDebugUnitTest",
        "lintDirectDebug", "lintDirectRelease", "lintPlayRelease",
        "assembleDirectDebug", "compileDirectDebugAndroidTestKotlin",
        "connectedDirectDebugAndroidTest", "api-level: 36"
    ).forEach { assertTrue("Missing CI gate: ${'$'}it", yaml.contains(it)) }
    assertFalse(yaml.contains("keystore.properties"))
    assertFalse(yaml.contains("assembleDirectRelease"))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest.githubQualityWorkflowRunsAllReleaseGatesWithoutSigningSecrets`

Expected: FAIL because `.github/workflows/android-quality.yml` does not exist.

- [ ] **Step 3: Add the quality workflow**

```yaml
name: Android quality

on:
  pull_request:
  push:
    branches: [main]

permissions:
  contents: read

jobs:
  unit-lint-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v6
      - name: Unit tests, Lint, debug build, Android-test compile
        run: >-
          ./gradlew
          testDirectDebugUnitTest testPlayDebugUnitTest
          lintDirectDebug lintDirectRelease lintPlayRelease
          assembleDirectDebug compileDirectDebugAndroidTestKotlin

  instrumentation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v6
      - name: Enable KVM
        run: sudo chmod 666 /dev/kvm
      - uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 36
          target: google_apis
          arch: x86_64
          disable-animations: true
          script: ./gradlew connectedDirectDebugAndroidTest
```

- [ ] **Step 4: Change wrapper mode and verify it through Git**

Run: `chmod +x gradlew`

Run: `git add gradlew && git ls-files -s gradlew`

Expected: the index entry starts with `100755`.

- [ ] **Step 5: Run the non-device CI command locally**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest testPlayDebugUnitTest lintDirectDebug lintDirectRelease lintPlayRelease assembleDirectDebug compileDirectDebugAndroidTestKotlin`

Expected at this stage: `BUILD SUCCESSFUL` and no signing credential request. The existing Lint warnings remain RED evidence for the warning-cleanup task.

- [ ] **Step 6: Commit wrapper and CI**

```bash
git add gradlew .github/workflows/android-quality.yml app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt
git commit -m "ci: add Android 4.0 quality gates"
```

### Task 5: Resolve all six current Lint warnings

**Files:**
- Modify: `app/src/main/java/com/example/timeapk/ui/settings/WidgetSettingsContent.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Interfaces:**
- Produces: `R.plurals.widget_config_cell_count` and no unused splash/typography/widget strings.

- [ ] **Step 1: Preserve the current Lint report as RED evidence**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lintDirectDebug lintDirectRelease lintPlayRelease`

Expected before changes: each audited report contains six warnings: one `PluralsCandidate` and five `UnusedResources`.

- [ ] **Step 2: Replace cell-count strings with plurals in all locales**

```xml
<!-- values/strings.xml and values-zh/strings.xml -->
<plurals name="widget_config_cell_count">
    <item quantity="other">%1$d格</item>
</plurals>

<!-- values-en/strings.xml -->
<plurals name="widget_config_cell_count">
    <item quantity="one">%1$d cell</item>
    <item quantity="other">%1$d cells</item>
</plurals>
```

- [ ] **Step 3: Use quantity-aware rendering for both dimensions**

```kotlin
options = (1..5).map { count ->
    count to pluralStringResource(R.plurals.widget_config_cell_count, count, count)
}
```

Apply this exact mapping to width and height options and import `androidx.compose.ui.res.pluralStringResource`.

- [ ] **Step 4: Remove the five confirmed unused resources from all locale files**

Remove these names only:

```text
splash_tagline
splash_subtitle
settings_typography_scale_summary
widget_config_preview_title
widget_config_collapse
```

Before deletion, run `rg -n 'splash_tagline|splash_subtitle|settings_typography_scale_summary|widget_config_preview_title|widget_config_collapse' app/src/main/java` and require no code references.

- [ ] **Step 5: Rerun all three Lint variants and record GREEN**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lintDirectDebug lintDirectRelease lintPlayRelease`

Expected: `0 errors, 0 warnings` in each text report.

- [ ] **Step 6: Commit warning cleanup**

```bash
git add app/src/main/java/com/example/timeapk/ui/settings/WidgetSettingsContent.kt app/src/main/res/values app/src/main/res/values-zh app/src/main/res/values-en
git commit -m "fix: clear Android Lint warnings"
```

### Task 6: Run and record the complete quality gate

**Files:**
- Modify: `docs/RELEASE_CHECKLIST.md`

- [ ] **Step 1: Run the exact non-device CI command**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest testPlayDebugUnitTest lintDirectDebug lintDirectRelease lintPlayRelease assembleDirectDebug compileDirectDebugAndroidTestKotlin`

Expected: Direct/Play unit tests, three Lint variants, Direct debug assembly, and Android-test compilation all pass.

- [ ] **Step 2: Run API 36 connected tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDirectDebugAndroidTest`

Expected: every migration and Compose instrumentation test executes and passes; a compiled-but-not-run source set is not acceptance evidence.

- [ ] **Step 3: Inspect authoritative reports**

```bash
rg -n "0 errors, 0 warnings" app/build/reports/lint-results-directDebug.txt app/build/reports/lint-results-directRelease.txt app/build/reports/lint-results-playRelease.txt
git ls-files -s gradlew
```

Expected: three zero-warning lines and a `gradlew` index entry whose mode field is `100755`.

- [ ] **Step 4: Record workflow URL, test counts, device/API, and report paths**

```markdown
- [x] Direct/Play JVM tests: counts and report paths
- [x] Direct debug/Direct release/Play release Lint: 0 errors, 0 warnings
- [x] API 36 connected tests: executed count and report path
- [x] Room 7→8, 8→9, 9→10, 7→10
- [x] gradlew Git mode 100755
- [x] GitHub Android quality workflow: run URL and conclusion
```

- [ ] **Step 5: Commit quality evidence**

```bash
git add docs/RELEASE_CHECKLIST.md
git commit -m "docs: record 4.0 quality gate evidence"
```
