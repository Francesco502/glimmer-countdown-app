package com.example.timeapk.update

import com.example.timeapk.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseUpdateCheckerTest {

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
    }

    @Test
    fun checkUpdate_sameVersion_returnsNoUpdateWithoutFailure() = runBlocking {
        val checker = GitHubReleaseUpdateChecker(
            fetchRelease = {
                ReleaseFetchResult(
                    isSuccessful = true,
                    release = ReleaseInfo(
                        tagName = BuildConfig.VERSION_NAME,
                        releaseNotes = "notes",
                        downloadUrl = "https://example.com/app.apk"
                    )
                )
            }
        )

        val result = checker.checkUpdate()

        assertFalse(result.hasUpdate)
        assertFalse("unexpected failure: ${result.errorMessage}", result.checkFailed)
    }
}
