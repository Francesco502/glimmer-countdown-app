package com.example.timeapk.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class ReleaseReadinessTest {
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
