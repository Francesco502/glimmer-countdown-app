# Glimmer

`v3.7` Android countdown, birthday, and anniversary app built with Jetpack Compose and Material 3.

## Version

- `versionName`: `3.7`
- `versionCode`: `11`
- Release date: `2026-04-01`

## Core Features

- Manage countdown, birthday, and anniversary events
- Support both solar and lunar calendar events
- Repeat rules for day, week, month, half-year, and year
- Custom reminders with "N days before + fixed time"
- System calendar sync with permission handling and sync status feedback
- Search, filters, custom sort, date-based sort, pinned items, and calendar view on the home screen
- Better Chinese time expressions in table mode and widgets
- JSON import and export for backup and restore

## 3.7 Highlights

- Reworked the home screen widget from a `RemoteViewsService + ListView` collection widget to a static multi-row widget rendered directly by the provider.
- Fixed cases where the widget stayed in the old light or dark appearance after a system theme change.
- Fixed cases where the widget could become blank or lose its content after theme switching.
- Unified theme resolution and refresh flow for app theme settings and widgets.
- Updated version metadata, APK naming, release script, README, and release docs for `3.7`.

## Build and Run

```bash
# Direct debug build
./gradlew installDirectDebug

# Play debug build
./gradlew installPlayDebug
```

```bash
# Direct release APK
./gradlew assembleDirectRelease

# Play release AAB
./gradlew bundlePlayRelease
```

Default output paths:

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-7.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## Release Docs

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
