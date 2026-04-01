# GitHub Commit and Release Flow (v3.7)

This document describes the Git workflow and GitHub Release steps for the `3.7` release.

## 1. Commit Locally

```bash
git status
git add app gradle.properties README.md CHANGELOG.md docs scripts
git commit -m "release: prepare v3.7"
```

Notes:

- Commit only app code, version metadata, release scripts, and release docs.
- Do not commit local cache folders such as `.gradle-user-home`, `.cursor`, `build`, or `.tmp`.
- If `docs/superpowers/` is only planning material, keep it out of the release commit.

## 2. Push to Remote

```bash
git push origin main
```

## 3. Tag the Release

```bash
git tag -a v3.7 -m "Release v3.7"
git push origin v3.7
```

## 4. Build the Release APK

```bash
./gradlew assembleDirectRelease
```

Expected output:

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-7.apk`

## 5. Create the GitHub Release

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

The script will:

- read `VERSION_NAME` from `gradle.properties`
- extract the `3.7` section from `CHANGELOG.md` as release notes
- create or reuse the `v3.7` GitHub Release
- upload the APK asset

## 6. Post-release Checks

- Confirm the title, tag, and notes all reference `v3.7`.
- Confirm the uploaded asset name is `glimmer-countdown-3-7.apk`.
- Confirm the app shows version `3.7` in Settings > About.
- Smoke test app theme switching, widget light/dark behavior, widget content rendering, and update checking.
