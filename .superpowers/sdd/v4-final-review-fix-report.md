# Glimmer 4.0 final whole-branch review fix report

## Outcome

- Base: `a4d9917`
- Functional fix commit: `f7866fcd1c1c5b7772ec6aaab6b46850308ceb56`
- Scope: all eight findings in `v4-final-review-fix-brief.md`
- Result: all focused tests, both flavor JVM suites, and both AndroidTest Kotlin compilation tasks pass.

## Root causes and design choices

1. Reminder discovery used a prefix `LIKE` query and trusted every returned row. Event `1` could therefore consume rows owned by events `10` and `100`. Discovery now treats the query as candidate retrieval only and validates each row with the existing exact managed-description marker before parsing or mutation.

2. A missing calendar-read permission, null provider cursor, or provider exception was collapsed into an empty successful result. That allowed the sync to insert duplicates and discard known ownership. Discovery now has an explicit success/failure result. Permission denial, null cursors, `SecurityException`, and provider exceptions abort before insert/update/delete and return an error while preserving the event's known schedule ownership.

3. Regular schedule paths had no process-wide per-event serialization boundary, while workers persisted stale whole `Event` snapshots after provider work. A shared keyed `ReentrantLock` now serializes provider reminder mutations for each event. Schedule persistence uses a DAO compare-and-set update limited to schedule-state columns, guarded by the expected schedule inputs and ownership values. Concurrent edits cannot be overwritten; workers retry when the compare-and-set loses. Reschedule-all performs milestone work without an intermediate stale persistence and writes the combined state once.

4. The checklist used the forbidden literal platform label. It now describes the target as an API 37 emulator system image; the release contract remains unchanged.

5. Home sort/filter rows used 42dp plain click targets with no mutually-exclusive semantics. Rows are now 48dp, live in a `selectableGroup`, and use `selectable(..., role = Role.RadioButton)` so Compose exposes selected state to TalkBack.

6. PR CI compiled only Direct AndroidTest sources. The workflow and release contract now require both Direct and Play AndroidTest Kotlin compilation tasks.

7. The exported AppWidget provider accepted app-internal clock/date-boundary actions. Custom actions now terminate at a manifest-declared non-exported `WidgetRefreshReceiver`; alarms and internal broadcasts explicitly target it. The exported provider retains system AppWidget callbacks and the system `DATE_CHANGED` path.

8. Restored/corrupt pin JSON could contain duplicate IDs, and downstream mappings could render duplicate cards or rows. Parsing and encoding now normalize with first-occurrence `distinct()`, with defensive normalization retained in both home and widget mapping paths.

No version, Room entity, Room database, migration, or schema file changed. Imported event fields remain intact because the new persistence operation updates only schedule-sync columns.

## Focused RED -> GREEN evidence

All Gradle commands below used Android Studio's JBR:

```text
JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew <tasks>
```

Two initial infrastructure attempts were not behavioral RED evidence: the system JDK was absent, then sandbox access to the shared Gradle home was denied. The same tests were rerun with the JBR and approved Gradle access.

### Findings 1 and 2: exact discovery and explicit failure

Command:

```text
./gradlew testDirectDebugUnitTest --tests com.example.timeapk.notifications.ReminderDiscoveryPolicyTest
```

- RED: the new contract test did not compile because explicit discovery candidates/results and the aborting discovery policy did not exist.
- GREEN: 3/3 tests pass, covering exact event-ID filtering, null/security/provider failure injection with no mutations and preserved ownership, and missing read permission with no provider query.

### Finding 3: serialization and atomic schedule persistence

Command:

```text
./gradlew testDirectDebugUnitTest --tests com.example.timeapk.notifications.ScheduleSyncConcurrencyTest
```

- RED: the new test did not compile because the shared per-event coordinator and schedule-only compare-and-set repository APIs did not exist.
- GREEN: 5/5 tests pass, proving same-event provider transactions do not overlap, concurrent edits reject stale schedule state without being overwritten, unchanged inputs accept schedule-only state, background workers use retry-on-CAS-miss behavior, and regular provider entrypoints use the shared coordinator.

### Finding 4: checklist wording

Command:

```text
./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest.releaseChecklistDistinguishesVerifiedCandidateArtifactsFromFinalPublication
```

- RED: the existing readiness contract failed at `ReleaseReadinessTest.kt:239` on the literal `Android 17` checklist text.
- GREEN: the focused readiness test passes after the API-level/device rewrite.

### Finding 5: accessible sort/filter choices

Commands:

```text
./gradlew testDirectDebugUnitTest --tests com.example.timeapk.ui.home.HomeActionOptionAccessibilityTest
./gradlew compileDirectDebugAndroidTestKotlin compilePlayDebugAndroidTestKotlin
```

- RED: the source contract failed on the 42dp/plain-clickable implementation; the connected-style semantics test initially could not compile against private option/grid helpers.
- GREEN: the source contract passes, and `HomeActionOptionSemanticsTest` compiles for both flavors while asserting radio role, selected/unselected state, and click action through the Compose semantics tree.

### Finding 6: both AndroidTest flavor compiles in CI

Command:

```text
./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest.repositoryProvidesExecutableWrapperAndSecretFreePullRequestCi
```

- RED: the release contract failed because the workflow lacked `compilePlayDebugAndroidTestKotlin`.
- GREEN: the contract passes and the workflow invokes both flavor compile tasks.

### Finding 7: private custom widget action boundary

Command:

```text
./gradlew testDirectDebugUnitTest --tests com.example.timeapk.widget.WidgetOrderingRefreshArchitectureTest
```

- RED: the new architecture contract failed because `WidgetRefreshReceiver.kt` did not exist and custom actions were handled by the exported provider.
- GREEN: all architecture tests pass; manifest/source assertions verify the custom receiver is non-exported, internal intents are explicit, async lifetime is owned, and system provider behavior remains wired.

### Finding 8: pin normalization

Command:

```text
./gradlew testDirectDebugUnitTest --tests com.example.timeapk.data.PinnedEventIdsNormalizationTest --tests com.example.timeapk.ui.home.HomeSortBehaviorTest --tests com.example.timeapk.widget.WidgetContentResolverTest
```

- RED: repository tests did not compile because normalization helpers were absent; home and widget regressions produced duplicate output for duplicate pin IDs.
- GREEN: repository normalization tests pass, and duplicate pin IDs render each event exactly once in both home cards and widget rows while preserving pins-first ordering.

## Final verification

Fresh verification against the complete functional tree:

```text
JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew testDirectDebugUnitTest testPlayDebugUnitTest compileDirectDebugAndroidTestKotlin compilePlayDebugAndroidTestKotlin
```

Result: `BUILD SUCCESSFUL`.

- Direct JVM: 506 tests, 0 failures, 0 errors, 0 skipped.
- Play JVM: 506 tests, 0 failures, 0 errors, 0 skipped.
- Direct AndroidTest Kotlin: compiled successfully.
- Play AndroidTest Kotlin: compiled successfully.
- `git diff --check`: clean.

Release configuration remains `versionCode=23`, Direct `4.0`, Play `4.0-play`. External publication and physical-phone gates were not changed. Per controller instruction, final lint, connected-device execution, and packaging were not run in this fix wave.

## Concerns

- The connected semantics test was compiled for both flavors but not executed on an emulator, as required by the brief's no-final-connected-matrix constraint.
- Gradle continues to emit the pre-existing `android.disallowKotlinSourceSets=false` warning; this fix wave did not alter that build setting.
