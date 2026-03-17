package com.example.timeapk.update

/**
 * 检查更新结果，便于后续接入真实 API 时统一使用。
 * 当前仅用于预留；设置页「检查更新」可先走 StubUpdateChecker，后续替换为 Play 或自建实现。
 */
data class CheckUpdateResult(
    val hasUpdate: Boolean,
    val versionName: String? = null,
    val downloadUrl: String? = null,
    val releaseNotes: String? = null,
    val checkFailed: Boolean = false,
    val errorMessage: String? = null
)
