package com.example.timeapk.update

import com.example.timeapk.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ReleaseFetchResult(
    val isSuccessful: Boolean,
    val responseBody: String? = null,
    val errorMessage: String? = null
)

private data class ParsedReleaseInfo(
    val tagName: String,
    val releaseNotes: String? = null,
    val assets: List<ReleaseAsset>
)

internal const val EXPECTED_DIRECT_APK_ERROR =
    "Expected Direct APK asset is missing or ambiguous"

/**
 * 通过 GitHub API 获取仓库最新 Release，与当前 [BuildConfig.VERSION_NAME] 比较，
 * 若有更新则返回版本号与 APK 下载链接，供设置页「检查更新」使用。
 */
class GitHubReleaseUpdateChecker(
    private val owner: String = "Francesco502",
    private val repo: String = "glimmer-countdown-app",
    private val client: OkHttpClient? = null,
    private val fetchRelease: (suspend (String) -> ReleaseFetchResult)? = null,
    private val installedVersionName: String = BuildConfig.VERSION_NAME
) : UpdateChecker {

    private val latestReleaseUrl
        get() = "https://api.github.com/repos/$owner/$repo/releases/latest"

    private val defaultClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun resolvedClient(): OkHttpClient = client ?: defaultClient

    override suspend fun checkUpdate(): CheckUpdateResult = withContext(Dispatchers.IO) {
        try {
            val release = if (fetchRelease != null) {
                fetchRelease.invoke(latestReleaseUrl)
            } else {
                val request = Request.Builder()
                    .url(latestReleaseUrl)
                    .get()
                    .build()
                resolvedClient().newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        ReleaseFetchResult(
                            isSuccessful = false,
                            errorMessage = "HTTP ${resp.code}"
                        )
                    } else {
                        ReleaseFetchResult(
                            isSuccessful = true,
                            responseBody = resp.body?.string()
                        )
                    }
                }
            }
            if (!release.isSuccessful || release.responseBody.isNullOrBlank()) {
                return@withContext CheckUpdateResult(
                    hasUpdate = false,
                    checkFailed = true,
                    errorMessage = release.errorMessage ?: "Release fetch failed"
                )
            }
            val releaseInfo = parseReleaseInfo(release.responseBody)
            val tagName = releaseInfo.tagName
            if (!isVersionNewer(tagName, installedVersionName)) {
                return@withContext CheckUpdateResult(hasUpdate = false)
            }
            val downloadUrl = selectDirectApkAsset(releaseInfo.tagName, releaseInfo.assets)?.downloadUrl
                ?: return@withContext CheckUpdateResult(
                    hasUpdate = false,
                    checkFailed = true,
                    errorMessage = EXPECTED_DIRECT_APK_ERROR
                )
            CheckUpdateResult(
                hasUpdate = true,
                versionName = tagName,
                downloadUrl = downloadUrl,
                releaseNotes = releaseInfo.releaseNotes
            )
        } catch (e: Exception) {
            CheckUpdateResult(
                hasUpdate = false,
                checkFailed = true,
                errorMessage = e.message
            )
        }
    }

    /**
     * 比较远程版本 remote 是否大于当前版本 current（如 "1.1" > "1.0"）。
     */
    private fun isVersionNewer(remote: String, current: String): Boolean {
        val r = parseVersionSegments(remote)
        val c = parseVersionSegments(normalizeReleaseVersion(current))
        for (i in 0 until maxOf(r.size, c.size)) {
            val rn = r.getOrElse(i) { 0 }
            val cn = c.getOrElse(i) { 0 }
            if (rn != cn) return rn > cn
        }
        return false
    }

    private fun parseVersionSegments(version: String): List<Int> =
        version.split(".").map(String::toInt)

    private fun parseReleaseInfo(responseBody: String): ParsedReleaseInfo {
        val json = JSONObject(responseBody)
        val rawTag = json.opt("tag_name")
        require(rawTag is String) { "Release tag must be a string" }
        val tagName = normalizeReleaseVersion(rawTag)
        val assetsJson = when {
            !json.has("assets") -> JSONArray()
            json.isNull("assets") -> throw IllegalArgumentException("Release assets must be an array")
            else -> json.optJSONArray("assets")
                ?: throw IllegalArgumentException("Release assets must be an array")
        }
        val assets = buildList {
            for (index in 0 until assetsJson.length()) {
                val asset = assetsJson.optJSONObject(index)
                    ?: throw IllegalArgumentException("Release asset at index $index must be an object")
                add(
                    ReleaseAsset(
                        name = asset.optString("name").trim(),
                        downloadUrl = asset.optString("browser_download_url").trim()
                    )
                )
            }
        }
        return ParsedReleaseInfo(
            tagName = tagName,
            releaseNotes = json.optString("body").trim().ifBlank { null },
            assets = assets
        )
    }
}
