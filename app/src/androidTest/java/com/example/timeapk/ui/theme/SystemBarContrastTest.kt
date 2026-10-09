package com.example.timeapk.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemBarContrastTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun darkCustomBackgroundUsesLightIconsEvenInLightMode() {
        verifyIcons(themeMode = 1, background = "#000000", darkIcons = false)
    }

    @Test
    fun lightCustomBackgroundUsesDarkIconsEvenInDarkMode() {
        verifyIcons(themeMode = 2, background = "#FFFFFF", darkIcons = true)
    }

    private fun verifyIcons(themeMode: Int, background: String, darkIcons: Boolean) {
        composeRule.setContent {
            TimeAPKTheme(themeMode = themeMode, customBackgroundHex = background) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            val window = composeRule.activity.window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            assertEquals(darkIcons, controller.isAppearanceLightStatusBars)
            assertEquals(darkIcons, controller.isAppearanceLightNavigationBars)
        }
    }
}
