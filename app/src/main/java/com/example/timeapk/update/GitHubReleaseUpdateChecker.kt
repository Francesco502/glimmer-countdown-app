package com.example.timeapk.update

import com.example.timeapk.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    val version: ReleaseVersion?,
    val releaseNotes: String? = null,
    val releasePageUrl: String? = null,
    val assets: List<ReleaseAsset>
)

internal const val EXPECTED_DIRECT_APK_ERROR =
    "Expected Direct APK asset is missing or ambiguous"

internal const val UNRECOGNIZED_VERSION_ERROR =
    "Release version format is not recognized"

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

    private val releasesPageUrl
        get() = "https://github.com/$owner/$repo/releases/latest"

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
                    errorMessage = release.errorMessage ?: "Release fetch failed",
                    failureKind = UpdateFailureKind.NETWORK
                )
            }
            val releaseInfo = parseReleaseInfo(release.responseBody)
            val remoteVersion = releaseInfo.version
            val installedVersion = parseReleaseVersion(installedVersionName)
            if (remoteVersion == null || installedVersion == null) {
                return@withContext CheckUpdateResult(
                    hasUpdate = false,
                    checkFailed = true,
                    errorMessage = UNRECOGNIZED_VERSION_ERROR,
                    failureKind = UpdateFailureKind.VERSION_UNRECOGNIZED
                )
            }
            if (remoteVersion <= installedVersion) {
                return@withContext CheckUpdateResult(hasUpdate = false)
            }
            val downloadUrl = selectDirectApkAsset(remoteVersion, releaseInfo.assets)?.downloadUrl
                ?: return@withContext CheckUpdateResult(
                    hasUpdate = false,
                    checkFailed = true,
                    errorMessage = EXPECTED_DIRECT_APK_ERROR,
                    failureKind = UpdateFailureKind.INVALID_RELEASE
                )
            CheckUpdateResult(
                hasUpdate = true,
                versionName = remoteVersion.displayName,
                downloadUrl = downloadUrl,
                releaseNotes = releaseInfo.releaseNotes,
                releasePageUrl = releaseInfo.releasePageUrl ?: releasesPageUrl
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            CheckUpdateResult(
                hasUpdate = false,
                checkFailed = true,
                errorMessage = e.message ?: e.javaClass.simpleName,
                failureKind = if (e is java.io.IOException) {
                    UpdateFailureKind.NETWORK
                } else {
                    UpdateFailureKind.INVALID_RELEASE
                }
            )
        }
    }

    private fun parseReleaseInfo(responseBody: String): ParsedReleaseInfo {
        val json = JSONObject(responseBody)
        val rawTag = json.opt("tag_name")
        require(rawTag is String) { "Release tag must be a string" }
        // Unparseable tags become an explicit VERSION_UNRECOGNIZED result, never an exception.
        val version = parseReleaseVersion(rawTag)
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
            version = version,
            releaseNotes = json.optString("body").trim().ifBlank { null },
            releasePageUrl = parseReleasePageUrl(json),
            assets = assets
        )
    }

    /** Only an https GitHub page is accepted for the browser; anything else falls back. */
    private fun parseReleasePageUrl(json: JSONObject): String? {
        val raw = json.opt("html_url") as? String ?: return null
        val url = raw.trim()
        return url.takeIf { it.startsWith("https://github.com/") }
    }
}
