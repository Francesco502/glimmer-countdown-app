package com.example.timeapk.update

internal data class ReleaseAsset(
    val name: String,
    val downloadUrl: String
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
    return exactMatches.singleOrNull()?.takeIf { it.downloadUrl.isNotBlank() }
}
