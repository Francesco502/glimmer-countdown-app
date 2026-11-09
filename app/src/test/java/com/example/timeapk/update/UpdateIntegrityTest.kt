package com.example.timeapk.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest

class UpdateIntegrityTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun trustedUrlsRequireHttpsGithubAndApkSuffix() {
        assertTrue(
            isTrustedAssetUrl(
                "https://github.com/Francesco502/glimmer-countdown-app/releases/download/v4.3.0/glimmer-countdown-4-3-0.apk"
            )
        )
        assertFalse(isTrustedAssetUrl("https://example.com/glimmer-countdown-4-3-0.apk"))
        assertFalse(isTrustedAssetUrl("http://github.com/o/r/app.apk"))
        assertFalse(isTrustedAssetUrl("https://github.com/o/r/release.zip"))
        assertFalse(isTrustedAssetUrl("https://github.com.evil.test/o/r/app.apk"))
        assertFalse(isTrustedAssetUrl(""))
    }

    @Test
    fun digestParserAcceptsOnlySha256Hex() {
        val hex = "a".repeat(64)
        assertEquals(hex, parseSha256Digest("sha256:$hex"))
        assertEquals(hex, parseSha256Digest("SHA256:${hex.uppercase()}"))
        assertNull(parseSha256Digest(null))
        assertNull(parseSha256Digest("md5:${"a".repeat(32)}"))
        assertNull(parseSha256Digest("sha256:not-hex"))
        assertNull(parseSha256Digest("sha256:${"a".repeat(63)}"))
    }

    @Test
    fun checksumMismatchKeepsPreviousApkAndRemovesPartialFile() = runBlocking {
        val apk = File(temp.newFolder(), "update.apk").apply { writeText("previous complete APK") }
        val bytes = "new complete APK".toByteArray()

        try {
            writeDownloadedApk(
                input = ByteArrayInputStream(bytes),
                apkFile = apk,
                expectedLength = bytes.size.toLong(),
                expectedSha256 = "0".repeat(64)
            )
            fail("Expected UpdateChecksumException")
        } catch (_: UpdateChecksumException) {
            // expected
        }

        assertEquals("previous complete APK", apk.readText())
        assertFalse(File(apk.parentFile, "update.apk.part").exists())
    }

    @Test
    fun matchingChecksumPublishesTheNewApk() = runBlocking {
        val apk = File(temp.newFolder(), "update.apk").apply { writeText("previous complete APK") }
        val bytes = "new complete APK".toByteArray()
        val expected = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

        val actual = writeDownloadedApk(
            input = ByteArrayInputStream(bytes),
            apkFile = apk,
            expectedLength = bytes.size.toLong(),
            expectedSha256 = expected.uppercase()
        )

        assertEquals(expected, actual)
        assertArrayEquals(bytes, apk.readBytes())
        assertFalse(File(apk.parentFile, "update.apk.part").exists())
    }
}
