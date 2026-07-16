package com.example.timeapk.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseUpdateCheckerTest {

    @Test
    fun selectDirectApkAsset_ignoresPlayAndOldApksAndFindsExactVersionedName() {
        val assets = listOf(
            ReleaseAsset("app-play-release.apk", "https://example/play"),
            ReleaseAsset("glimmer-countdown-3-17.apk", "https://example/old"),
            ReleaseAsset("glimmer-countdown-4-1.apk", "https://example/direct")
        )

        assertEquals(
            "https://example/direct",
            selectDirectApkAsset("v4.1", assets)?.downloadUrl
        )
    }

    @Test
    fun selectDirectApkAsset_returnsNullForMissingDuplicateOrBlankUrl() {
        assertNull(selectDirectApkAsset("4.1", listOf(ReleaseAsset("other.apk", "x"))))
        assertNull(
            selectDirectApkAsset(
                "4.1",
                listOf(
                    ReleaseAsset("glimmer-countdown-4-1.apk", "a"),
                    ReleaseAsset("glimmer-countdown-4-1.apk", "b")
                )
            )
        )
        assertNull(
            selectDirectApkAsset(
                "4.1",
                listOf(ReleaseAsset("glimmer-countdown-4-1.apk", "   "))
            )
        )
    }

    @Test
    fun checkUpdate_parsesAllAssetsSelectsExactApkAndRemovesTagPrefixForUi() = runBlocking {
        val checker = checkerReturning(
            releaseJson(
                tag = "v4.1",
                assets = listOf(
                    "app-play-release.apk" to "https://example/play",
                    "glimmer-countdown-3-17.apk" to "https://example/old",
                    "glimmer-countdown-4-1.apk" to "https://example/direct"
                )
            )
        )

        val result = checker.checkUpdate()

        assertTrue(result.hasUpdate)
        assertFalse(result.checkFailed)
        assertEquals("4.1", result.versionName)
        assertEquals("https://example/direct", result.downloadUrl)
        assertEquals("notes", result.releaseNotes)
    }

    @Test
    fun checkUpdate_newerReleaseMissingExactAsset_marksCheckAsFailed() = runBlocking {
        val checker = checkerReturning(
            releaseJson(
                tag = "v4.1",
                assets = listOf("app-direct-release.apk" to "https://example/wrong")
            )
        )

        val result = checker.checkUpdate()

        assertFalse(result.hasUpdate)
        assertTrue(result.checkFailed)
        assertEquals(EXPECTED_DIRECT_APK_ERROR, result.errorMessage)
    }

    @Test
    fun checkUpdate_newerReleaseDuplicateExactAsset_marksCheckAsFailed() = runBlocking {
        val checker = checkerReturning(
            releaseJson(
                tag = "v4.1",
                assets = listOf(
                    "glimmer-countdown-4-1.apk" to "https://example/a",
                    "glimmer-countdown-4-1.apk" to "https://example/b"
                )
            )
        )

        val result = checker.checkUpdate()

        assertFalse(result.hasUpdate)
        assertTrue(result.checkFailed)
        assertEquals(EXPECTED_DIRECT_APK_ERROR, result.errorMessage)
    }

    @Test
    fun checkUpdate_newerReleaseExactAssetWithBlankUrl_marksCheckAsFailed() = runBlocking {
        val checker = checkerReturning(
            releaseJson(
                tag = "v4.1",
                assets = listOf("glimmer-countdown-4-1.apk" to "")
            )
        )

        val result = checker.checkUpdate()

        assertFalse(result.hasUpdate)
        assertTrue(result.checkFailed)
        assertEquals(EXPECTED_DIRECT_APK_ERROR, result.errorMessage)
    }

    @Test
    fun checkUpdate_sameOrOlderVersion_returnsNoUpdateWithoutRequiringAsset() = runBlocking {
        listOf("4.0", "v3.17").forEach { tag ->
            val result = checkerReturning(releaseJson(tag = tag, assets = emptyList())).checkUpdate()

            assertFalse(tag, result.hasUpdate)
            assertFalse("$tag unexpected failure: ${result.errorMessage}", result.checkFailed)
        }
    }

    @Test
    fun checkUpdate_httpFailure_marksCheckAsFailed() = runBlocking {
        val checker = GitHubReleaseUpdateChecker(
            fetchRelease = {
                ReleaseFetchResult(
                    isSuccessful = false,
                    errorMessage = "HTTP 500"
                )
            }
        )

        val result = checker.checkUpdate()

        assertFalse(result.hasUpdate)
        assertTrue(result.checkFailed)
        assertEquals("HTTP 500", result.errorMessage)
    }

    private fun checkerReturning(json: String) = GitHubReleaseUpdateChecker(
        fetchRelease = {
            ReleaseFetchResult(
                isSuccessful = true,
                responseBody = json
            )
        }
    )

    private fun releaseJson(
        tag: String,
        assets: List<Pair<String, String>>
    ): String {
        val assetJson = assets.joinToString(",") { (name, url) ->
            """{"name":"$name","browser_download_url":"$url"}"""
        }
        return """{"tag_name":"$tag","body":"notes","assets":[$assetJson]}"""
    }
}
