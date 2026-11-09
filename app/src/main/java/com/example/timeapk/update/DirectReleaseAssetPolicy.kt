package com.example.timeapk.update

internal data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    /** GitHub asset digest, e.g. "sha256:5aa396a3…"; null for older API responses. */
    val digest: String? = null
)

private val RELEASE_VERSION_PATTERN = Regex("^\\d+(?:\\.\\d+)*$")

internal fun normalizeReleaseVersion(remoteVersion: String): String {
    val versionWithoutPrefix = remoteVersion.trim().removePrefix("v")
    require(RELEASE_VERSION_PATTERN.matches(versionWithoutPrefix)) {
        "Invalid release version: $remoteVersion"
    }
    val segments = versionWithoutPrefix.split('.').map { segment ->
        segment.toIntOrNull()
            ?: throw IllegalArgumentException("Release version segment is out of range: $segment")
    }
    return segments.joinToString(".")
}

internal fun expectedDirectApkName(remoteVersion: String): String {
    val normalizedVersion = normalizeReleaseVersion(remoteVersion)
    return "glimmer-countdown-${normalizedVersion.replace('.', '-')}.apk"
}

/** Asset name for a parsed (possibly prerelease) version, mirroring the build's apkBaseName. */
internal fun expectedDirectApkName(version: ReleaseVersion): String =
    "glimmer-countdown-${version.displayName.replace('.', '-')}.apk"

internal fun selectDirectApkAsset(
    remoteVersion: String,
    assets: List<ReleaseAsset>
): ReleaseAsset? = selectDirectApkAssetNamed(expectedDirectApkName(remoteVersion), assets)

internal fun selectDirectApkAsset(
    version: ReleaseVersion,
    assets: List<ReleaseAsset>
): ReleaseAsset? = selectDirectApkAssetNamed(expectedDirectApkName(version), assets)

private fun selectDirectApkAssetNamed(
    expectedName: String,
    assets: List<ReleaseAsset>
): ReleaseAsset? {
    val exactMatches = assets.filter { it.name == expectedName }
    return exactMatches.singleOrNull()?.takeIf { isTrustedAssetUrl(it.downloadUrl) }
}

/** The updater only accepts the exact GitHub release-download origin it publishes to. */
internal fun isTrustedAssetUrl(url: String): Boolean =
    url.startsWith("https://github.com/", ignoreCase = true) &&
        url.substringBefore('?').endsWith(".apk", ignoreCase = true)

/** Extracts the hex part of a GitHub `digest` field ("sha256:<hex>"). */
internal fun parseSha256Digest(digest: String?): String? {
    val value = digest?.trim().orEmpty()
    if (!value.startsWith("sha256:", ignoreCase = true)) return null
    val hex = value.substringAfter(':').trim().lowercase()
    return hex.takeIf { it.length == 64 && it.all { ch -> ch in "0123456789abcdef" } }
}
