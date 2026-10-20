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
import kotlinx.coroutines.runBlocking
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
    private val imageRegistries = mutableMapOf<String, EventImageRegistry>()

    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun isValidFileName(name: String?): Boolean = EventImageRegistry.validName(name)

    fun directory(context: Context): File = File(context.filesDir, DIRECTORY)

    fun fileFor(context: Context, name: String): File? {
        if (!isValidFileName(name)) return null
        return runCatching {
            val root = directory(context).canonicalFile
            File(root, name).canonicalFile.takeIf { it.parentFile == root && it.name == name }
        }.getOrNull()
    }

    @Synchronized
    private fun registry(context: Context): EventImageRegistry = imageRegistries.getOrPut(context.noBackupFilesDir.canonicalPath) {
        EventImageRegistry(File(context.noBackupFilesDir, "event-image-lifecycle.properties"), directory(context))
    }

    internal fun replaceDraftReferences(context: Context, owner: String, names: Set<String>): Boolean =
        runCatching { registry(context).replaceReferences(owner, names.filterTo(mutableSetOf(), ::isValidFileName)) }.getOrDefault(false)

    internal fun retainDraftReferences(context: Context, owner: String, names: Set<String>): Boolean =
        runCatching { registry(context).retainDraftReferences(owner, names.filterTo(mutableSetOf(), ::isValidFileName)) }.getOrDefault(false)

    internal fun registerCleanupCandidate(context: Context, name: String?): Boolean =
        isValidFileName(name) && runCatching {
            registry(context).registerCandidate(requireNotNull(name))
        }.getOrDefault(false)

    internal fun releasePreviousUndoReferences(context: Context) {
        runCatching { registry(context).releaseOwnersWithPrefix("undo-") }
    }

    internal suspend fun cleanupRegisteredImages(context: Context, referencedFiles: suspend () -> Set<String>): Boolean =
        withContext(Dispatchers.IO) {
            // Serialize reference release with the latest DB snapshot and deletion on an IO thread.
            runCatching { registry(context).cleanup { runBlocking { referencedFiles() } } }.getOrDefault(false).also {
                cache.evictAll()
            }
        }

    /** 读取并压缩所选图片，返回新文件名；失败返回 null。 */
    suspend fun importFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        var decoded: Bitmap? = null
        var scaled: Bitmap? = null
        var temp: File? = null
        var unpublishedTarget: File? = null
        var imported = false
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = resolver.openInputStream(uri) ?: return@withContext null
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, EVENT_IMAGE_MAX_EDGE_PX)
            }
            val source = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext null
            decoded = source
            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val result = scaleAndRotate(source, EVENT_IMAGE_MAX_EDGE_PX, orientation)
            scaled = result
            val dir = directory(context)
            check(dir.isDirectory || dir.mkdirs())
            val name = UUID.randomUUID().toString().replace("-", "") + ".jpg"
            val target = File(dir, name)
            check(!target.exists())
            val pending = File(dir, "$name.tmp")
            temp = pending
            pending.outputStream().use { out ->
                check(result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out))
                out.fd.sync()
            }
            if (!pending.renameTo(target)) {
                return@withContext null
            }
            unpublishedTarget = target
            check(registerCleanupCandidate(context, name))
            imported = true
            name
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            if (!imported) unpublishedTarget?.delete()
            temp?.delete()
            if (scaled !== decoded) scaled?.recycle()
            decoded?.recycle()
        }
    }

    suspend fun delete(context: Context, name: String?) = withContext(Dispatchers.IO) {
        if (name == null) return@withContext
        cache.evictAll()
        fileFor(context, name)?.delete()
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
