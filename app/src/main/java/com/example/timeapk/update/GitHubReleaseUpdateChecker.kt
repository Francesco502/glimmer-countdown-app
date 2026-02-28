package com.example.timeapk.update

import com.example.timeapk.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 通过 GitHub API 获取仓库最新 Release，与当前 [BuildConfig.VERSION_NAME] 比较，
 * 若有更新则返回版本号与 APK 下载链接，供设置页「检查更新」使用。
 */
class GitHubReleaseUpdateChecker(
    private val owner: String = "Francesco502",
    private val repo: String = "glimmer-countdown-app",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : UpdateChecker {

    private val latestReleaseUrl
        get() = "https://api.github.com/repos/$owner/$repo/releases/latest"

    override suspend fun checkUpdate(): CheckUpdateResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(latestReleaseUrl)
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                return@withContext CheckUpdateResult(hasUpdate = false)
            }
            val json = JSONObject(response.body!!.string())
            val tagName = json.optString("tag_name", "").trim().removePrefix("v")
            val body = json.optString("body", "").trim()
            val assets = json.optJSONArray("assets") ?: return@withContext CheckUpdateResult(hasUpdate = false)
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url", "").takeIf { it.isNotBlank() }
                    break
                }
            }
            val currentVersion = BuildConfig.VERSION_NAME ?: "0"
            if (downloadUrl.isNullOrBlank() || !isVersionNewer(tagName, currentVersion)) {
                return@withContext CheckUpdateResult(hasUpdate = false)
            }
            CheckUpdateResult(
                hasUpdate = true,
                versionName = tagName,
                downloadUrl = downloadUrl,
                releaseNotes = body.ifBlank { null }
            )
        } catch (_: Exception) {
            CheckUpdateResult(hasUpdate = false)
        }
    }

    /**
     * 比较远程版本 remote 是否大于当前版本 current（如 "1.1" > "1.0"）。
     */
    private fun isVersionNewer(remote: String, current: String): Boolean {
        val r = parseVersionSegments(remote)
        val c = parseVersionSegments(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val rn = r.getOrElse(i) { 0 }
            val cn = c.getOrElse(i) { 0 }
            if (rn != cn) return rn > cn
        }
        return false
    }

    private fun parseVersionSegments(version: String): List<Int> =
        version.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
}
