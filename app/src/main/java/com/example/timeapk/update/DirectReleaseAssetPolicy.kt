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

internal fun selectDirectApkAsset(
    remoteVersion: String,
    assets: List<ReleaseAsset>
): ReleaseAsset? {
    val expectedName = expectedDirectApkName(remoteVersion)
    val exactMatches = assets.filter { it.name == expectedName }
    return exactMatches.singleOrNull()?.takeIf { it.downloadUrl.isNotBlank() }
}
