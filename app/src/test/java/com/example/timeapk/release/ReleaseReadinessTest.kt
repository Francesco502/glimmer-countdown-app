package com.example.timeapk.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class ReleaseReadinessTest {
    @Test
    fun versionConfigTargets313Release() {
        val properties = Properties().apply {
            rootGradlePropertiesFile().inputStream().use(::load)
        }

        assertEquals("18", properties.getProperty("VERSION_CODE"))
        assertEquals("3.13", properties.getProperty("VERSION_NAME"))

        val buildFile = appBuildGradleFile().readText(Charsets.UTF_8)
        assertTrue(buildFile.contains("versionCode = versionCodeOverride ?: 18"))
        assertTrue(buildFile.contains("versionName = versionNameOverride ?: \"3.13\""))
        assertTrue(buildFile.contains("val versionNameForApk = versionNameOverride ?: \"3.13\""))
    }

    @Test
    fun songTypographyUsesBundledNotoSerifFont() {
        val source = mainSource("ui/theme/Type.kt").readText(Charsets.UTF_8)

        assertTrue(source.contains("R.font.noto_serif_sc"))
        assertTrue(source.contains("Font("))
        assertFalse(source.contains("If a licensed Song-style font is bundled later"))
    }

    @Test
    fun eventEntryRequestsNotificationPermissionThroughCompatHelper() {
        val source = mainSource("ui/event/EventEntryScreen.kt").readText(Charsets.UTF_8)

        assertTrue(source.contains("notificationRuntimePermissionName()"))
        assertFalse(source.contains("Manifest.permission.POST_NOTIFICATIONS"))
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

    private fun existingFile(vararg paths: String): File {
        return paths.map(::File).firstOrNull(File::exists) ?: error("Missing file: ${paths.joinToString()}")
    }
}
