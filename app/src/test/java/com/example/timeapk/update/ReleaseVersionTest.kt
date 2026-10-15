package com.example.timeapk.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {

    private fun v(raw: String) = requireNotNull(parseReleaseVersion(raw)) { raw }

    @Test
    fun ordersReleaseAbovePrereleaseAboveOlder() {
        assertTrue(v("4.2.1") > v("4.2"))
        assertTrue(v("4.2") > v("4.2-rc1"))
        assertTrue(v("4.2-rc1") > v("4.1"))
        assertTrue(v("4.2-rc2") > v("4.2-rc1"))
        assertTrue(v("4.2-rc10") > v("4.2-rc2"))
        assertTrue(v("4.2-rc1") > v("4.2-beta"))
        assertTrue(v("4.2.beta") < v("4.2"))
        assertTrue(v("4.10") > v("4.9"))
    }

    @Test
    fun leadingVAndTrailingZerosAreEqual() {
        assertEquals(0, v("v4.2").compareTo(v("4.2")))
        assertEquals(0, v("4.2.0").compareTo(v("4.2")))
        assertEquals(v("4.2.0"), v("V4.2"))
        assertEquals(v("4.2.0").hashCode(), v("4.2").hashCode())
    }

    @Test
    fun parsesSuffixesAndCanonicalDisplay() {
        assertEquals(ReleaseVersion(listOf(4, 2), "rc1"), v(" v4.2-RC1 "))
        assertEquals("4.2-beta", v("4.2.beta").displayName)
        assertEquals("4.1", v("v04.01").displayName)
        assertEquals("4.2-beta.2", v("4.2-beta.2").displayName)
        assertTrue(v("4.2-rc1").isPrerelease)
        assertFalse(v("4.2").isPrerelease)
    }

    @Test
    fun unparseableReturnsNullInsteadOfThrowing() {
        listOf("", "v", "abc", "v5beta", "5..1", "4.2-", "4.2--rc", "999999999999999999999999").forEach {
            assertNull(it, parseReleaseVersion(it))
        }
    }

    @Test
    fun prereleaseAssetNameMirrorsBuildApkBaseName() {
        assertEquals("glimmer-countdown-4-2-1.apk", expectedDirectApkName(v("4.2.1")))
        assertEquals("glimmer-countdown-4-2-rc1.apk", expectedDirectApkName(v("v4.2-rc1")))
        assertEquals("glimmer-countdown-4-2.apk", expectedDirectApkName(v("4.2")))
    }

    @Test
    fun checker_unrecognizedTagReturnsExplicitVersionResult() = runBlocking {
        val result = checker("""{"tag_name":"abc","assets":[]}""").checkUpdate()
        assertFalse(result.hasUpdate)
        assertTrue(result.checkFailed)
        assertEquals(UpdateFailureKind.VERSION_UNRECOGNIZED, result.failureKind)
        assertEquals(UNRECOGNIZED_VERSION_ERROR, result.errorMessage)
    }

    @Test
    fun checker_unrecognizedInstalledVersionReturnsExplicitVersionResult() = runBlocking {
        val result = checker("""{"tag_name":"4.2","assets":[]}""", installed = "dev-build").checkUpdate()
        assertEquals(UpdateFailureKind.VERSION_UNRECOGNIZED, result.failureKind)
    }

    @Test
    fun checker_prereleaseOfInstalledNumberIsNotAnUpdate() = runBlocking {
        val result = checker("""{"tag_name":"v4.2-rc1","assets":[]}""", installed = "4.2").checkUpdate()
        assertFalse(result.hasUpdate)
        assertFalse(result.checkFailed)
    }

    @Test
    fun checker_equalVersionWithTrailingZeroIsNotAnUpdate() = runBlocking {
        val result = checker("""{"tag_name":"v4.2.0","assets":[]}""", installed = "4.2").checkUpdate()
        assertFalse(result.hasUpdate)
        assertFalse(result.checkFailed)
    }

    @Test
    fun checker_newerPrereleaseSelectsMatchingAssetAndExposesReleasePage() = runBlocking {
        val result = checker(
            """{"tag_name":"v4.2-rc1","body":"n","html_url":"https://github.com/o/r/releases/tag/v4.2-rc1",
               "assets":[{"name":"glimmer-countdown-4-2-rc1.apk","browser_download_url":"https://example/apk"}]}""",
            installed = "4.1"
        ).checkUpdate()
        assertTrue(result.hasUpdate)
        assertEquals("4.2-rc1", result.versionName)
        assertEquals("https://example/apk", result.downloadUrl)
        assertEquals("https://github.com/o/r/releases/tag/v4.2-rc1", result.releasePageUrl)
    }

    @Test
    fun checker_nonGithubHtmlUrlFallsBackToReleasesPage() = runBlocking {
        val result = checker(
            """{"tag_name":"4.2","html_url":"javascript:alert(1)",
               "assets":[{"name":"glimmer-countdown-4-2.apk","browser_download_url":"https://example/apk"}]}""",
            installed = "4.1"
        ).checkUpdate()
        assertTrue(result.hasUpdate)
        assertEquals(
            "https://github.com/Francesco502/glimmer-countdown-app/releases/latest",
            result.releasePageUrl
        )
    }

    private fun checker(json: String, installed: String = "4.0") = GitHubReleaseUpdateChecker(
        installedVersionName = installed,
        fetchRelease = { ReleaseFetchResult(isSuccessful = true, responseBody = json) }
    )
}
