# TimeAPK Release and Update Guide

This document explains how to sign, build, and publish the current `3.7` release and how to keep using GitHub Releases as the update source.

## 1. Current Status

| Item | Status |
|------|--------|
| `applicationId` and versioning | Configured and overridable through `gradle.properties` |
| Min and target SDK | `minSdk 26` / `targetSdk 36` |
| Release build | `release` buildType enabled with `minify` and `shrinkResources` |
| Release signing | Reads signing data from `keystore.properties` |
| APK naming | Renames output to `glimmer-countdown-3-7.apk` |
| Update checking | GitHub Release checker and in-app entry already exist |
| Channels | Supports `direct` and `play` flavors |

## 2. Pre-release Preparation

### Signing Configuration

Create `keystore.properties` in the repo root:

```properties
storeFile=timeapk-release.keystore
storePassword=xxx
keyAlias=timeapk
keyPassword=xxx
```

Make sure these files are never committed:

- `keystore.properties`
- `*.keystore`

### Version Confirmation

Current release values:

- `VERSION_NAME=3.7`
- `VERSION_CODE=11`

For this `3.7` release, the version metadata, APK name, release notes, and release script must stay aligned. Increment `versionCode` before the next follow-up release.

## 3. Build Commands

```bash
./gradlew test
./gradlew assembleDirectRelease
./gradlew bundlePlayRelease
```

Expected APK path:

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-7.apk`

## 4. GitHub Release Steps

### Push Code

```bash
git add app gradle.properties README.md CHANGELOG.md docs scripts
git commit -m "release: prepare v3.7"
git push origin main
```

### Push Tag

```bash
git tag -a v3.7 -m "Release v3.7"
git push origin v3.7
```

### Publish Release

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

The script will:

- read the current version
- extract the `3.7` section from `CHANGELOG.md`
- call the GitHub Releases API
- upload the release APK

## 5. Relationship to In-app Update Checks

The project already includes:

- GitHub Release update checking
- an in-app "Check for updates" entry in settings
- flavor-aware version display

If you want to add full download and install support later, extend the existing update module instead of rebuilding the release pipeline.
