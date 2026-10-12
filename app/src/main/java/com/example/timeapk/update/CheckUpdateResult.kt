package com.example.timeapk.update

/** Why an update check failed, so the UI can show a specific localized message. */
enum class UpdateFailureKind {
    /** Network or HTTP failure while contacting the release channel. */
    NETWORK,
    /** The release tag (or installed version) is not a recognizable version number. */
    VERSION_UNRECOGNIZED,
    /** The release JSON is malformed or the expected APK asset is missing/ambiguous. */
    INVALID_RELEASE
}

/**
 * 检查更新结果。设置页「检查更新」通过 [UpdateChecker] 获取。
 */
data class CheckUpdateResult(
    val hasUpdate: Boolean,
    val versionName: String? = null,
    val downloadUrl: String? = null,
    val releaseNotes: String? = null,
    val checkFailed: Boolean = false,
    val errorMessage: String? = null,
    val failureKind: UpdateFailureKind? = null,
    /** Human-facing release page (GitHub `html_url`), opened by "open in browser". */
    val releasePageUrl: String? = null
)
