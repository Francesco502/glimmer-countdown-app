package com.example.timeapk.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 将 APK 从 [downloadUrl] 下载到应用私有目录，并调起系统安装界面。
 * 需配合 AndroidManifest 中的 FileProvider 与 REQUEST_INSTALL_PACKAGES 使用。
 */
object UpdateInstaller {

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
        val dir = File(context.filesDir, "updates").apply { mkdirs() }
        val apkFile = File(dir, "update.apk")
        try {
            val request = Request.Builder().url(downloadUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                response.body.byteStream().use { input ->
                    apkFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            true
        } catch (_: Exception) {
            false
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
