package com.example.timeapk.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetConfigRepositoryInstrumentationTest {
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
