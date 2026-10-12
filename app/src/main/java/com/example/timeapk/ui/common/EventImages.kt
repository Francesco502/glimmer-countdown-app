package com.example.timeapk.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 事件配图保存在应用私有目录 files/event_images/，数据库只记录文件名。
 * 选图后按长边 [EVENT_IMAGE_MAX_EDGE_PX] 压缩为 JPEG，不需要存储权限。
 */
object EventImageStore {
    const val DIRECTORY = "event_images"
    const val EVENT_IMAGE_MAX_EDGE_PX = 1600
    private const val JPEG_QUALITY = 86
    private val FILE_NAME_PATTERN = Regex("^[0-9a-f]{32}\\.jpg$")

    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun isValidFileName(name: String?): Boolean = name != null && FILE_NAME_PATTERN.matches(name)

    fun directory(context: Context): File = File(context.filesDir, DIRECTORY)

    fun fileFor(context: Context, name: String): File? {
        if (!isValidFileName(name)) return null
        return File(directory(context), name)
    }

    /** 读取并压缩所选图片，返回新文件名；失败返回 null。 */
    suspend fun importFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return@withContext null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, EVENT_IMAGE_MAX_EDGE_PX)
            }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext null
            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val scaled = scaleAndRotate(decoded, EVENT_IMAGE_MAX_EDGE_PX, orientation)
            val dir = directory(context).apply { mkdirs() }
            val name = UUID.randomUUID().toString().replace("-", "") + ".jpg"
            val target = File(dir, name)
            val temp = File(dir, "$name.tmp")
            temp.outputStream().use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
            if (!temp.renameTo(target)) {
                temp.delete()
                return@withContext null
            }
            name
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    suspend fun delete(context: Context, name: String?) = withContext(Dispatchers.IO) {
        if (name == null) return@withContext
        cache.evictAll()
        fileFor(context, name)?.delete()
    }

    /** 删除不再被任何事件引用的配图（撤销删除窗口过后才清理，故只处理一天前的文件）。 */
    suspend fun cleanupOrphans(context: Context, referenced: Set<String>, nowMillis: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            directory(context).listFiles()?.forEach { file ->
                val stale = nowMillis - file.lastModified() > 24L * 60 * 60 * 1000
                if (stale && file.name !in referenced) file.delete()
            }
        }

    suspend fun loadBitmap(context: Context, name: String, maxEdgePx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$name@$maxEdgePx"
        cache.get(key)?.let { return@withContext it }
        val file = fileFor(context, name) ?: return@withContext null
        if (!file.exists()) return@withContext null
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdgePx)
            }
            BitmapFactory.decodeFile(file.path, options)?.also { cache.put(key, it) }
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    internal fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= maxEdge) {
            sample *= 2
            longest /= 2
        }
        return sample
    }

    private fun scaleAndRotate(source: Bitmap, maxEdge: Int, orientation: Int): Bitmap {
        val matrix = Matrix()
        val longest = maxOf(source.width, source.height)
        if (longest > maxEdge) {
            val factor = maxEdge.toFloat() / longest
            matrix.postScale(factor, factor)
        }
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        if (matrix.isIdentity) return source
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}

/** 异步加载事件配图；无配图或失败时为 null。 */
@Composable
fun rememberEventImage(name: String?, maxEdgePx: Int): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name, maxEdgePx) {
        value = if (name == null) null else EventImageStore.loadBitmap(context, name, maxEdgePx)?.asImageBitmap()
    }
    return bitmap
}
