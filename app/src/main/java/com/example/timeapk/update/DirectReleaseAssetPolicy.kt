package com.example.timeapk.update

internal data class ReleaseAsset(
    val name: String,
    val downloadUrl: String
)

internal fun expectedDirectApkName(remoteVersion: String): String {
    val cleanVersion = remoteVersion.trim().removePrefix("v")
    return "glimmer-countdown-${cleanVersion.replace('.', '-')}.apk"
}

internal fun selectDirectApkAsset(
    remoteVersion: String,
    assets: List<ReleaseAsset>
): ReleaseAsset? {
    val expectedName = expectedDirectApkName(remoteVersion)
    val exactMatches = assets.filter { it.name == expectedName }
    return exactMatches.singleOrNull()?.takeIf { it.downloadUrl.isNotBlank() }
}
