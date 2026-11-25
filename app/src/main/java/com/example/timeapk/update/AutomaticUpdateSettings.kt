package com.example.timeapk.update

private const val UPDATE_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
internal const val UPDATE_POSTPONE_MS = 3 * UPDATE_CHECK_INTERVAL_MS

internal data class AutomaticUpdateSettings(
    val enabled: Boolean = true,
    val lastCheckedAt: Long = 0,
    val skippedVersion: String? = null,
    val postponedUntil: Long = 0
) {
    fun canCheck(now: Long): Boolean =
        enabled && (lastCheckedAt == 0L || now < lastCheckedAt || now - lastCheckedAt >= UPDATE_CHECK_INTERVAL_MS)

    fun shouldPrompt(result: CheckUpdateResult?, now: Long): Boolean =
        enabled && result?.hasUpdate == true && !result.checkFailed &&
            result.versionName != null && result.downloadUrl != null &&
            result.versionName != skippedVersion && now >= postponedUntil
}
