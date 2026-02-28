package com.example.timeapk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.timeapk.ui.theme.TimeAPKTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val openEventIdState = mutableStateOf<Int?>(null)

    override fun attachBaseContext(newBase: Context) {
        // 在 Activity 创建前，根据用户偏好包裹带有指定语言的 Context，默认中文
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
            val fontPreset by prefs.fontPresetFlow.collectAsState(initial = 0)
            TimeAPKTheme(
                themeMode = themeMode,
                darkTheme = isSystemInDarkTheme(),
                fontPreset = fontPreset,
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
