# Glimmer 4.0 Release and Update Safety Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce only signed, deterministically named 4.0 artifacts, publish Direct releases atomically, and prevent either channel from selecting or advertising the wrong update path.

**Architecture:** Put release-package credential checks in Gradle task preflight while leaving Lint/tests secret-free. Parse the expected Direct asset through a pure Kotlin policy, and make the PowerShell publisher validate notes, signer, draft state, asset set, and post-upload metadata before publication.

**Tech Stack:** Gradle Kotlin DSL/AGP 9.1, Android build tools `apksigner`, JDK `jarsigner`, Kotlin/OkHttp/JSON, PowerShell/GitHub REST API, JUnit 4.

## Global Constraints

- Keep `versionName=4.0` and `versionCode=23` while this release remains unpublished.
- The exact Direct artifact name is `glimmer-countdown-4-0.apk`.
- Keep production signing material and certificate fingerprints outside Git.
- `lintDirectRelease`, `lintPlayRelease`, and tests run without signing secrets; Release packaging fails closed without them.
- Direct and Play remain separate application IDs and update channels.
- An already published target tag is immutable to the publisher.
- Preserve existing uncommitted 4.0 version/documentation edits and stage them only in their matching tasks.

---

## File Map

- `app/build.gradle.kts`: signing preflight and deterministic Direct artifact.
- `gradle.properties`: 4.0 version source.
- `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`: release configuration/source contracts.
- `app/src/main/java/com/example/timeapk/update/DirectReleaseAssetPolicy.kt`: exact asset name and unique match.
- `app/src/main/java/com/example/timeapk/update/GitHubReleaseUpdateChecker.kt`: use the exact Direct asset policy.
- `app/src/test/java/com/example/timeapk/update/GitHubReleaseUpdateCheckerTest.kt`: wrong/ambiguous asset regressions.
- `app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt`: channel-specific About UI.
- `app/src/main/res/values*/strings.xml`: store-managed update copy.
- `scripts/publish-release.ps1`: signer verification and draft-first atomic publication.
- `app/src/test/java/com/example/timeapk/release/ReleasePublicationContractTest.kt`: static publisher invariants.
- `README.md`, `CHANGELOG.md`, `docs/GITHUB_AND_RELEASE.md`, `docs/RELEASE_CHECKLIST.md`, `docs/release_and_update_guide.md`: exact 4.0 release process and readiness state.

### Task 1: Fail Release packaging closed and produce the exact Direct APK

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`
- Modify: `app/build.gradle.kts`
- Modify: `gradle.properties`

**Interfaces:**
- Produces: packaging-only `validateReleaseSigning` task wired into every Release assemble, bundle, package, and rename task.
- Produces: `renameDirectReleaseApk` output `app/build/outputs/apk/direct/release/glimmer-countdown-4-0.apk`.

- [ ] **Step 1: Add failing Gradle source contracts**

```kotlin
@Test
fun releasePackagingFailsClosedButReleaseLintRemainsSecretFree() {
    val build = appBuildGradleFile().readText()
    assertTrue(build.contains("validateReleaseSigning"))
    assertTrue(build.contains("Missing or invalid release signing configuration"))
    assertTrue(build.contains("assemble|bundle|package"))
    assertTrue(build.contains("dependsOn(validateReleaseSigning)"))
}

@Test
fun directArtifactRenameRequiresSignedSourceAndExactOutput() {
    val build = appBuildGradleFile().readText()
    assertTrue(build.contains("glimmer-countdown-${'$'}{versionNameForApk.replace(\".\", \"-\")}"))
    assertTrue(build.contains("Missing signed Direct release APK"))
    assertTrue(build.contains("StandardCopyOption.REPLACE_EXISTING"))
}
```

- [ ] **Step 2: Run the contracts and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest`

Expected: the new signing/rename assertions fail.

- [ ] **Step 3: Parse and validate external signing properties**

```kotlin
val keystorePropertiesFile = providers.environmentVariable("TIMEAPK_KEYSTORE_PROPERTIES")
    .map(::file)
    .orElse(provider { rootProject.file("keystore.properties") })
    .get()
val signingProperties = keystorePropertiesFile.takeIf(File::isFile)?.let { file ->
    Properties().apply { file.reader().use(::load) }
}
val signingKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val resolvedStoreFile = signingProperties?.getProperty("storeFile")?.let { value ->
    keystorePropertiesFile.parentFile.resolve(value).canonicalFile
}
val hasValidReleaseSigning = signingProperties != null &&
    signingKeys.all { !signingProperties.getProperty(it).isNullOrBlank() } &&
    resolvedStoreFile?.isFile == true

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    doLast {
        check(hasValidReleaseSigning) {
            "Missing or invalid release signing configuration: ${keystorePropertiesFile.path}"
        }
    }
}
```

Create the `release` signing config only when `hasValidReleaseSigning`; assign it to the Release build type when present. After all Android tasks are registered, attach the validation task only to Release packaging entry points:

```kotlin
val releasePackagingTask = Regex("(?i)^(assemble|bundle|package).+Release$")
tasks.configureEach {
    if (releasePackagingTask.matches(name) || name == "renameDirectReleaseApk") {
        dependsOn(validateReleaseSigning)
    }
}
```

This also closes aggregate `assemble` and `bundle`: their Release variant dependencies cannot finish without validation. Lint, compilation, and unit-test tasks never depend on the gate and remain secret-free.

- [ ] **Step 4: Make the rename task require and move the signed source**

```kotlin
import java.nio.file.Files
import java.nio.file.StandardCopyOption

tasks.register("renameDirectReleaseApk") {
    dependsOn("packageDirectRelease")
    doLast {
        val releaseDir = layout.buildDirectory.dir("outputs/apk/direct/release").get().asFile
        val source = File(releaseDir, "app-direct-release.apk")
        require(source.isFile) { "Missing signed Direct release APK: ${source.path}" }
        val target = File(releaseDir, "$apkBaseName.apk")
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        require(target.isFile && target.length() > 0L) { "Missing renamed Direct release APK" }
        File(releaseDir, "output-metadata.json").takeIf(File::isFile)?.let { metadata ->
            metadata.writeText(
                metadata.readText().replace(
                    "\"outputFile\": \"app-direct-release.apk\"",
                    "\"outputFile\": \"$apkBaseName.apk\""
                )
            )
        }
    }
}
```

- [ ] **Step 5: Verify secret-free and fail-closed paths**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lintDirectRelease testDirectDebugUnitTest`

Expected: configuration succeeds without a keystore.

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDirectRelease`

Expected: FAIL before compilation with `Missing or invalid release signing configuration`.

- [ ] **Step 6: Verify the signed path with an ephemeral external keystore**

Create `/tmp/timeapk-v4-signing/qa.jks` with `keytool`, and `/tmp/timeapk-v4-signing/keystore.properties` containing relative `storeFile=qa.jks` plus test-only passwords/alias. Do not put either file under the repository.

Run: `TIMEAPK_KEYSTORE_PROPERTIES=/tmp/timeapk-v4-signing/keystore.properties JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDirectRelease assemblePlayRelease bundlePlayRelease`

Expected: all packaging tasks pass and `glimmer-countdown-4-0.apk` exists.

Run: `$HOME/Library/Android/sdk/build-tools/37.0.0/apksigner verify --print-certs app/build/outputs/apk/direct/release/glimmer-countdown-4-0.apk`

Expected: `Verified` and the ephemeral certificate SHA-256 is printed.

Run: `'/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/jarsigner' -verify -verbose -certs app/build/outputs/bundle/playRelease/app-play-release.aab`

Expected: `jar verified`.

- [ ] **Step 7: Commit version/signing/artifact behavior**

```bash
git add app/build.gradle.kts gradle.properties app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt
git commit -m "build: require signed 4.0 release artifacts"
```

### Task 2: Select only the exact Direct APK asset

**Files:**
- Create: `app/src/main/java/com/example/timeapk/update/DirectReleaseAssetPolicy.kt`
- Modify: `app/src/main/java/com/example/timeapk/update/GitHubReleaseUpdateChecker.kt`
- Modify: `app/src/test/java/com/example/timeapk/update/GitHubReleaseUpdateCheckerTest.kt`

**Interfaces:**
- Produces: `ReleaseAsset(name: String, downloadUrl: String)`.
- Produces: `expectedDirectApkName(remoteVersion: String): String`.
- Produces: `selectDirectApkAsset(remoteVersion, assets): ReleaseAsset?` returning a match only when unique.

- [ ] **Step 1: Add wrong-first and ambiguous-asset tests**

```kotlin
@Test
fun selectDirectApkAsset_ignoresOtherApksAndFindsExactVersionedName() {
    val assets = listOf(
        ReleaseAsset("app-play-release.apk", "https://example/play"),
        ReleaseAsset("glimmer-countdown-3-17.apk", "https://example/old"),
        ReleaseAsset("glimmer-countdown-4-0.apk", "https://example/direct")
    )
    assertEquals(
        "https://example/direct",
        selectDirectApkAsset("v4.0", assets)?.downloadUrl
    )
}

@Test
fun selectDirectApkAsset_returnsNullForMissingOrDuplicateExpectedAsset() {
    assertNull(selectDirectApkAsset("4.0", listOf(ReleaseAsset("other.apk", "x"))))
    assertNull(
        selectDirectApkAsset(
            "4.0",
            listOf(
                ReleaseAsset("glimmer-countdown-4-0.apk", "a"),
                ReleaseAsset("glimmer-countdown-4-0.apk", "b")
            )
        )
    )
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.update.GitHubReleaseUpdateCheckerTest`

Expected: compilation fails because the policy types/functions do not exist.

- [ ] **Step 3: Implement exact, unique asset matching**

```kotlin
package com.example.timeapk.update

internal data class ReleaseAsset(val name: String, val downloadUrl: String)

internal fun expectedDirectApkName(remoteVersion: String): String {
    val clean = remoteVersion.trim().removePrefix("v")
    return "glimmer-countdown-${clean.replace('.', '-')}.apk"
}

internal fun selectDirectApkAsset(
    remoteVersion: String,
    assets: List<ReleaseAsset>
): ReleaseAsset? {
    val expected = expectedDirectApkName(remoteVersion)
    return assets.filter { it.name == expected && it.downloadUrl.isNotBlank() }.singleOrNull()
}
```

- [ ] **Step 4: Parse every asset, then apply the policy**

```kotlin
val tagName = json.getString("tag_name").trim()
val assetsJson = json.optJSONArray("assets") ?: JSONArray()
val assets = buildList {
    for (index in 0 until assetsJson.length()) {
        val asset = assetsJson.getJSONObject(index)
        add(
            ReleaseAsset(
                name = asset.optString("name").trim(),
                downloadUrl = asset.optString("browser_download_url").trim()
            )
        )
    }
}
val downloadUrl = selectDirectApkAsset(tagName, assets)?.downloadUrl
```

Keep the version returned to UI without the leading `v`. If the exact asset is missing/ambiguous, return `checkFailed=true` with a distinct `Expected Direct APK asset is missing or ambiguous` message rather than `hasUpdate=false`.

- [ ] **Step 5: Run update tests and record GREEN**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.update.GitHubReleaseUpdateCheckerTest`

Expected: PASS.

- [ ] **Step 6: Commit exact asset selection**

```bash
git add app/src/main/java/com/example/timeapk/update/DirectReleaseAssetPolicy.kt app/src/main/java/com/example/timeapk/update/GitHubReleaseUpdateChecker.kt app/src/test/java/com/example/timeapk/update/GitHubReleaseUpdateCheckerTest.kt
git commit -m "fix: select the exact Direct update asset"
```

### Task 3: Remove fake update checking from Play UI

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`
- Modify: `app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Interfaces:**
- Consumes: `BuildConfig.DIRECT_APK_UPDATES_ENABLED`.
- Produces: Direct-only `settings_check_update` action and Play store-managed explanatory text.

- [ ] **Step 1: Strengthen the flavor UI contract**

```kotlin
@Test
fun aboutScreenHidesManualUpdateCheckForPlayChannel() {
    val source = mainSource("ui/settings/SettingsSubScreens.kt").readText()
    val about = source.substringAfter("fun AboutSettingsContent(")
    assertTrue(about.contains("if (directApkUpdatesEnabled)"))
    assertTrue(about.contains("R.string.settings_updates_managed_by_store"))
    assertTrue(about.indexOf("if (directApkUpdatesEnabled)") < about.indexOf("R.string.settings_check_update"))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest.aboutScreenHidesManualUpdateCheckForPlayChannel`

Expected: FAIL because the action is unconditional.

- [ ] **Step 3: Render channel-specific About content**

```kotlin
fun startUpdateCheck() {
    if (updateCheckInProgress) return
    updateResult = null
    updateCheckInProgress = true
    scope.launch {
        val result = try {
            app.updateChecker.checkUpdate()
        } catch (_: Exception) {
            updateCheckInProgress = false
            snackbarHostState.showSnackbar(
                context.getString(R.string.update_error),
                withDismissAction = true
            )
            return@launch
        }
        updateCheckInProgress = false
        updateResult = result
        when {
            result.checkFailed -> snackbarHostState.showSnackbar(
                context.getString(R.string.update_error),
                withDismissAction = true
            )
            !result.hasUpdate -> snackbarHostState.showSnackbar(
                context.getString(R.string.update_latest),
                withDismissAction = true
            )
        }
    }
}

if (directApkUpdatesEnabled) {
    SettingsActionRow(
        label = stringResource(R.string.settings_check_update),
        supportingText = if (updateCheckInProgress) {
            stringResource(R.string.settings_check_update_loading)
        } else null,
        onClick = { startUpdateCheck() }
    )
} else {
    Text(
        text = stringResource(R.string.settings_updates_managed_by_store),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}
```

Place `startUpdateCheck()` beside the existing About-screen state; it must be called only from the Direct branch.

- [ ] **Step 4: Add localized store-managed copy**

```xml
<!-- values/strings.xml and values-zh/strings.xml -->
<string name="settings_updates_managed_by_store">更新由应用商店管理</string>
<!-- values-en/strings.xml -->
<string name="settings_updates_managed_by_store">Updates are managed by the app store</string>
```

- [ ] **Step 5: Run source tests and compile Play**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest compilePlayDebugKotlin`

Expected: PASS.

- [ ] **Step 6: Commit channel-correct About behavior**

```bash
git add app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt app/src/main/res/values app/src/main/res/values-zh app/src/main/res/values-en app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt
git commit -m "fix: make Play updates store managed"
```

### Task 4: Make publication draft-first, signer-verified, and asset-atomic

**Files:**
- Create: `app/src/test/java/com/example/timeapk/release/ReleasePublicationContractTest.kt`
- Modify: `scripts/publish-release.ps1`

**Interfaces:**
- Consumes: `GLIMMER_RELEASE_CERT_SHA256`, GitHub token/`gh auth token`, exact Direct APK.
- Produces: draft-only upload flow that publishes after exact-asset verification.

- [ ] **Step 1: Add failing publisher contracts**

```kotlin
@Test
fun publisherValidatesEverythingBeforeNetworkMutation() {
    val script = releaseScript()
    val firstMutation = listOf("-Method Post", "-Method Patch", "-Method Delete")
        .map(script::indexOf).filter { it >= 0 }.min()
    assertTrue(script.indexOf("& ${'$'}apksigner verify --print-certs") in 0 until firstMutation)
    assertTrue(script.indexOf("GLIMMER_RELEASE_CERT_SHA256") in 0 until firstMutation)
    assertTrue(script.indexOf("Missing changelog section") in 0 until firstMutation)
}

@Test
fun publisherUsesDraftAndPublishesOnlyAfterExactAssetVerification() {
    val script = releaseScript()
    assertTrue(script.contains("draft = ${'$'}true"))
    assertTrue(script.contains("already published; refusing to mutate"))
    assertTrue(script.contains("Where-Object { ${'$'}_.name -like '*.apk' }"))
    assertTrue(script.contains("exactly one verified APK"))
    assertTrue(script.lastIndexOf("draft = ${'$'}false") > script.indexOf("exactly one verified APK"))
}
```

- [ ] **Step 2: Run and record RED**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleasePublicationContractTest`

Expected: the new contract fails against the current public-first publisher.

- [ ] **Step 3: Make changelog and certificate input mandatory before auth/network**

```powershell
function Get-ReleaseNotes {
    param([string]$Path, [string]$VersionName)
    if (-not (Test-Path $Path)) { throw "Missing changelog: $Path" }
    $content = Get-Content $Path -Raw -Encoding UTF8
    $escapedVersion = [regex]::Escape($VersionName)
    $pattern = "(?ms)^##\s+\[$escapedVersion\][^\r\n]*\r?\n(.*?)(?=\r?\n##\s+\[|\z)"
    if ($content -notmatch $pattern -or [string]::IsNullOrWhiteSpace($Matches[1])) {
        throw "Missing changelog section for $VersionName"
    }
    return $Matches[1].Trim()
}

$expectedCert = $env:GLIMMER_RELEASE_CERT_SHA256
if ([string]::IsNullOrWhiteSpace($expectedCert)) {
    throw 'GLIMMER_RELEASE_CERT_SHA256 is required.'
}
$apksigner = Get-ChildItem (Join-Path $env:ANDROID_HOME 'build-tools') -Recurse -File |
    Where-Object { $_.Name -in @('apksigner', 'apksigner.bat') } |
    Sort-Object { [version]$_.Directory.Name } -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $apksigner) { throw 'Unable to locate apksigner under ANDROID_HOME/build-tools.' }
$verifyOutput = & $apksigner verify --print-certs $apkPath 2>&1
if ($LASTEXITCODE -ne 0) { throw "apksigner verification failed: $verifyOutput" }
$actualCert = (($verifyOutput | Select-String 'Signer #1 certificate SHA-256 digest:').Line -split ': ', 2)[1]
if (($actualCert -replace ':', '').ToUpperInvariant() -ne ($expectedCert -replace ':', '').ToUpperInvariant()) {
    throw "APK signer mismatch. Expected $expectedCert, got $actualCert"
}
```

Resolve `$apkPath`, release notes, expected certificate, and authentication token before the first `Invoke-RestMethod` with `Post`, `Patch`, or `Delete`.

- [ ] **Step 4: Create or update draft only and reject published target tags**

```powershell
try {
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/tags/$Tag" -Method Get -Headers $headers
    if (-not $release.draft) { throw "Release $Tag is already published; refusing to mutate." }
} catch {
    if ((Get-StatusCode -ErrorRecord $_) -ne 404) { throw }
    $draftBody = @{ tag_name=$Tag; name=$ReleaseName; body=$releaseNotes; draft = $true; prerelease=$false } | ConvertTo-Json
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases" -Method Post -Headers $headers -Body $draftBody -ContentType 'application/json; charset=utf-8'
}
```

For an existing draft, patch only its name/body while keeping `draft=$true`.

- [ ] **Step 5: Replace every APK in the draft, upload, refetch, verify, then publish**

```powershell
$release.assets | Where-Object { $_.name -like '*.apk' } | ForEach-Object {
    Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/assets/$($_.id)" -Method Delete -Headers $headers
}
Upload-ReleaseAsset -Release $release -AssetName $apkName -AssetPath $apkPath -ContentType 'application/vnd.android.package-archive' -Headers $headers -Owner $owner -Repo $repo
$verified = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/$($release.id)" -Method Get -Headers $headers
$apkAssets = @($verified.assets | Where-Object { $_.name -like '*.apk' })
if ($apkAssets.Count -ne 1 -or $apkAssets[0].name -ne $apkName -or [long]$apkAssets[0].size -le 0) {
    throw "Draft does not contain exactly one verified APK named $apkName"
}
$publishBody = @{ name=$ReleaseName; body=$releaseNotes; draft = $false; prerelease=$false } | ConvertTo-Json
$published = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/$($release.id)" -Method Patch -Headers $headers -Body $publishBody -ContentType 'application/json; charset=utf-8'
```

- [ ] **Step 6: Run publication source contracts**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleasePublicationContractTest --tests com.example.timeapk.release.ReleaseReadinessTest`

Expected: PASS.

- [ ] **Step 7: Run a draft-only disposable-tag dry run where PowerShell and GitHub auth are available**

Use a test repository or disposable tag and stop before the final publish patch. Expected: wrong fingerprint, missing notes, existing published tag, upload failure, or wrong post-upload asset set leaves no new public release.

- [ ] **Step 8: Commit atomic publication**

```bash
git add scripts/publish-release.ps1 app/src/test/java/com/example/timeapk/release/ReleasePublicationContractTest.kt app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt
git commit -m "fix: publish verified Direct releases atomically"
```

### Task 5: Align 4.0 release documentation with the enforced pipeline

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/GITHUB_AND_RELEASE.md`
- Modify: `docs/RELEASE_CHECKLIST.md`
- Modify: `docs/release_and_update_guide.md`
- Modify: `app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt`

- [ ] **Step 1: Add documentation assertions for every enforced invariant**

```kotlin
assertTrue(combined.contains("TIMEAPK_KEYSTORE_PROPERTIES"))
assertTrue(combined.contains("GLIMMER_RELEASE_CERT_SHA256"))
assertTrue(combined.contains("glimmer-countdown-4-0.apk"))
assertTrue(combined.contains("草稿") || combined.contains("draft"))
assertTrue(combined.contains("apksigner"))
assertTrue(combined.contains("jarsigner"))
assertTrue(checklist.contains("发布状态：待验证"))
```

- [ ] **Step 2: Run and record RED against incomplete docs**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest.releaseDocsTarget40MaturityCandidateAndDirectGithubApkOnly`

Expected: at least the signing/fingerprint/draft assertions fail.

- [ ] **Step 3: Document exact commands and keep readiness unclaimed**

```markdown
1. Export `TIMEAPK_KEYSTORE_PROPERTIES` to an external properties file.
2. Run `./gradlew assembleDirectRelease assemblePlayRelease bundlePlayRelease`.
3. Verify the Direct/Play APKs with `apksigner verify --print-certs`.
4. Verify the Play AAB with `jarsigner -verify -verbose -certs`.
5. Export `GLIMMER_RELEASE_CERT_SHA256` with the production Direct fingerprint.
6. Run `scripts/publish-release.ps1`; it uploads a draft and publishes only after exact-asset verification.
```

State explicitly that 3.17 remains the latest public release until the final 4.0 gate is complete.

- [ ] **Step 4: Run release documentation and update tests**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.release.ReleaseReadinessTest --tests com.example.timeapk.release.ReleasePublicationContractTest --tests com.example.timeapk.update.GitHubReleaseUpdateCheckerTest`

Expected: PASS.

- [ ] **Step 5: Commit documentation separately**

```bash
git add README.md CHANGELOG.md docs/GITHUB_AND_RELEASE.md docs/RELEASE_CHECKLIST.md docs/release_and_update_guide.md app/src/test/java/com/example/timeapk/release/ReleaseReadinessTest.kt
git commit -m "docs: define the verified 4.0 release process"
```

### Task 6: Run release-safety acceptance checks

- [ ] **Step 1: Prove missing-key failure and secret-free Lint**

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lintDirectRelease testDirectDebugUnitTest`

Expected: configuration succeeds without a keystore.

Run: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDirectRelease`

Expected: failure before compilation with `Missing or invalid release signing configuration`. Retain both outputs in the release checklist.

- [ ] **Step 2: Build with the ephemeral key and verify all three artifacts**

Expected: exact Direct APK plus signed Play APK/AAB; no `*-unsigned.apk` is accepted as evidence.

- [ ] **Step 3: Confirm Play UI and Direct exact-asset behavior on emulator**

Expected: Play says store-managed and has no check action; Direct rejects a release JSON fixture whose first APK is wrong and selects the exact versioned asset when present once.

- [ ] **Step 4: Leave production signer/publication boxes unchecked until real credentials are supplied**

```markdown
- [x] Ephemeral-key pipeline proof
- [ ] Production Direct certificate fingerprint verified
- [ ] Production Play upload certificate verified
- [ ] v4.0 draft uploaded and exact asset verified
- [ ] v4.0 published
```

Do not publish or mark 4.0 release-ready with the ephemeral key.
