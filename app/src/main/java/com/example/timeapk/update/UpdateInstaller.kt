package com.example.timeapk.update

import android.content.Context
import android.content.Intent
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
import java.util.concurrent.TimeUnit

/**
 * 将 APK 从 [downloadUrl] 下载到应用私有目录，并调起系统安装界面。
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
     * 下载 APK 到 context.filesDir/updates/update.apk，并启动安装界面。
     * @return 成功返回 true，失败返回 false（如网络错误、无安装权限等）。
     */
    suspend fun downloadAndInstall(context: Context, downloadUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (!downloadMutex.tryLock()) return@withContext false
        try {
            val dir = File(context.filesDir, "updates")
            check(dir.isDirectory || dir.mkdirs()) { "Could not create update directory" }
            val apkFile = File(dir, "update.apk")
            val request = Request.Builder().url(downloadUrl).get().build()
            downloadApk(client.newCall(request), apkFile)
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            false
        } finally {
            downloadMutex.unlock()
        }
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

    /**
     * 在浏览器中打开下载页（如 GitHub Release 页），用户可手动下载 APK 后安装。
     */
    fun openDownloadPageInBrowser(context: Context, downloadUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, downloadUrl.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(intent, null))
    }
}

internal suspend fun downloadApk(call: Call, apkFile: File) = coroutineScope {
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
            response.body.byteStream().use { input ->
                writeDownloadedApk(input, apkFile, response.body.contentLength())
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
    expectedLength: Long
) {
    val partialFile = File(apkFile.parentFile, "${apkFile.name}.part")
    try {
        require(expectedLength >= -1L) { "Invalid update response length" }
        require(expectedLength != 0L) { "Empty update response" }
        var copied = 0L
        partialFile.outputStream().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
                copied += read
                require(expectedLength < 0 || copied <= expectedLength) { "Update response exceeds its length" }
            }
            require(copied > 0) { "Empty update response" }
            require(expectedLength < 0 || copied == expectedLength) { "Incomplete update response" }
            output.fd.sync()
        }
        currentCoroutineContext().ensureActive()
        Files.move(
            partialFile.toPath(),
            apkFile.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
        )
    } finally {
        partialFile.delete()
    }
}
