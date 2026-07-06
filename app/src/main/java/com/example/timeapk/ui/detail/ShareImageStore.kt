package com.example.timeapk.ui.detail

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareImageStore {
    private const val MIME_TYPE_PNG = "image/png"
    private const val SHARE_DIR = "share/"

    fun cacheShareImage(
        context: Context,
        bitmap: Bitmap,
        displayName: String
    ): Uri {
        val shareDir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        val imageFile = File(shareDir, displayName)
        FileOutputStream(imageFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    fun saveShareImage(
        context: Context,
        bitmap: Bitmap,
        displayName: String
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, bitmap, displayName)
        } else {
            saveWithExternalPicturesDir(context, bitmap, displayName)
        }
    }

    fun shareImage(
        context: Context,
        imageUri: Uri,
        chooserTitle: String
    ) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE_PNG
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
    }

    fun shareImageName(title: String): String {
        val safeTitle = title
            .replace(Regex("[^A-Za-z0-9\\u4e00-\\u9fa5_-]+"), "-")
            .trim('-')
            .take(28)
            .ifBlank { "event" }
        return "timeapk-$safeTitle.png"
    }

    private fun saveWithMediaStore(
        context: Context,
        bitmap: Bitmap,
        displayName: String
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_PNG)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TimeAPK")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            } ?: return null
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun saveWithExternalPicturesDir(
        context: Context,
        bitmap: Bitmap,
        displayName: String
    ): Uri? {
        val picturesDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "TimeAPK"
        ).apply { mkdirs() }
        val imageFile = File(picturesDir, displayName)
        return try {
            FileOutputStream(imageFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(imageFile.absolutePath),
                arrayOf(MIME_TYPE_PNG),
                null
            )
            Uri.fromFile(imageFile)
        } catch (_: Exception) {
            null
        }
    }
}
