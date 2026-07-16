package com.example.timeapk.release

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleasePublicationContractTest {
    @Test
    fun publisherValidatesEverythingBeforeNetworkMutation() {
        val script = releaseScript()
        val firstMutation = listOf("-Method Post", "-Method Patch", "-Method Delete")
            .map(script::indexOf)
            .filter { it >= 0 }
            .minOrNull() ?: error("No network mutation found")

        listOf(
            "Missing changelog section",
            "GLIMMER_RELEASE_CERT_SHA256 is required",
            "Invalid GLIMMER_RELEASE_CERT_SHA256",
            "APK not found",
            "& ${'$'}apksigner verify --print-certs",
            "Unable to resolve GitHub authentication token"
        ).forEach { marker ->
            assertTrue("$marker must precede network mutation", script.indexOf(marker) in 0 until firstMutation)
        }
    }

    @Test
    fun publisherUsesDraftAndPublishesOnlyAfterExactAssetVerification() {
        val script = releaseScript()

        assertTrue(script.contains("draft = ${'$'}true"))
        assertTrue(script.contains("already published; refusing to mutate"))
        assertTrue(script.contains("Where-Object { ${'$'}_.name -like '*.apk' }"))
        assertTrue(script.contains("exactly one verified APK"))
        assertTrue(script.contains("browser_download_url"))
        assertTrue(script.contains("application/vnd.android.package-archive"))
        val verifiedDraftCheck = script.indexOf("if (-not [bool]${'$'}verified.draft")
        assertTrue(verifiedDraftCheck in 0 until script.indexOf("exactly one verified APK"))
        assertTrue(script.contains("${'$'}verified.tag_name -ne ${'$'}Tag"))
        assertTrue(script.lastIndexOf("draft = ${'$'}false") > script.indexOf("exactly one verified APK"))
        assertTrue(script.contains("${'$'}published.tag_name -ne ${'$'}Tag"))
    }

    @Test
    fun publisherHandlesTagLookupAndCreateRaceWithoutMutatingPublishedRelease() {
        val script = releaseScript()

        assertTrue(script.contains("Get-ReleaseByTag"))
        assertTrue(script.contains("if (${'$'}statusCode -eq 404)"))
        assertTrue(script.contains("if (${'$'}statusCode -ne 422)"))
        assertTrue(script.contains("Release appeared during draft creation"))
        assertTrue(script.contains("Update-DraftRelease"))
        assertTrue(script.contains("draft = ${'$'}true"))
    }

    @Test
    fun publisherDoesNotPrintAuthenticationToken() {
        val script = releaseScript()

        assertFalse(script.contains("Write-Host ${'$'}token"))
        assertFalse(script.contains("Write-Output ${'$'}token"))
        assertFalse(script.contains("Write-Verbose ${'$'}token"))
    }

    private fun releaseScript(): String {
        val file = listOf(
            File("scripts/publish-release.ps1"),
            File("../scripts/publish-release.ps1")
        ).firstOrNull(File::isFile) ?: error("publish-release.ps1 not found")
        return file.readText(Charsets.UTF_8)
    }
}
