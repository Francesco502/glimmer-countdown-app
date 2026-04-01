# Changelog

This document records version changes for Glimmer.

## [3.7] - 2026-04-01

### Changed

- Bumped the app version to `3.7` (`versionCode=11`).
- Reworked the home screen widget from a `RemoteViewsService / ListView` collection widget to a static multi-row layout rendered by the provider.
- Unified app theme settings, widget theme resolution, and widget refresh flow.
- Updated README, release docs, and release scripts for `3.7`.

### Fixed

- Fixed cases where the widget stayed in the previous light or dark appearance after a system theme change.
- Fixed cases where the widget became blank or stopped showing content after a theme switch.
- Fixed missed widget refreshes caused by activity recreation or cancelled UI-scoped jobs.

## [3.6] - 2026-03-17

### Changed

- Bumped the app version to `3.6` (`versionCode=10`).
- Limited lunar repeat options to `none` and `yearly` so the editor and runtime behavior stay consistent.
- Switched the home screen default sort mode to custom sort and kept drag and drop only for that mode.

### Fixed

- Fixed accidental milestone calendar deletions during normal system calendar reminder sync.
- Fixed missed milestone reminders for future events.
- Fixed update-check failures incorrectly reporting that the app was already up to date.
- Fixed release metadata drift in the `3.6` release pipeline.

## [3.5] - 2026-03-11

### Added

- Added a unified rebuild flow covering boot, timezone changes, manual time changes, and cold start recovery.
- Added reminder time and days-before pickers that support writing reminder entries to the system calendar on consecutive days.

### Changed

- Bumped the app version to `3.5` (`versionCode=9`).
- Reworked the settings information architecture and the event editor order.
- Updated README and release documents for the `3.5` release.

### Fixed

- Fixed a settings crash when calendar permission was not granted.
- Fixed rebuild jobs swallowing failures and preventing retries.
- Fixed drag-sort results being overwritten outside custom sort mode.
- Fixed inconsistent system calendar sync status reporting.
- Fixed birthday detail and widget text output that could only show plain numeric values.

## [3.4] - 2026-03-10

### Added

- Added richer home screen event management with search, combined filters, and calendar view.
- Added app-wide base font scaling and widget-specific font scaling.
- Added milestone reminder time configuration and post-import rescheduling.

### Changed

- Unified `3.4` release docs, README, and release checklist.
- Reworked table mode and widget display strategy for better readability.
- Consolidated reminder and system calendar sync entry points.

### Fixed

- Fixed expired repeating solar events failing to reschedule reminders and calendar sync.
- Fixed monthly and half-year repeats skipping incorrectly on same-day transitions.
- Fixed reminder scheduling gaps for large days-before values.
- Fixed one-shot reminder worker behavior.
- Fixed notification permission handling on Android 13 and above.

## [3.3] - 2026-03-04

- Added the system calendar sync switch and baseline sync capability.
- Aligned widget sorting semantics with the home screen.
- Completed the first round of widget copy and layout compression.

## [3.2] - 2026-03-03

- Improved the calendar picker interaction for solar and lunar modes.
- Improved home screen view switching and small-screen adaptation.

## [3.1] - 2026-03-02

- Added end-to-end support for lunar events.
- Added lunar indicators on the home screen and in widgets.

## [3.0] - 2025-02-28

- Reworked the theme and visual system.
- Improved home screen detail interactions and general stability.

## [2.0] - 2025-02-27

- Added the initial in-app update foundation and strengthened widget behavior.

## [1.0] - 2025-02-26

- Initial release with event management, reminders, themes, and widgets.
