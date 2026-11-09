package com.example.timeapk.update

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Download progress; [totalBytes] is -1 when the server sent no Content-Length. */
data class DownloadProgress(
    val bytesRead: Long,
    val totalBytes: Long
) {
    /** 0..1 when the length is known, null for an indeterminate download. */
    val fraction: Float?
        get() = if (totalBytes > 0L) (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f) else null
}

/** What happened after the user asked to download and install an update. */
enum class UpdateInstallOutcome {
    /** The system installer was launched. */
    INSTALL_REQUESTED,
    /** Networking, IO or the download itself failed. */
    DOWNLOAD_FAILED,
    /** The downloaded bytes do not match the release asset digest. */
    CHECKSUM_MISMATCH,
    /** The APK is downloaded and verified but the app is in the background (install must wait). */
    NEEDS_FOREGROUND
}

internal class UpdateChecksumException(
    val expected: String,
    val actual: String
) : Exception("Update checksum mismatch")

/**
 * 将 APK 从 [downloadUrl] 下载到应用私有目录，校验发布方提供的 SHA-256 后调起系统安装界面。
 * 需配合 AndroidManifest 中的 FileProvider 与 REQUEST_INSTALL_PACKAGES 使用。
 */
object UpdateInstaller {

    private val downloadMutex = Mutex()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 下载并安装。返回结果而不是布尔值，让界面能区分「校验失败」「需要回到前台」「下载失败」。
     */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        expectedSha256: String? = null,
        onProgress: (DownloadProgress) -> Unit = {}
    ): UpdateInstallOutcome = withContext(Dispatchers.IO) {
        if (!isTrustedAssetUrl(downloadUrl)) return@withContext UpdateInstallOutcome.DOWNLOAD_FAILED
        if (!downloadMutex.tryLock()) return@withContext UpdateInstallOutcome.DOWNLOAD_FAILED
        try {
            val dir = File(context.filesDir, "updates")
            check(dir.isDirectory || dir.mkdirs()) { "Could not create update directory" }
            val apkFile = File(dir, "update.apk")
            val request = Request.Builder().url(downloadUrl).get().build()
            downloadApk(client.newCall(request), apkFile, expectedSha256, onProgress)
            if (!isAppInForeground(context)) {
                // Keep the verified APK; MainActivity installs it when the app returns to front.
                return@withContext UpdateInstallOutcome.NEEDS_FOREGROUND
            }
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            UpdateInstallOutcome.INSTALL_REQUESTED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: UpdateChecksumException) {
            UpdateInstallOutcome.CHECKSUM_MISMATCH
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            UpdateInstallOutcome.DOWNLOAD_FAILED
        } finally {
            downloadMutex.unlock()
        }
    }

    /** Installs an already downloaded and verified APK. Returns true when the installer opened. */
    fun installDownloadedApk(context: Context): Boolean {
        val apkFile = File(File(context.filesDir, "updates"), "update.apk")
        if (!apkFile.isFile || apkFile.length() <= 0L) return false
        if (!isAppInForeground(context)) return false
        return runCatching { installApk(context, apkFile) }.isSuccess
    }

    fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return false
        val importance = activityManager.runningAppProcesses
            ?.firstOrNull { it.pid == Process.myPid() }
            ?.importance
        return importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    /** Whether the system will let this app launch the package installer (minSdk is 26). */
    fun canRequestPackageInstalls(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Opens the system "install unknown apps" page for this package. */
    fun openUnknownAppSourcesSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * 在浏览器中打开 Release 页面（GitHub html_url），用户可查看说明并手动下载 APK。
     */
    fun openReleasePageInBrowser(context: Context, releasePageUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, releasePageUrl.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(intent, null))
    }
}

internal suspend fun downloadApk(
    call: Call,
    apkFile: File,
    expectedSha256: String? = null,
    onProgress: (DownloadProgress) -> Unit = {}
): String = coroutineScope {
    // Start before execute/read can block so page cancellation closes the socket too.
    val cancellationWatcher = launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            call.cancel()
        }
    }
    try {
        currentCoroutineContext().ensureActive()
        call.execute().use { response ->
            currentCoroutineContext().ensureActive()
            check(response.code == 200) { "Update download returned HTTP ${response.code}" }
            val body = response.body
            body.byteStream().use { input ->
                writeDownloadedApk(
                    input = input,
                    apkFile = apkFile,
                    expectedLength = body.contentLength(),
                    expectedSha256 = expectedSha256,
                    onProgress = onProgress
                )
            }
        }
    } catch (failure: Exception) {
        currentCoroutineContext().ensureActive()
        throw failure
    } finally {
        cancellationWatcher.cancel()
    }
}

internal suspend fun writeDownloadedApk(
    input: InputStream,
    apkFile: File,
    expectedLength: Long,
    expectedSha256: String? = null,
    onProgress: (DownloadProgress) -> Unit = {}
): String {
    val partialFile = File(apkFile.parentFile, "${apkFile.name}.part")
    val digest = MessageDigest.getInstance("SHA-256")
    try {
        require(expectedLength >= -1L) { "Invalid update response length" }
        require(expectedLength != 0L) { "Empty update response" }
        var copied = 0L
        var lastReported = -1L
        onProgress(DownloadProgress(0L, expectedLength))
        partialFile.outputStream().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                digest.update(buffer, 0, read)
                copied += read
                require(expectedLength < 0 || copied <= expectedLength) { "Update response exceeds its length" }
                // Throttle to roughly every 64 KiB so the UI is not flooded with recompositions.
                if (copied - lastReported >= PROGRESS_REPORT_STEP_BYTES || copied == expectedLength) {
                    lastReported = copied
                    onProgress(DownloadProgress(copied, expectedLength))
                }
            }
            require(copied > 0) { "Empty update response" }
            require(expectedLength < 0 || copied == expectedLength) { "Incomplete update response" }
            output.fd.sync()
        }
        currentCoroutineContext().ensureActive()
        val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
        if (expectedSha256 != null && !expectedSha256.equals(actualSha256, ignoreCase = true)) {
            // Verified before the atomic move, so a bad download never replaces a good APK.
            throw UpdateChecksumException(expectedSha256, actualSha256)
        }
        Files.move(
            partialFile.toPath(),
            apkFile.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
        )
        return actualSha256
    } finally {
        partialFile.delete()
    }
}

private const val PROGRESS_REPORT_STEP_BYTES = 64L * 1024L
