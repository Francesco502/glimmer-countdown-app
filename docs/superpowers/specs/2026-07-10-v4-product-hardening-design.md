# Glimmer 4.0 Product Hardening Design

**Status:** Approved for implementation on 2026-07-10.

**Goal:** Resolve every P1 and P2 item in the approved 4.0 audit so the candidate fails safely, preserves user data, remains correct across date boundaries, and can pass repeatable release gates.

**Scope source:** `/Users/francesco/.codex/attachments/25eb0c03-edc5-4f40-8103-cfc2b8e01cd4/pasted-text-1.txt`.

## Product Constraints

- Keep `versionName=4.0` and `versionCode=23` while this release remains unpublished.
- Keep the existing Song/Glimmer visual language; this work fixes behavior, contrast, reflow, and semantics rather than redesigning the product.
- Keep the production signing key and credentials outside Git. Local and CI release tasks must fail closed when the required signing material is absent.
- Preserve the Room database at version 10. The audit does not require a schema change.
- Direct and Play remain separate application IDs and update channels.
- Existing uncommitted 4.0 widget-sort changes are part of the candidate and must be preserved.
- Every behavior change follows red-green-refactor. Configuration fixes use an existing failing gate or a new source/architecture assertion before the configuration changes.

## Architecture

The work is split into five independently verifiable hardening units. Data correctness owns lunar recurrence, imports, backup, and calendar cleanup. Widget freshness owns date-boundary delivery and asynchronous RemoteViews refresh. Release safety owns signed artifacts, atomic publication, exact asset selection, and channel-specific update UI. Experience hardening owns responsive layout, contrast, semantics, and background work. Quality gates own Room schemas, Android test compatibility, CI, executable wrapper metadata, and warning-free Lint.

The units share only small pure policies. Import validation and duplicate identity live in data-layer functions. Reorder merging lives in a home model helper. Release asset selection lives in a pure parser used by the update checker and unit tests. Platform side effects return explicit outcomes instead of being inferred from exceptions that are currently swallowed.

## 1. Data Correctness and Recovery

### Lunar yearly recurrence

`getNextLunarOccurrence` and `getPreviousLunarOccurrence` will anchor their search to the lunar year obtained by converting the supplied pivot date, not to the pivot's Gregorian year. The next search starts at `pivotLunar.year`; the previous search also starts at `pivotLunar.year`. The original lunar month, leap-month sign, and lunar day remain unchanged.

Regression tests will cover a pivot before Lunar New Year, the occurrence on the pivot day, a pivot after the current lunar occurrence, and a leap-month event in a year where that leap month is absent. Home state, widget content, and reminder calculation continue to share this single recurrence function.

### Android backup and device transfer

Both backup rule formats will explicitly include:

- `database` for `event_database` and its Room side files;
- `file` path `datastore/` for `user_preferences.preferences_pb` and `widget_preferences.preferences_pb`.

Cache, no-backup files, generated share images, and external files remain excluded. A source-level release test will verify that the legacy and Android 12+ rule files include the same database and DataStore domains.

### Legacy JSON safety and validation

Missing `remindEnabled` and missing `syncToScheduleEnabled` both default to `false`. Imports must never create notification or calendar side effects that were not explicitly represented in the backup.

An imported event is accepted only when:

- `title` is a string whose trimmed value is non-empty;
- `date` is a numeric epoch value on or after 1900-01-01 UTC, matching event-entry validation;
- `category` is exactly `birthday`, `anniversary`, or `other`;
- optional fields have the expected JSON types.

Invalid rows increment the parse error count and are excluded. Missing title, date, or category is invalid rather than being replaced with a current-time or empty fallback.

Duplicate identity will include normalized title, date, category, note, color, repeat type, reminder days/time/enabled state, schedule-sync enabled state, and lunar state. The import scanner will update its seen-key set after each accepted row, so duplicates within the same file and duplicates against the database are both skipped. Database IDs, provider-specific calendar IDs, and `createdAt` metadata remain excluded: two functionally identical events are duplicates even if their export timestamps differ, while events with different notes, reminder behavior, or other functional fields remain distinct.

### Calendar cleanup outcomes

Calendar removal APIs will return a sealed cleanup result: removed/not present, permission required, or provider failure. They will not swallow permission and provider exceptions.

When schedule sync is turned off:

- successful cleanup clears `scheduleEventId`, `targetCalendarId`, and the previous sync error;
- failed cleanup keeps the provider IDs, records `lastScheduleSyncError`, and returns a warning to the UI so a later save can retry.

When deleting an event that has managed calendar entries, local deletion proceeds only after calendar cleanup succeeds or confirms that no managed entry exists. A permission/provider failure keeps the local event and surfaces an actionable message. This prevents an orphaned system-calendar reminder whose owning local ID has been discarded.

## 2. Home Ordering and Widget Freshness

### Full-data search and filtering

The arbitrary 100-upcoming/50-past truncation will be removed from the state pipeline. All Room events participate in filtering, searching, pinning, and sorting. Lazy UI containers remain responsible for rendering only visible items. Calendar and widget “follow home” behavior therefore consume the same complete ordered set.

### Reordering a visible subset

A pure `mergeVisibleOrderIntoGlobalOrder(globalIds, visibleIds, reorderedVisibleIds)` policy will replace the current behavior that persists only the visible subset. It will preserve every hidden event in its existing slot while substituting reordered visible IDs in visible slots. New IDs absent from the stored global order are appended once, using the current full custom order as the base.

Search, category filters, and timeline subsets may therefore be reordered without changing the relative order of hidden events.

### Midnight and configuration refresh

The widget provider will listen for `ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED`, and `ACTION_TIMEZONE_CHANGED` in addition to configuration changes. These actions will refresh all widget instances immediately; the 24-hour host period remains a fallback only.

All provider refresh entry points will use `goAsync()` plus an application-scoped IO coroutine. Widget configuration is loaded with suspend calls, RemoteViews are built off the broadcast main thread, and the pending broadcast result is always finished in `finally`. No widget provider callback uses `runBlocking`.

## 3. Release and Update Safety

### Signed artifacts and deterministic names

Any requested Release packaging task, including `assemble...Release`, `bundle...Release`, and the Direct rename/publish path, must fail during preflight when `keystore.properties` is missing, incomplete, or references a missing keystore. Static analysis and tests for Release variants, such as `lint...Release`, remain runnable without credentials so CI can audit release code without holding production secrets. Debug and unit-test tasks also remain usable without release credentials.

The Direct release rename task will consume the actual signed package output, require it to exist, and produce exactly `glimmer-countdown-4-0.apk`. It will fail if the expected source or renamed artifact is absent. The Play APK and AAB must also be signed with the configured upload key before release readiness can pass.

The publishing script will run `apksigner verify --print-certs`, compare the certificate SHA-256 fingerprint with a required release configuration value, and reject unsigned or unexpectedly signed APKs before any network mutation. Release-readiness verification will also validate the Play AAB with `jarsigner -verify` and confirm the expected signer certificate. Automated pipeline tests may use an ephemeral non-production keystore to prove the signing and naming path; only artifacts signed with the externally supplied production/upload key may satisfy the final release gate.

### Atomic GitHub publication

Release notes are mandatory; a missing 4.0 changelog section is fatal. Publication proceeds as follows:

1. refuse to mutate a published release that already uses the target tag, or create/update the target only when it is a draft;
2. remove every existing `.apk` asset from that draft;
3. upload exactly `glimmer-countdown-4-0.apk`;
4. fetch the draft again and verify there is exactly one APK with the expected name and non-zero size;
5. publish the release only after verification succeeds.

If upload or verification fails, the target remains a draft and every previously published release remains unaffected. Retrying may replace assets only on that draft; an already published target tag is a fatal conflict rather than an object the script silently converts or edits.

### Direct asset selection and Play behavior

The Direct update checker accepts only the exact asset name derived from the remote tag: `glimmer-countdown-{version-with-hyphens}.apk`. A missing or ambiguous expected asset makes the check fail; it never downloads the first arbitrary APK.

The Play flavor will not expose an in-app “check update” action backed by `StubUpdateChecker`. Its About screen will state that updates are managed by the application store. Direct keeps GitHub update checking and installation.

## 4. Experience, Accessibility, and Performance

### Calendar contrast and reflow

The selected dark-calendar cell will use a foreground/background pair that passes the existing 4.5:1 text contrast guardrail. Selected, today, and event-marker states remain visually distinct without relying on color alone.

The month calendar screen becomes vertically scrollable as a whole. The grid keeps a minimum 48dp cell target, while the selected-day event list measures to content inside the outer scrolling container. At 150% and 200% system font scales, the selected-day section and add-event action remain reachable without clipping.

### Touch targets and semantics

- Home mode tabs, search clear, color swatches, and other named controls receive a minimum 48dp interactive container.
- The decorative empty-state plus becomes a real add-event action with button role and label; the bottom add button remains the persistent action.
- Toggle semantics merge the visible setting label with checked state into one switch node.
- Radio rows expose one selectable node rather than a clickable row plus a second clickable radio node.
- The wheel picker uses a minimum 48dp row and exposes its current value, adjustable role, increment action, and decrement action. Individual visible values remain readable at enlarged font scales.

### Background work and locale startup

Share-card rendering runs on `Dispatchers.Default`; file creation and PNG compression run on `Dispatchers.IO`. The share action is disabled while work is in progress and surfaces failure without retaining an orphan temporary file.

Language selection is synchronously mirrored in private SharedPreferences for `attachBaseContext`, while DataStore remains the reactive settings source and is dual-written on changes. For existing installations without the mirror, the activity starts without blocking, asynchronously reads the DataStore value once, writes the mirror, and recreates only if the resolved locale differs. Subsequent cold starts contain no `runBlocking` path.

## 5. Quality Gates and Maintainability

### Room migration evidence

A valid exported Room 7 schema will be reconstructed from the version-7 table defined by migrations 1→7 and verified against the existing 7→8 test. Migration instrumentation will cover 7→8, 8→9, 9→10, and the full 7→10 chain while preserving representative rows.

AndroidX Test/Espresso dependencies will be updated to a stable version compatible with target/API 36 and current preview testing. Device tests must actually run; compiling the Android-test source set is not a release gate.

### CI and build entry points

`gradlew` will be executable in Git. A GitHub Actions quality workflow will run:

- Direct and Play unit tests;
- Direct debug, Direct release, and Play release Lint;
- debug assembly and Android-test compilation without signing secrets;
- API 36 connected instrumentation tests;
- source-level release/permission/backup/asset-contract checks.

Production Release signing and publication stay in an explicitly authorized release job/environment with secrets, not in pull-request CI.

### Lint and focused decomposition

All current Lint warnings will be resolved: the English widget cell count becomes a plural resource and unused strings are either wired to their intended UI or removed. Lint must report zero errors and zero warnings for the three audited variants.

Large screens will only be split where required by these fixes: import policy, release asset parsing, home reorder merging, calendar cleanup outcomes, and share orchestration become focused testable units. No unrelated visual or navigation rewrite is included.

## Error Handling and User Communication

- Invalid imports report recognized, importable, duplicate, and invalid counts before confirmation.
- Calendar cleanup failure keeps recoverable IDs and explains that calendar permission or provider access is required.
- Release failures happen before public mutation and identify the missing credential, signature, changelog, or asset invariant.
- Direct update failures distinguish network failure, invalid release metadata, and missing expected APK.
- Background sharing reports failure and re-enables the action.

## Verification Matrix

| Audit requirement | Authoritative evidence |
|---|---|
| Lunar New Year recurrence | focused JVM tests plus home/widget/reminder regression tests |
| Backup and device transfer | both XML rule tests and an emulator backup/restore smoke when transport is available |
| Reminder/calendar consistency | JSON tests, cleanup outcome tests, and permission-revocation emulator flow |
| Midnight widget freshness | provider action tests and emulator `DATE_CHANGED` refresh smoke |
| Signed Release artifacts | Gradle failure-without-key test, signed build, `apksigner`, and expected filename check |
| Atomic release and exact asset | PowerShell architecture tests plus a draft-only dry run against a disposable tag |
| Room migrations | connected API 36 migration suite for each supported hop and 7→10 |
| Dark calendar contrast | color guardrail unit test and dark-theme screenshot |
| Full search and subset reorder | >150-event search test and reorder merge tests |
| Import validation/deduplication | malformed JSON, legacy JSON, database duplicate, and in-file duplicate tests |
| Large font and accessibility | Compose semantics tests plus 150%/200% emulator screenshots and TalkBack smoke |
| Main-thread work | source guard tests and a cold-start/share/widget trace smoke |
| Wrapper, CI, and Lint | executable Git mode, workflow run, and zero-warning Lint reports |
| Play update behavior | flavor architecture test and Play About-screen instrumentation test |

## Release Acceptance

4.0 is release-ready only when every P1 and P2 row above has passing authoritative evidence, Direct and Play release artifacts are signed by the expected keys, Direct publishes exactly one verified APK, all audited Lint variants are warning-free, connected migration/UI tests run successfully on API 36, and Android 8/12/target-device smoke results are recorded in the release checklist.
