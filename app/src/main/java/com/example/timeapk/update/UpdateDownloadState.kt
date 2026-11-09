package com.example.timeapk.update

/** UI-visible state of the in-app update download; it outlives the About screen. */
data class UpdateDownloadState(
    val downloading: Boolean = false,
    val progress: DownloadProgress? = null,
    val outcome: UpdateInstallOutcome? = null
)
