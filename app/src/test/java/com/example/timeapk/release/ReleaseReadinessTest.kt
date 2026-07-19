package com.example.timeapk.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class ReleaseReadinessTest {
    @Test
    fun repositoryProvidesExecutableWrapperAndSecretFreePullRequestCi() {
        val wrapper = existingFile("gradlew", "../gradlew")
        assertTrue("gradlew must be executable after checkout", wrapper.canExecute())

        val workflow = existingFile(
            ".github/workflows/android-ci.yml",
            "../.github/workflows/android-ci.yml"
        ).readText(Charsets.UTF_8)
        assertTrue(workflow.contains("pull_request:"))
        assertTrue(workflow.contains("contents: read"))
        assertTrue(workflow.contains("actions/checkout@v6"))
        assertTrue(workflow.contains("actions/setup-java@v5"))
        assertTrue(workflow.contains("gradle/actions/setup-gradle@v4"))
        assertTrue(workflow.contains("testDirectDebugUnitTest"))
        assertTrue(workflow.contains("testPlayDebugUnitTest"))
        assertTrue(workflow.contains("compileDirectDebugAndroidTestKotlin"))
        assertTrue(workflow.contains("compilePlayDebugAndroidTestKotlin"))
        assertTrue(workflow.contains("lintDirectRelease"))
        assertTrue(workflow.contains("lintPlayRelease"))
        assertTrue(workflow.contains("assembleDirectDebug"))
        assertTrue(workflow.contains("assemblePlayDebug"))
        assertTrue(workflow.contains("shell: pwsh"))
        assertTrue(workflow.contains("scripts/tests/publish-release-mock-harness.ps1 -Scenario all"))
        assertFalse(workflow.contains("assembleDirectRelease"))
        assertFalse(workflow.contains("GITHUB_TOKEN"))
        assertFalse(workflow.contains("TIMEAPK_KEYSTORE"))
    }

    @Test
    fun versionConfigTargets40Release() {
        val properties = Properties().apply {
            rootGradlePropertiesFile().inputStream().use(::load)
        }

        assertEquals("23", properties.getProperty("VERSION_CODE"))
        assertEquals("4.0", properties.getProperty("VERSION_NAME"))

        val buildFile = appBuildGradleFile().readText(Charsets.UTF_8)
        assertTrue(buildFile.contains("versionCode = versionCodeOverride ?: 23"))
        assertTrue(buildFile.contains("versionName = versionNameOverride ?: \"4.0\""))
        assertTrue(buildFile.contains("val versionNameForApk = versionNameOverride ?: \"4.0\""))
    }

    @Test
    fun releasePackagingFailsClosedButReleaseLintRemainsSecretFree() {
        val build = appBuildGradleFile().readText(Charsets.UTF_8)

        assertTrue(build.contains("validateReleaseSigning"))
        assertTrue(build.contains("Missing or invalid release signing configuration"))
        assertTrue(build.contains("(?:assemble|bundle).+Release"))
        assertTrue(build.contains("package.+Release(?:Bundle|UniversalApk)?"))
        assertTrue(build.contains("dependsOn(validateReleaseSigning)"))
        assertTrue(build.contains("mustRunAfter(validateReleaseSigning)"))
        assertFalse(build.contains("pre.+ReleaseBuild"))
        assertFalse(build.contains("Release.*"))
        assertFalse(build.contains("taskGraph.whenReady"))
    }

    @Test
    fun directArtifactRenameRequiresSignedSourceAndExactOutput() {
        val build = appBuildGradleFile().readText(Charsets.UTF_8)

        assertTrue(build.contains("glimmer-countdown-${'$'}{versionNameForApk.replace(\".\", \"-\")}"))
        assertTrue(build.contains("Missing signed Direct release APK"))
        assertTrue(build.contains("StandardCopyOption.REPLACE_EXISTING"))
        assertTrue(build.contains("abstract class RenameDirectReleaseApkTask"))
        assertTrue(build.contains("if (source.isFile)"))
        assertTrue(build.contains("target.isFile && target.length() > 0L"))
        assertTrue(build.contains("metadataText.contains(expectedMetadataEntry)"))
    }

    @Test
    fun releaseDocsTarget40MaturityCandidateAndDirectGithubApkOnly() {
        val readme = existingFile("README.md", "../README.md").readText(Charsets.UTF_8)
        val changelog = existingFile("CHANGELOG.md", "../CHANGELOG.md").readText(Charsets.UTF_8)
        val checklist = existingFile(
            "docs/RELEASE_CHECKLIST.md",
            "../docs/RELEASE_CHECKLIST.md"
        ).readText(Charsets.UTF_8)
        val githubGuide = existingFile(
            "docs/GITHUB_AND_RELEASE.md",
            "../docs/GITHUB_AND_RELEASE.md"
        ).readText(Charsets.UTF_8)
        val releaseGuide = existingFile(
            "docs/release_and_update_guide.md",
            "../docs/release_and_update_guide.md"
        ).readText(Charsets.UTF_8)
        val combined = listOf(readme, changelog, checklist, githubGuide, releaseGuide).joinToString("\n")

        assertTrue(readme.contains("4.0 发布目标"))
        assertTrue(readme.contains("最新公开版本仍为 `3.17`"))
        assertTrue(readme.contains("releases/tag/v3.17"))
        listOf(readme, changelog, checklist, githubGuide, releaseGuide).forEach { document ->
            assertTrue(document.replace("`", "").contains("最新公开版本仍为 3.17"))
        }
        assertTrue(changelog.contains("## [4.0] - 待发布"))
        assertTrue(checklist.contains("# 发布检查清单（v4.0）"))
        assertTrue(checklist.contains("发布状态：待验证"))
        assertTrue(combined.contains("versionCode=23") || combined.contains("versionCode`：`23"))
        assertTrue(combined.contains("glimmer-countdown-4-0.apk"))
        assertTrue(combined.contains("预览宽度 / 预览高度"))
        assertTrue(combined.contains("无可写系统日历"))
        assertFalse(combined.contains("小组件 2x2、3x3、4x2 模板"))

        val script = existingFile(
            "scripts/publish-release.ps1",
            "../scripts/publish-release.ps1"
        ).readText(Charsets.UTF_8)

        assertTrue(script.contains("Publish the direct APK to GitHub Release"))
        assertTrue(script.contains("publish-release.ps1 -Tag v4.0 -ReleaseName v4.0"))
        assertTrue(script.contains("Unable to resolve VERSION_NAME"))
        assertTrue(script.contains("does not match VERSION_NAME"))
        assertFalse(script.contains("${'$'}fallback = '3.17'"))
        assertFalse(script.contains("Upload-ReleaseAsset `\n    -Release ${'$'}release `\n    -AssetName ${'$'}aabName"))
        assertFalse(script.contains("ERROR: AAB not found"))
    }

    @Test
    fun releaseDocsRequireImmutableTagAndLockedOwnedDraftPublication() {
        val githubGuide = existingFile(
            "docs/GITHUB_AND_RELEASE.md",
            "../docs/GITHUB_AND_RELEASE.md"
        ).readText(Charsets.UTF_8)
        val releaseGuide = existingFile(
            "docs/release_and_update_guide.md",
            "../docs/release_and_update_guide.md"
        ).readText(Charsets.UTF_8)
        val combined = "$githubGuide\n$releaseGuide"

        assertTrue(combined.contains("最终发布 commit"))
        assertTrue(combined.contains("本地与远端 tag 解引用后的 commit"))
        assertTrue(combined.contains("ownership marker"))
        assertTrue(combined.contains("Git ref 锁"))
        assertTrue(combined.contains("残留锁"))
        assertTrue(combined.contains("GitHub Contents: write"))
        assertTrue(combined.contains("GLIMMER_RELEASE_CERT_SHA256"))
        assertTrue(combined.contains("ANDROID_HOME"))
        assertTrue(combined.contains("GITHUB_TOKEN"))
        assertTrue(combined.contains("size、digest、下载 URL"))
        assertTrue(combined.contains("最终 GET"))
        assertTrue(combined.contains("Play AAB 不上传 GitHub Release"))
        assertFalse(combined.contains("git tag -fa"))
        assertFalse(combined.contains("git push origin v4.0 --force"))
        assertFalse(combined.contains("自动更新已存在的 GitHub Release"))
        assertFalse(combined.contains("自动替换同名 Direct APK 资产"))
    }

    @Test
    fun releaseDocsRequireFreshTagBoundBuildAndSafeCredentialFlow() {
        val documents = listOf(
            existingFile("README.md", "../README.md"),
            existingFile("CHANGELOG.md", "../CHANGELOG.md"),
            existingFile("docs/GITHUB_AND_RELEASE.md", "../docs/GITHUB_AND_RELEASE.md"),
            existingFile("docs/RELEASE_CHECKLIST.md", "../docs/RELEASE_CHECKLIST.md"),
            existingFile("docs/release_and_update_guide.md", "../docs/release_and_update_guide.md"),
        ).associateWith { it.readText(Charsets.UTF_8) }
        val combined = documents.values.joinToString("\n")
        val githubGuide = documents.entries.single { it.key.name == "GITHUB_AND_RELEASE.md" }.value
        val releaseGuide = documents.entries.single { it.key.name == "release_and_update_guide.md" }.value

        listOf(githubGuide, releaseGuide).forEach { guide ->
            val cleanCommit = guide.indexOf("最终代码与发布文档已提交，且工作区干净")
            val immutableTag = guide.indexOf("创建并推送不可变的 exact tag")
            val freshBuild = guide.indexOf("从该 tag 对应 commit 的工作树重新正式签名构建")
            val verify = guide.indexOf("验证签名、渠道权限与 SHA-256")
            val credentials = guide.indexOf("准备安全凭据环境")
            val publish = guide.indexOf("运行发布脚本")
            assertTrue(cleanCommit >= 0)
            assertTrue(cleanCommit < immutableTag)
            assertTrue(immutableTag < freshBuild)
            assertTrue(freshBuild < verify)
            assertTrue(verify < credentials)
            assertTrue(credentials < publish)
            assertTrue(guide.contains("不得复用旧构建产物"))
            assertTrue(guide.contains("gh auth login"))
            assertTrue(guide.contains("gh auth token"))
            assertTrue(guide.contains("CI"))
        }

        assertTrue(combined.contains("删除 owned draft 中的所有旧资产"))
        assertTrue(combined.contains("整个 Release 只保留唯一的 exact Direct APK"))
        assertTrue(combined.contains("Play AAB 只交付 Play Console"))
        assertFalse(Regex("(?m)\\${'$'}env:GITHUB_TOKEN\\s*=").containsMatchIn(combined))
        assertFalse(combined.contains("不写入命令历史"))
    }

    @Test
    fun releaseChecklistDistinguishesVerifiedCandidateArtifactsFromFinalPublication() {
        val checklist = existingFile(
            "docs/RELEASE_CHECKLIST.md",
            "../docs/RELEASE_CHECKLIST.md"
        ).readText(Charsets.UTF_8)
        val publisherHarness = existingFile(
            "scripts/tests/publish-release-mock-harness.ps1",
            "../scripts/tests/publish-release-mock-harness.ps1"
        ).readText(Charsets.UTF_8)
        val githubGuide = existingFile(
            "docs/GITHUB_AND_RELEASE.md",
            "../docs/GITHUB_AND_RELEASE.md"
        ).readText(Charsets.UTF_8)

        assertTrue(checklist.contains("临时签名配置"))
        assertTrue(checklist.contains("候选提交使用与线上 v3.17 相同的正式发布证书"))
        assertTrue(checklist.contains("最终仍须从不可变 `v4.0` tag 再次新鲜构建"))
        assertTrue(checklist.contains("Direct / Play 各 410 项通过"))
        assertTrue(checklist.contains("- [x] Direct Debug APK 包含 `REQUEST_INSTALL_PACKAGES`"))
        assertTrue(checklist.contains("- [x] 使用正式发布证书重复完整构建、签名、权限"))
        assertTrue(checklist.contains("证书与线上 v3.17 一致"))
        assertTrue(checklist.contains("保留数据原地升级"))
        assertTrue(checklist.contains("AAB 已通过 bundletool 生成与安装测试"))
        assertTrue(checklist.contains("PowerShell 脚本运行（2026-07-17）"))
        assertTrue(checklist.contains("5/5 场景通过"))
        assertTrue(checklist.contains("真实 GitHub mutation：未检查"))
        assertTrue(checklist.contains("- [x] publisher 隔离 PowerShell 状态机验证"))
        assertTrue(publisherHarness.contains("'lock-contention'"))
        assertTrue(publisherHarness.contains("'owned-draft'"))
        assertTrue(publisherHarness.contains("'failure-cleanup'"))
        assertTrue(publisherHarness.contains("'residual-lock'"))
        assertTrue(publisherHarness.contains(". ${'$'}publisherPath"))
        assertTrue(githubGuide.contains("--network none"))
        assertTrue(checklist.contains("- [ ] 冷启动、首页滚动、月历切换、详情与设置导航"))
        assertTrue(checklist.contains("- [ ] 从该 tag 对应 commit 的工作树重新正式签名构建"))
        assertTrue(checklist.contains("- [ ] 发布后安装线上 APK 并完成更新检查与关键链路 smoke"))
        assertTrue(checklist.contains("Backup / restore smoke"))
        assertTrue(checklist.contains("筛选后的真实拖拽"))
        assertTrue(checklist.contains("真机：未检查"))
        assertTrue(checklist.contains("API 37"))
        assertFalse(checklist.contains("Android 17"))
        assertFalse(checklist.contains("- [x] 创建并推送 `v4.0` 标签"))
        assertFalse(checklist.contains("- [x] 发布 `glimmer-countdown-4-0.apk`"))
    }

    @Test
    fun songTypographyUsesBundledNotoSerifFont() {
        val source = mainSource("ui/theme/Type.kt").readText(Charsets.UTF_8)

        assertTrue(source.contains("R.font.noto_serif_sc"))
        assertTrue(fontResource("noto_serif_sc.ttf").isFile)
        assertTrue(fontResource("zcool_xiaowei_regular.ttf").isFile)
        assertFalse(fontResourcePath("noto_sans_sc.ttf").exists())
        assertTrue(source.contains("Font("))
        assertFalse(source.contains("If a licensed Song-style font is bundled later"))
    }

    @Test
    fun bundledFontResourcesStayWithinReleaseBudget() {
        val fontBytes = listOf(
            fontResource("noto_serif_sc.ttf"),
            fontResource("zcool_xiaowei_regular.ttf")
        ).sumOf { it.length() }

        assertTrue(
            "Bundled font resources are too large: $fontBytes bytes",
            fontBytes <= 32_000_000
        )
    }

    @Test
    fun playFlavorRemovesDirectApkUpdateCapability() {
        val buildFile = appBuildGradleFile().readText(Charsets.UTF_8)
        assertTrue(buildFile.contains("buildConfigField(\"boolean\", \"DIRECT_APK_UPDATES_ENABLED\", \"true\")"))
        assertTrue(buildFile.contains("buildConfigField(\"boolean\", \"DIRECT_APK_UPDATES_ENABLED\", \"false\")"))

        val playManifest = existingFile(
            "src/play/AndroidManifest.xml",
            "app/src/play/AndroidManifest.xml"
        ).readText(Charsets.UTF_8)
        assertTrue(playManifest.contains("android.permission.REQUEST_INSTALL_PACKAGES"))
        assertTrue(playManifest.contains("tools:node=\"remove\""))
    }

    @Test
    fun updateCheckerIsSelectedByChannelCapability() {
        val appSource = mainSource("TimeApplication.kt").readText(Charsets.UTF_8)
        val factorySource = mainSource("update/UpdateCheckerFactory.kt").readText(Charsets.UTF_8)

        assertTrue(appSource.contains("UpdateCheckerFactory.create()"))
        assertFalse(appSource.contains("GitHubReleaseUpdateChecker()"))
        assertTrue(factorySource.contains("BuildConfig.DIRECT_APK_UPDATES_ENABLED"))
        assertTrue(factorySource.contains("GitHubReleaseUpdateChecker()"))
        assertTrue(factorySource.contains("StubUpdateChecker()"))
    }

    @Test
    fun aboutScreenHidesManualUpdateCheckForPlayChannel() {
        val source = mainSource("ui/settings/SettingsSubScreens.kt").readText(Charsets.UTF_8)
        val about = source.substringAfter("fun AboutSettingsContent(")
        val controls = about.substringAfter("SettingsGroupHeader(title = stringResource(R.string.settings_about_entry_title))")

        assertTrue(about.contains("fun startUpdateCheck()"))
        assertTrue(controls.contains("if (directApkUpdatesEnabled)"))
        val manualActionIndex = controls.indexOf("R.string.settings_check_update")
        val directGateIndex = controls.lastIndexOf("if (directApkUpdatesEnabled) {", manualActionIndex)
        val directClickIndex = controls.indexOf("onClick = { startUpdateCheck() }", manualActionIndex)
        val managedStoreIndex = controls.indexOf("R.string.settings_updates_managed_by_store")
        assertTrue(directGateIndex in 0 until manualActionIndex)
        assertTrue(directClickIndex in (manualActionIndex + 1) until managedStoreIndex)
        assertTrue(
            controls.contains(
                "} else {\n            Text(\n                " +
                    "text = stringResource(R.string.settings_updates_managed_by_store)"
            )
        )
    }

    @Test
    fun composeEntryPointsStartWithStoredDefaultSongFontPreset() {
        listOf(
            mainSource("MainActivity.kt"),
            mainSource("widget/WidgetConfigActivity.kt")
        ).forEach { sourceFile ->
            val source = sourceFile.readText(Charsets.UTF_8)
            assertTrue(sourceFile.path, source.contains("FONT_PRESET_NOTO_SERIF_SC"))
            assertTrue(
                sourceFile.path,
                source.contains("fontPresetFlow.collectAsState(initial = FONT_PRESET_NOTO_SERIF_SC)")
            )
            assertFalse(sourceFile.path, source.contains("fontPresetFlow.collectAsState(initial = 0)"))
        }
    }

    @Test
    fun mainActivityUsesLocaleMirrorWithoutRunBlocking() {
        val main = mainSource("MainActivity.kt").readText()
        assertFalse(main.contains("runBlocking"))
        assertTrue(main.contains("LocalePreferenceMirror.read(newBase)"))
        assertTrue(main.contains("migrateLocaleMirror"))
        val prefs = mainSource("data/UserPreferencesRepository.kt").readText()
        val setter = prefs.substringAfter("suspend fun setLanguageMode")
            .substringBefore("suspend fun setDateFormatMode")
        assertTrue(setter.contains("LocalePreferenceMirror.write"))
    }

    @Test
    fun localeMirrorUsesCoreEditExtensionWithoutRawEditorChain() {
        val mirror = mainSource("LocalePreferenceMirror.kt").readText()

        assertTrue(mirror.contains("import androidx.core.content.edit"))
        assertTrue(mirror.contains(".edit(commit = false) {"))
        assertFalse(mirror.contains(".edit()\n            .putInt(KEY, mode)\n            .apply()"))
    }

    @Test
    fun nativeSplashAssetsRemainConfiguredWithoutComposeSplashRoute() {
        val timeAppSource = mainSource("TimeApp.kt").readText(Charsets.UTF_8)
        assertTrue(timeAppSource.contains("val startDestination = Routes.Home"))
        assertFalse(timeAppSource.contains("Routes.Splash"))
        assertFalse(timeAppSource.contains("SplashScreen"))
        assertFalse(File(mainSourceRoot(), "ui/splash/SplashScreen.kt").exists())

        val splashBackground = existingFile(
            "src/main/res/drawable/splash_background.xml",
            "app/src/main/res/drawable/splash_background.xml"
        ).readText(Charsets.UTF_8)
        assertTrue(splashBackground.contains("android:drawable=\"@drawable/ic_launcher_foreground\""))

        val android12Theme = existingFile(
            "src/main/res/values-v31/themes.xml",
            "app/src/main/res/values-v31/themes.xml"
        ).readText(Charsets.UTF_8)
        assertTrue(android12Theme.contains("android:windowSplashScreenAnimatedIcon"))
        assertTrue(android12Theme.contains("@drawable/ic_launcher_foreground"))

        val adaptiveIcon = existingFile(
            "src/main/res/drawable-anydpi/ic_launcher.xml",
            "app/src/main/res/drawable-anydpi/ic_launcher.xml"
        ).readText(Charsets.UTF_8)
        assertTrue(adaptiveIcon.contains("<monochrome android:drawable=\"@drawable/ic_launcher_monochrome\""))
        assertTrue(existingFile("src/main/res/drawable/ic_launcher_monochrome.xml", "app/src/main/res/drawable/ic_launcher_monochrome.xml").isFile)
        assertTrue(existingFile("src/main/res/drawable/ic_launcher_foreground.xml", "app/src/main/res/drawable/ic_launcher_foreground.xml").isFile)
    }

    @Test
    fun eventEntryRequestsNotificationPermissionThroughCompatHelper() {
        val source = mainSource("ui/event/EventEntryScreen.kt").readText(Charsets.UTF_8)

        assertTrue(source.contains("notificationRuntimePermissionName()"))
        assertFalse(source.contains("Manifest.permission.POST_NOTIFICATIONS"))
    }

    @Test
    fun notificationDeepLinksNavigateHomeBeforeShowingDetailOverlay() {
        val source = mainSource("TimeApp.kt").readText(Charsets.UTF_8)
        val launchBlock = source.substringAfter("LaunchedEffect(initialOpenEventId)")
            .substringBefore("val startDestination")

        assertTrue(launchBlock.contains("navController.navigate(Routes.Home)"))
        assertTrue(launchBlock.contains("launchSingleTop = true"))
        assertTrue(launchBlock.contains("selectedEventIdForDetail = id"))
    }

    @Test
    fun mainKotlinSourcesDoNotContainKnownMojibakeMarkers() {
        val markers = listOf("锛", "鎻", "閲", "鍒", "歊", "绯荤", "堕棿", "�")
        val offenders = mainSourceRoot()
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val content = file.readText(Charsets.UTF_8)
                val marker = markers.firstOrNull(content::contains)
                marker?.let { "${file.relativeTo(mainSourceRoot()).path}: $it" }
            }
            .toList()

        assertTrue("Mojibake markers found: $offenders", offenders.isEmpty())
    }

    @Test
    fun backupRulesIncludeRoomAndBothDataStoresForCloudAndTransfer() {
        val expectedIncludes = listOf(
            "database" to "event_database",
            "database" to "event_database-wal",
            "database" to "event_database-shm",
            "file" to "datastore/"
        )
        val documentBuilder = javax.xml.parsers.DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()

        fun directChildren(
            parent: org.w3c.dom.Element,
            tagName: String
        ): List<org.w3c.dom.Element> {
            return (0 until parent.childNodes.length)
                .map(parent.childNodes::item)
                .filterIsInstance<org.w3c.dom.Element>()
                .filter { it.tagName == tagName }
        }

        fun directIncludes(parent: org.w3c.dom.Element): List<Pair<String, String>> {
            return directChildren(parent, "include")
                .map { it.getAttribute("domain") to it.getAttribute("path") }
        }

        fun singleDirectChild(
            parent: org.w3c.dom.Element,
            tagName: String
        ): org.w3c.dom.Element {
            return directChildren(parent, tagName).single()
        }

        val legacyRoot = documentBuilder.parse(existingFile(
            "src/main/res/xml/backup_rules.xml",
            "app/src/main/res/xml/backup_rules.xml"
        )).documentElement
        assertEquals("full-backup-content", legacyRoot.tagName)
        assertEquals(expectedIncludes, directIncludes(legacyRoot))

        val modernRoot = documentBuilder.parse(existingFile(
            "src/main/res/xml/data_extraction_rules.xml",
            "app/src/main/res/xml/data_extraction_rules.xml"
        )).documentElement
        assertEquals("data-extraction-rules", modernRoot.tagName)

        val cloudBackup = singleDirectChild(modernRoot, "cloud-backup")
        val deviceTransfer = singleDirectChild(modernRoot, "device-transfer")
        assertEquals("false", cloudBackup.getAttribute("disableIfNoEncryptionCapabilities"))
        assertEquals(expectedIncludes, directIncludes(cloudBackup))
        assertEquals(expectedIncludes, directIncludes(deviceTransfer))
    }

    private fun rootGradlePropertiesFile(): File {
        return existingFile("gradle.properties", "../gradle.properties")
    }

    private fun appBuildGradleFile(): File {
        return listOf(File("build.gradle.kts"), File("app/build.gradle.kts"))
            .firstOrNull { file ->
                file.exists() && file.readText(Charsets.UTF_8).contains("com.android.application")
            }
            ?: error("Missing app build.gradle.kts")
    }

    private fun mainSource(relative: String): File {
        return existingFile("src/main/java/com/example/timeapk/$relative", "app/src/main/java/com/example/timeapk/$relative")
    }

    private fun mainSourceRoot(): File {
        return existingFile("src/main/java/com/example/timeapk", "app/src/main/java/com/example/timeapk")
    }

    private fun fontResource(name: String): File {
        return fontResourcePath(name).takeIf(File::exists) ?: error("Missing font resource: $name")
    }

    private fun fontResourcePath(name: String): File {
        val direct = File("src/main/res/font/$name")
        return if (direct.parentFile?.exists() == true) {
            direct
        } else {
            File("app/src/main/res/font/$name")
        }
    }

    private fun existingFile(vararg paths: String): File {
        return paths.map(::File).firstOrNull(File::exists) ?: error("Missing file: ${paths.joinToString()}")
    }
}
