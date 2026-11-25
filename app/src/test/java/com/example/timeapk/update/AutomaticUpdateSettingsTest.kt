package com.example.timeapk.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticUpdateSettingsTest {
    private val hour = 3_600_000L
    private val now = 100 * hour
    private val release = CheckUpdateResult(true, "4.3.5", "https://example.com/verified.apk")

    @Test
    fun startupCheckIsThrottledForExactlyTwentyFourHours() {
        val settings = AutomaticUpdateSettings(lastCheckedAt = now)
        assertTrue(AutomaticUpdateSettings().canCheck(now))
        assertFalse(settings.canCheck(now + 24 * hour - 1))
        assertTrue(settings.canCheck(now + 24 * hour))
        assertTrue(settings.canCheck(now - 1))
        assertFalse(settings.copy(enabled = false).canCheck(now + 24 * hour))
    }

    @Test
    fun promptsRespectPostponementSkippedVersionAndValidRelease() {
        val settings = AutomaticUpdateSettings(postponedUntil = now + UPDATE_POSTPONE_MS)
        assertFalse(settings.shouldPrompt(release, now + 72 * hour - 1))
        assertTrue(settings.shouldPrompt(release, now + 72 * hour))
        assertFalse(settings.copy(skippedVersion = "4.3.5").shouldPrompt(release, now + 72 * hour))
        assertTrue(settings.copy(skippedVersion = "4.3.4").shouldPrompt(release, now + 72 * hour))
        assertFalse(AutomaticUpdateSettings().shouldPrompt(release.copy(downloadUrl = null), now))
        assertFalse(AutomaticUpdateSettings().shouldPrompt(release.copy(checkFailed = true), now))
    }
}
