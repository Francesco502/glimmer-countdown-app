package com.example.timeapk.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetConfigRepositoryInstrumentationTest {
    @Test
    fun widgetRootLayoutsExposeTopLevelLauncherBackground() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val rootLayouts = listOf(
            R.layout.widget_countdown,
            R.layout.widget_countdown_corner_small,
            R.layout.widget_countdown_corner_medium,
            R.layout.widget_countdown_corner_large,
            R.layout.widget_countdown_transparent,
            R.layout.widget_countdown_transparent_corner_small,
            R.layout.widget_countdown_transparent_corner_medium,
            R.layout.widget_countdown_transparent_corner_large
        )

        rootLayouts.forEach { layoutResId ->
            val root = LayoutInflater.from(context).inflate(layoutResId, null, false)

            assertEquals(
                "Android 12+ Launcher background must be the top-level view for layout $layoutResId",
                android.R.id.background,
                root.id
            )
            assertTrue(
                "Clickable widget content root is missing for layout $layoutResId",
                root.findViewById<View>(R.id.widget_root) != null
            )
        }
    }

    @Test
    fun transparentRootLayoutsStartWithIdentifiableLauncherBackground() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val transparentLayouts = listOf(
            R.layout.widget_countdown_transparent,
            R.layout.widget_countdown_transparent_corner_small,
            R.layout.widget_countdown_transparent_corner_medium,
            R.layout.widget_countdown_transparent_corner_large
        )

        transparentLayouts.forEach { layoutResId ->
            val root = LayoutInflater.from(context).inflate(layoutResId, null, false)
            val background = root.findViewById<View>(android.R.id.background).background
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            background.setBounds(0, 0, bitmap.width, bitmap.height)
            background.draw(Canvas(bitmap))

            assertTrue(
                "Launcher must be able to identify the initial widget background for layout $layoutResId",
                Color.alpha(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)) > 0
            )
        }
    }

    @Test
    fun cornerModesSelectDistinctRemoteViewsLayouts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val provider = CountdownAppWidgetProvider.Companion
        val theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = true)

        val systemLayout = provider.buildWidgetRemoteViews(
            context = context,
            appWidgetId = 62_000,
            sizeBucket = WidgetSizeBucket.STANDARD_SQUARE,
            config = WidgetConfig.default().copy(cornerMode = CORNER_SYSTEM),
            themeSnapshot = theme,
            attachRemoteAdapter = false
        ).layoutId
        val smallLayout = provider.buildWidgetRemoteViews(
            context = context,
            appWidgetId = 62_001,
            sizeBucket = WidgetSizeBucket.STANDARD_SQUARE,
            config = WidgetConfig.default().copy(cornerMode = CORNER_SMALL),
            themeSnapshot = theme,
            attachRemoteAdapter = false
        ).layoutId
        val mediumLayout = provider.buildWidgetRemoteViews(
            context = context,
            appWidgetId = 62_002,
            sizeBucket = WidgetSizeBucket.STANDARD_SQUARE,
            config = WidgetConfig.default().copy(cornerMode = CORNER_MEDIUM),
            themeSnapshot = theme,
            attachRemoteAdapter = false
        ).layoutId
        val largeLayout = provider.buildWidgetRemoteViews(
            context = context,
            appWidgetId = 62_003,
            sizeBucket = WidgetSizeBucket.STANDARD_SQUARE,
            config = WidgetConfig.default().copy(cornerMode = CORNER_LARGE),
            themeSnapshot = theme,
            attachRemoteAdapter = false
        ).layoutId

        assertEquals(R.layout.widget_countdown, systemLayout)
        assertNotEquals(systemLayout, smallLayout)
        assertNotEquals(smallLayout, mediumLayout)
        assertNotEquals(mediumLayout, largeLayout)
        assertNotEquals(systemLayout, largeLayout)
    }

    @Test
    fun multipleWidgetInstancesKeepIndependentConfigsAndFallbackToLatestDefault() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = WidgetConfigRepository(context)
        val originalDefault = repository.getDefaultConfig()
        val originalInstances = repository.getInstanceConfigs()
        val firstWidgetId = 61_001
        val secondWidgetId = 61_002
        val unconfiguredWidgetId = 61_003

        val firstConfig = WidgetConfig.default().copy(
            appearancePreset = APPEARANCE_SEAL,
            contentScope = CONTENT_PINNED,
            sortMode = SORT_HOME
        )
        val secondConfig = WidgetConfig.default().copy(
            widthCells = 4,
            heightCells = 2,
            appearancePreset = APPEARANCE_TRANSPARENT,
            sortMode = SORT_NEAREST_FIRST
        )
        val latestDefault = WidgetConfig.default().copy(
            appearancePreset = APPEARANCE_CELADON,
            contentScope = CONTENT_FUTURE
        )

        try {
            repository.setConfigForWidget(firstWidgetId, firstConfig)
            repository.setConfigForWidget(secondWidgetId, secondConfig)
            repository.setDefaultConfig(latestDefault)

            assertEquals(firstConfig, repository.getConfigForWidget(firstWidgetId))
            assertEquals(secondConfig, repository.getConfigForWidget(secondWidgetId))
            assertEquals(latestDefault, repository.getConfigForWidget(unconfiguredWidgetId))

            repository.removeConfigForWidget(firstWidgetId)

            assertEquals(latestDefault, repository.getConfigForWidget(firstWidgetId))
            assertEquals(secondConfig, repository.getConfigForWidget(secondWidgetId))
        } finally {
            repository.setDefaultConfig(originalDefault)
            repository.setAllInstanceConfigs(originalInstances)
        }
    }
}
