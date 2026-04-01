# Release Checklist (v3.7)

**Version**: `3.7` (`versionCode=11`)
**Release date**: `2026-04-01`

## 1. Version and Docs

- [ ] `gradle.properties` contains `VERSION_CODE=11` and `VERSION_NAME=3.7`
- [ ] `app/build.gradle.kts` version reading, APK rename, and Play AAB flow work correctly
- [ ] `README.md`, `CHANGELOG.md`, and release docs match the current implementation
- [ ] GitHub Release tag and title use `v3.7`

## 2. Functional Regression Checks

- [ ] App theme follows system light and dark mode correctly
- [ ] Manual app theme switching between light and dark works immediately
- [ ] Widget follows system light and dark mode changes and does not stay on the old theme
- [ ] Widget updates after in-app theme changes and never becomes blank
- [ ] Repeating reminders work for day, week, month, half-year, and year cases
- [ ] "N days before" reminders write the expected series of system calendar entries
- [ ] Unified rebuild after boot, timezone changes, manual time changes, and cold start works correctly
- [ ] Notification taps still open the correct event detail
- [ ] System calendar permission, write, update, and delete flows remain consistent
- [ ] Home search, filters, sort modes, pinning, and calendar view still work together correctly

## 3. UI and Readability

- [ ] Settings structure, naming, and visual hierarchy remain consistent
- [ ] Table mode still renders Chinese relative time phrases correctly
- [ ] Widget key numbers and time text do not get truncated
- [ ] Small, medium, and large widget layouts switch display strategy correctly
- [ ] Light and dark themes keep key text readable

## 4. Build Outputs

- [ ] `./gradlew test` passes
- [ ] `./gradlew assembleDirectRelease` succeeds
- [ ] `./gradlew bundlePlayRelease` succeeds
- [ ] APK output path is `app/build/outputs/apk/direct/release/glimmer-countdown-3-7.apk`
- [ ] AAB output path is `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 5. Release Actions

- [ ] Changes are committed and pushed to `origin/main`
- [ ] Tag `v3.7` is created and pushed
- [ ] GitHub Release is created and includes the APK and `3.7` release notes
