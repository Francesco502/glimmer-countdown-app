package com.example.timeapk.ui.common

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EventImageImportTest {
    @Test
    fun boundsDecodeReturningNullStillImportsAndScalesARealImage() = withImageContext { context, root ->
        val source = File(root, "source.png")
        Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888).also { bitmap ->
            source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        assertNull(BitmapFactory.decodeFile(source.path, bounds))
        assertEquals(2000, bounds.outWidth)
        val name = runBlocking { EventImageStore.importFromUri(context, Uri.fromFile(source)) }
        assertNotNull(name)
        val imported = requireNotNull(EventImageStore.fileFor(context, requireNotNull(name)))
        BitmapFactory.decodeFile(imported.path).also { bitmap ->
            assertNotNull(bitmap)
            assertEquals(1600, bitmap.width)
            assertEquals(800, bitmap.height)
            bitmap.recycle()
        }
        assertTrue(imported.length() > 0)
        assertTrue(EventImageStore.directory(context).listFiles().orEmpty().none { it.name.endsWith(".tmp") })

        val invalid = File(root, "invalid.jpg").apply { writeText("Not an image") }
        assertNull(runBlocking { EventImageStore.importFromUri(context, Uri.fromFile(invalid)) })
        assertNull(runBlocking { EventImageStore.importFromUri(context, Uri.fromFile(File(root, "missing.jpg"))) })
        assertTrue(imported.exists())
    }

    @Test
    fun importedJpegRetainsExifRotation() = withImageContext { context, root ->
        val source = File(root, "rotated.jpg")
        Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888).also { bitmap ->
            source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            bitmap.recycle()
        }
        ExifInterface(source.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val name = runBlocking { EventImageStore.importFromUri(context, Uri.fromFile(source)) }
        assertNotNull(name)
        val file = requireNotNull(EventImageStore.fileFor(context, requireNotNull(name)))
        BitmapFactory.decodeFile(file.path).also { bitmap ->
            assertEquals(100, bitmap.width)
            assertEquals(200, bitmap.height)
            bitmap.recycle()
        }
    }

    @Test
    fun failedOutputDirectoryDoesNotLeaveAPartialImage() = withImageContext { context, root ->
        val source = File(root, "source.png")
        Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).also { bitmap ->
            source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
        val blockedDirectory = EventImageStore.directory(context).apply { writeText("blocked") }
        assertNull(runBlocking { EventImageStore.importFromUri(context, Uri.fromFile(source)) })
        assertEquals("blocked", blockedDirectory.readText())
    }

    @Test
    fun failedDurableRegistrationRemovesOnlyTheUnpublishedImport() = withImageContext { context, root ->
        val source = File(root, "source.png")
        Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).also { bitmap ->
            source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
        val original = File(EventImageStore.directory(context).apply { mkdirs() }, "a".repeat(32) + ".jpg")
            .apply { writeText("original must survive") }
        val blockedMetadata = File(root, "blocked-metadata").apply { writeText("unavailable") }
        val failingContext = object : ContextWrapper(context) {
            override fun getNoBackupFilesDir(): File = blockedMetadata
        }
        assertNull(runBlocking { EventImageStore.importFromUri(failingContext, Uri.fromFile(source)) })
        assertEquals("original must survive", original.readText())
        assertEquals(listOf(original.name), EventImageStore.directory(context).listFiles().orEmpty().map { it.name })
    }

    private fun withImageContext(block: (Context, File) -> Unit) {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = File(base.cacheDir, "image-import-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
            override fun getNoBackupFilesDir(): File = File(root, "no-backup").apply { mkdirs() }
        }
        try {
            block(context, root)
        } finally {
            root.deleteRecursively()
        }
    }
}
