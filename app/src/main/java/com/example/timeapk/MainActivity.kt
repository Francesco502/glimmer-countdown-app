package com.example.timeapk

import android.content.Context
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.timeapk.ui.theme.TimeAPKTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // 在 Activity 创建前，根据用户偏好包裹带有指定语言的 Context，默认中文
        val app = newBase.applicationContext as TimeApplication
        val languageMode = runBlocking { app.userPrefs.languageModeFlow.first() }
        val wrapped = LocaleUtils.wrapContext(newBase, languageMode)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var openEventId by remember {
                mutableStateOf(intent.getIntExtra("open_event_id", -1).takeIf { it >= 0 })
            }
            val app = applicationContext as TimeApplication
            val themeMode by app.userPrefs.themeModeFlow.collectAsState(initial = 0)
            TimeAPKTheme(
                themeMode = themeMode,
                darkTheme = isSystemInDarkTheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimeApp(
                        initialOpenEventId = openEventId,
                        onOpenEventIdConsumed = { openEventId = null }
                    )
                }
            }
        }
    }
}
