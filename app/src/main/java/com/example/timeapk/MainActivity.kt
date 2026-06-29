package com.example.timeapk

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.timeapk.ui.theme.FONT_PRESET_NOTO_SERIF_SC
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.TimeAPKTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val openEventIdState = mutableStateOf<Int?>(null)

    override fun attachBaseContext(newBase: Context) {
        val app = newBase.applicationContext as TimeApplication
        val languageMode = runBlocking { app.userPrefs.languageModeFlow.first() }
        val wrapped = LocaleUtils.wrapContext(newBase, languageMode)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateOpenEventIdFromIntent(intent)
        setContent {
            val openEventId by openEventIdState
            val app = applicationContext as TimeApplication
            val prefs = app.userPrefs
            val themeMode by prefs.themeModeFlow.collectAsState(initial = 0)
            val customBackgroundHex by prefs.customBackgroundHexFlow.collectAsState(initial = null)
            val customSurfaceHex by prefs.customSurfaceHexFlow.collectAsState(initial = null)
            val customPrimaryHex by prefs.customPrimaryHexFlow.collectAsState(initial = null)
            val customOnBackgroundHex by prefs.customOnBackgroundHexFlow.collectAsState(initial = null)
            val fontPreset by prefs.fontPresetFlow.collectAsState(initial = FONT_PRESET_NOTO_SERIF_SC)
            val appBaseFontScale by prefs.appBaseFontScaleFlow.collectAsState(initial = 1f)
            val reduceMotionEnabled by prefs.reduceMotionEnabledFlow.collectAsState(initial = false)

            SideEffect {
                val systemReducedMotion = !ValueAnimator.areAnimatorsEnabled()
                AnimationSpecs.setReducedMotionEnabled(reduceMotionEnabled || systemReducedMotion)
            }

            TimeAPKTheme(
                themeMode = themeMode,
                darkTheme = isSystemInDarkTheme(),
                fontPreset = fontPreset,
                baseFontScale = appBaseFontScale,
                customBackgroundHex = customBackgroundHex,
                customSurfaceHex = customSurfaceHex,
                customPrimaryHex = customPrimaryHex,
                customOnBackgroundHex = customOnBackgroundHex
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimeApp(
                        initialOpenEventId = openEventId,
                        onOpenEventIdConsumed = { openEventIdState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateOpenEventIdFromIntent(intent)
    }

    private fun updateOpenEventIdFromIntent(intent: Intent) {
        openEventIdState.value = intent.getIntExtra("open_event_id", -1).takeIf { it >= 0 }
    }
}
