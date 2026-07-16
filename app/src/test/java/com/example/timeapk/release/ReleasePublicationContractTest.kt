package com.example.timeapk.release

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleasePublicationContractTest {
    @Test
    fun publisherCompletesLocalAndRemoteTagPreflightBeforeLockMutation() {
        val script = releaseScript()
        val lockMutation = lastWholeLineOffset(script, "New-ReleaseLock")

        assertTrue(lockMutation > 0)
        listOf(
            "Missing changelog section",
            "GLIMMER_RELEASE_CERT_SHA256 is required",
            "Invalid GLIMMER_RELEASE_CERT_SHA256",
            "APK not found or empty",
            "& ${'$'}apksigner verify --print-certs",
            "rev-list -n 1",
            "/commits/",
            "Remote tag commit does not match the local tag commit",
            "Get-FileHash",
            "Unable to resolve GitHub authentication token",
        ).forEach { marker ->
            assertTrue("$marker must precede the lock mutation call", script.indexOf(marker) in 0 until lockMutation)
        }
    }

    @Test
    fun publisherUsesAnExclusiveTagSpecificServerLockWithFinallyCleanup() {
        val script = releaseScript()

        assertTrue(script.contains("refs/heads/release-locks/${'$'}Tag"))
        assertTrue(script.contains("function New-ReleaseLock"))
        assertTrue(script.contains("if (${'$'}statusCode -in @(409, 422))"))
        assertTrue(script.contains("Another publisher owns the release lock"))
        assertTrue(script.contains("try {"))
        assertTrue(script.contains("} finally {"))
        assertTrue(script.contains("Remove-ReleaseLock"))
        assertTrue(script.contains("refusing to take over or mutate it"))
        assertTrue(script.contains("Get-ResumableDraftRelease"))
        assertTrue(script.contains("Existing draft ${'$'}Tag is not owned by this publisher"))

        val lockCall = lastWholeLineOffset(script, "New-ReleaseLock")
        val publicationTry = script.indexOf("try {", startIndex = lockCall)
        val publicationFinally = script.indexOf("} finally {", startIndex = publicationTry)
        val lockCleanup = script.indexOf("Remove-ReleaseLock", startIndex = publicationFinally)
        assertTrue(lockCall < publicationTry)
        assertTrue(publicationTry < publicationFinally)
        assertTrue(publicationFinally < lockCleanup)
    }

    @Test
    fun everyReleaseMutationIsGuardedByOwnedDraftValidation() {
        val script = releaseScript()

        assertTrue(script.contains("function Get-OwnedDraftRelease"))
        assertTrue(script.contains("Release ownership marker does not match"))
        assertTrue(script.contains("Release id changed during publication"))
        assertTrue(script.contains("Release tag changed during publication"))
        assertTrue(script.contains("Release is no longer the owned draft"))
        assertTrue(script.contains("${'$'}current = Get-OwnedDraftRelease", ignoreCase = false))
        assertTrue(script.contains("-ExpectedReleaseId ${'$'}releaseId"))

        val upload = script.lastIndexOf("${'$'}uploadResult = Upload-ReleaseAsset")
        val publish = script.lastIndexOf("${'$'}publishPatchResponse = Invoke-RestMethod")
        assertTrue(script.lastIndexOf("${'$'}current = Get-OwnedDraftRelease", upload) in 0 until upload)
        assertTrue(script.lastIndexOf("${'$'}current = Get-OwnedDraftRelease", publish) in 0 until publish)
    }

    @Test
    fun publisherBindsUploadResponseAndRefetchToExactLocalArtifact() {
        val script = releaseScript()

        listOf(
            "Upload response asset name does not match exactly",
            "Upload response asset id is invalid",
            "Upload response state is not uploaded",
            "Upload response content type does not match",
            "Upload response size does not match the local APK",
            "Upload response digest does not match the local APK",
            "Refetched asset id does not match the upload response",
            "Refetched asset URL does not match the expected repository release URL",
            "Refetched asset digest does not match the local APK",
        ).forEach { marker -> assertTrue("missing $marker", script.contains(marker)) }

        assertTrue(script.contains("[System.StringComparison]::Ordinal"))
        assertTrue(script.contains("sha256:${'$'}apkSha256"))
        assertTrue(script.contains("${'$'}uploadResult = Upload-ReleaseAsset"))
        assertTrue(script.contains("${'$'}uploadedAssetId"))
        assertTrue(script.contains("https://uploads.github.com/repos/${'$'}owner/${'$'}repo/releases/"))
        assertTrue(script.contains("Release upload URL is not the expected GitHub uploads endpoint for this release id"))
    }

    @Test
    fun publisherSelectsOnlyStablePlatformApkSignerBuildTools() {
        val script = releaseScript()

        assertTrue(script.contains("[version]::TryParse"))
        assertTrue(script.contains("apksigner.bat"))
        assertTrue(script.contains("Test-Path ${'$'}candidate -PathType Leaf"))
        assertFalse(script.contains("[version]${'$'}_.Directory.Name"))
        assertFalse(script.contains("Get-ChildItem ${'$'}buildTools -Recurse"))
    }

    @Test
    fun finalGetIsAuthoritativeEvenWhenPublishPatchIsAmbiguous() {
        val script = releaseScript()

        assertTrue(script.contains("${'$'}publishPatchError"))
        assertTrue(script.contains("Get-FinalPublishedRelease"))
        assertTrue(script.contains("Final release id does not match"))
        assertTrue(script.contains("Final release name does not match"))
        assertTrue(script.contains("Final release is not public and stable"))
        assertTrue(script.contains("Final release URL does not match"))
        assertTrue(script.contains("-Release ${'$'}publishPatchResponse"))
        assertTrue(script.contains("-Release ${'$'}final"))
        assertTrue(script.contains("Published verified release ${'$'}Tag"))
        assertTrue(script.contains("Write-Host ${'$'}finalRelease.html_url"))
        assertTrue(script.indexOf("Get-FinalPublishedRelease") < script.lastIndexOf("Published verified release"))
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
            File("../scripts/publish-release.ps1"),
        ).firstOrNull(File::isFile) ?: error("publish-release.ps1 not found")
        return file.readText(Charsets.UTF_8)
    }

    private fun lastWholeLineOffset(source: String, line: String): Int =
        Regex("(?m)^${Regex.escape(line)}\\r?${'$'}")
            .findAll(source)
            .lastOrNull()
            ?.range
            ?.first
            ?: -1
}
