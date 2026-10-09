package com.example.timeapk.ui.detail

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ProviderInfo
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import com.example.timeapk.data.Event
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.ui.home.EventUiState
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class ShareImageFailureCleanupTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun nullStreamDeletesTheOwnedPendingRecord() {
        withProvider(nullStream = true) { provider, context ->
            val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
            try {
                assertNull(ShareImageStore.saveShareImage(context, bitmap, "test.png"))
                assertEquals(1, provider.inserted)
                assertEquals(1, provider.deleted)
                assertEquals(0, provider.published)
            } finally { bitmap.recycle() }
        }
    }

    @Test
    fun falseEncodingAndFailedPublicationBothDeleteTheirPendingRecord() {
        withProvider { provider, context ->
            assertNull(ShareImageStore.saveWithMediaStore(context.contentResolver, "test.png") { false })
            assertEquals(1, provider.deleted)
            assertEquals(0, provider.published)
        }
        withProvider(publishSucceeds = false) { provider, context ->
            assertNull(ShareImageStore.saveWithMediaStore(context.contentResolver, "test.png") { it.write(byteArrayOf(1)); true })
            assertEquals(1, provider.deleted)
            assertEquals(0, provider.published)
        }
    }

    @Test
    fun cancellationCleansThePendingRecordAndRemainsCancellation() {
        withProvider { provider, context ->
            var cancelled = false
            try {
                ShareImageStore.saveWithMediaStore(context.contentResolver, "test.png") { throw CancellationException("test") }
            } catch (_: CancellationException) { cancelled = true }
            assertTrue(cancelled)
            assertEquals(1, provider.deleted)
        }
    }

    @Test
    fun twoImmediateSaveActionsCreateOnlyOneImage() {
        withProvider { provider, context ->
            val tomorrow = LocalDate.now().plusDays(1)
            val state = EventUiState(
                event = Event(id = 981737, title = "Guarded image save", category = CATEGORY_OTHER, date = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()),
                daysRemaining = 1, isPast = false, daysLeft = 1, nextOccurrenceDate = tomorrow
            )
            composeRule.setContent {
                CompositionLocalProvider(LocalContext provides context) {
                    MaterialTheme { DetailScreen(state, onNavigateBack = {}, onEditClick = {}, onDeleteClick = { true }) }
                }
            }
            composeRule.onNodeWithContentDescription(context.getString(R.string.button_share)).performClick()
            val click = composeRule.onNodeWithText(context.getString(R.string.share_save_image))
                .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
            composeRule.runOnIdle { click(); click() }
            composeRule.waitUntil(timeoutMillis = 5_000) { provider.published == 1 }
            assertEquals(1, provider.inserted)
            assertEquals(0, provider.deleted)
        }
    }

    private fun withProvider(
        nullStream: Boolean = false,
        publishSucceeds: Boolean = true,
        block: (ShareTestProvider, Context) -> Unit
    ) {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val provider = ShareTestProvider(targetContext.cacheDir, nullStream, publishSucceeds)
        provider.attachInfo(targetContext, ProviderInfo().apply { authority = "media"; applicationInfo = targetContext.applicationInfo })
        // This resolver redirects every URI to this unregistered provider, including "media".
        // The target app's actual ContentResolver and gallery are never used.
        val resolver = ContentResolver.wrap(provider)
        val context = object : ContextWrapper(targetContext) {
            override fun getContentResolver(): ContentResolver = resolver
        }
        try { block(provider, context) } finally { provider.outputFile?.delete() }
    }

    private class ShareTestProvider(
        private val cacheDir: File,
        private val nullStream: Boolean,
        private val publishSucceeds: Boolean
    ) : ContentProvider() {
        private val ownedUri = Uri.parse("content://media/external/images/media/981737")
        @Volatile var inserted = 0
        @Volatile var deleted = 0
        @Volatile var published = 0
        var outputFile: File? = null
        override fun onCreate() = true
        override fun getType(uri: Uri) = "image/png"
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri {
            check(uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            check(values?.getAsInteger(MediaStore.Images.Media.IS_PENDING) == 1)
            inserted += 1
            return ownedUri
        }
        override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
            check(uri == ownedUri)
            if (nullStream) return null
            val file = File.createTempFile("glimmer-share-test-", ".png", cacheDir)
            outputFile = file
            return AssetFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE), 0, AssetFileDescriptor.UNKNOWN_LENGTH)
        }
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            check(uri == ownedUri)
            deleted += 1
            outputFile?.delete()
            return 1
        }
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
            check(uri == ownedUri)
            check(values?.getAsInteger(MediaStore.Images.Media.IS_PENDING) == 0)
            if (!publishSucceeds) return 0
            published += 1
            return 1
        }
    }
}
