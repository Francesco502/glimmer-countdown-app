package com.example.timeapk.widget

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WidgetCollectionRefreshTest {
    @Test
    fun systemCanLoadCollectionWhileProviderIsPublishing() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val factory = CountdownRemoteViewsFactory(context, Intent())
        val worker = Executors.newSingleThreadExecutor()
        try {
            // Model Android's synchronous callback during updateAppWidget/notifyDataChanged.
            // A shared provider/factory lock makes this time out, also blocking config save.
            WidgetRefreshCoordinator.runLatestSnapshot {
                worker.submit { factory.onDataSetChanged() }.get(10, TimeUnit.SECONDS)
            }
            // A second refresh must also complete, including after an empty collection.
            WidgetRefreshCoordinator.runLatestSnapshot {
                worker.submit { factory.onDataSetChanged() }.get(10, TimeUnit.SECONDS)
            }
            factory.onDestroy()
            assertEquals(0, factory.getCount())
            assertNull(factory.getViewAt(0))
        } finally {
            worker.shutdownNow()
            worker.awaitTermination(10, TimeUnit.SECONDS)
        }
    }
}
