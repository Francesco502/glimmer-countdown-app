package com.example.timeapk.release

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GithubPublisherPreflightContractTest {
    @Test
    fun publisherRejectsDirtyOrUntaggedHeadBeforeAnyRemoteRequest() {
        val script = releaseScript()
        val initialProvenanceCheck = script.indexOf("${'$'}localTagCommit = Assert-ReleaseSourceProvenance")
        val firstRemoteRequest = script.indexOf("${'$'}remoteTagRef = Invoke-RestMethod")
        val finalProvenanceCheck = script.indexOf(
            "${'$'}null = Assert-ReleaseSourceProvenance",
            startIndex = firstRemoteRequest,
        )
        val firstRemoteMutation = script.indexOf("${'$'}releaseLockObjectSha = New-ReleaseLock")

        assertTrue(initialProvenanceCheck in 1 until firstRemoteRequest)
        assertTrue(finalProvenanceCheck in (firstRemoteRequest + 1) until firstRemoteMutation)
        listOf(
            "status --porcelain=v1 --untracked-files=all",
            "Release worktree must be clean; tracked or untracked changes were found.",
            "rev-parse --verify 'HEAD^{commit}'",
            "HEAD commit does not match the exact local tag commit.",
        ).forEach { marker ->
            val offset = script.indexOf(marker)
            assertTrue("$marker must precede the first remote request", offset in 0 until firstRemoteRequest)
        }
    }

    @Test
    fun publisherBindsSingleDirectMetadataArtifactToGradleVersionAndApkName() {
        val script = releaseScript()

        listOf(
            "output-metadata.json",
            "Direct release metadata must declare applicationId com.example.timeapk.",
            "Direct release metadata must declare variantName directRelease.",
            "Direct release metadata must contain exactly one artifact.",
            "Direct release metadata versionCode does not match VERSION_CODE.",
            "Direct release metadata versionName does not match VERSION_NAME.",
            "Direct release metadata outputFile does not match the exact GitHub APK name.",
        ).forEach { marker -> assertTrue("missing metadata preflight: $marker", script.contains(marker)) }
    }

    @Test
    fun publisherUsesStableAaptToVerifyTheActualDirectApkIdentity() {
        val script = releaseScript()

        listOf(
            "function Find-Aapt",
            "aapt.exe",
            "dump badging",
            "APK package name does not match com.example.timeapk.",
            "APK versionCode does not match VERSION_CODE.",
            "APK versionName does not match VERSION_NAME.",
            "Direct APK must not be debuggable.",
            "Direct APK must declare android.permission.REQUEST_INSTALL_PACKAGES.",
        ).forEach { marker -> assertTrue("missing APK identity preflight: $marker", script.contains(marker)) }
    }

    @Test
    fun mockHarnessExercisesLocalFailuresWithoutAnyGithubRequest() {
        val harness = mockHarness()

        listOf(
            "dirty-worktree",
            "untracked-worktree",
            "head-tag-mismatch",
            "metadata-invalid",
            "apk-identity-invalid",
            "Assert-NoRemoteCalls",
        ).forEach { marker -> assertTrue("missing publisher mock coverage: $marker", harness.contains(marker)) }
    }

    private fun releaseScript(): String = existingFile(
        "scripts/publish-release.ps1",
        "../scripts/publish-release.ps1",
    ).readText(Charsets.UTF_8)

    private fun mockHarness(): String = existingFile(
        "scripts/tests/publish-release-mock-harness.ps1",
        "../scripts/tests/publish-release-mock-harness.ps1",
    ).readText(Charsets.UTF_8)

    private fun existingFile(vararg paths: String): File =
        paths.asSequence().map(::File).firstOrNull(File::isFile)
            ?: error("Missing file; tried ${paths.joinToString()}")
}
