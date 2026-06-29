package com.example.timeapk.update

import com.example.timeapk.BuildConfig

object UpdateCheckerFactory {
    fun create(): UpdateChecker {
        return if (BuildConfig.DIRECT_APK_UPDATES_ENABLED) {
            GitHubReleaseUpdateChecker()
        } else {
            StubUpdateChecker()
        }
    }
}
