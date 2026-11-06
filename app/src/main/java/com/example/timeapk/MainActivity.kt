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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.timeapk.data.LANG_ZH
import com.example.timeapk.ui.theme.FONT_PRESET_NOTO_SERIF_SC
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.TimeAPKTheme
import com.example.timeapk.widget.WidgetDateBoundaryScheduler
import com.example.timeapk.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val openEventIdState = mutableStateOf<Int?>(null)

    override fun attachBaseContext(newBase: Context) {
        val mode = LocalePreferenceMirror.read(newBase) ?: LANG_ZH
        super.attachBaseContext(LocaleUtils.wrapContext(newBase, mode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateOpenEventIdFromIntent(intent)
        migrateLocaleMirror()
        setContent {
            val openEventId by openEventIdState
            val app = applicationContext as TimeApplication
            val prefs = app.userPrefs
            val themeMode by prefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = 0)
            val customBackgroundHex by prefs.customBackgroundHexFlow.collectAsStateWithLifecycle(initialValue = null)
            val customSurfaceHex by prefs.customSurfaceHexFlow.collectAsStateWithLifecycle(initialValue = null)
            val customPrimaryHex by prefs.customPrimaryHexFlow.collectAsStateWithLifecycle(initialValue = null)
            val customOnBackgroundHex by prefs.customOnBackgroundHexFlow.collectAsStateWithLifecycle(initialValue = null)
            val fontPreset by prefs.fontPresetFlow.collectAsStateWithLifecycle(initialValue = FONT_PRESET_NOTO_SERIF_SC)
            val appBaseFontScale by prefs.appBaseFontScaleFlow.collectAsStateWithLifecycle(initialValue = 1f)
            val reduceMotionEnabled by prefs.reduceMotionEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
            val dynamicColorEnabled by prefs.dynamicColorEnabledFlow.collectAsStateWithLifecycle(initialValue = false)

            SideEffect {
                val systemReducedMotion = !ValueAnimator.areAnimatorsEnabled()
                AnimationSpecs.setReducedMotionEnabled(reduceMotionEnabled || systemReducedMotion)
            }

            TimeAPKTheme(
                themeMode = themeMode,
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = dynamicColorEnabled,
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

    override fun onResume() {
        super.onResume()
        // Cheap safety net for the date boundary: the widget alarm can be delayed under Doze.
        WidgetUpdater.refreshCountdownWidgets(this)
        WidgetDateBoundaryScheduler.scheduleOrCancel(this)
        (application as? TimeApplication)?.installPendingUpdate(this)
    }

    private fun updateOpenEventIdFromIntent(intent: Intent) {
        openEventIdState.value = intent.getIntExtra("open_event_id", -1).takeIf { it >= 0 }
    }

    private fun migrateLocaleMirror() {
        val app = applicationContext as TimeApplication
        lifecycleScope.launch {
            val stored = app.userPrefs.languageModeFlow.first()
            val mirrored = LocalePreferenceMirror.read(this@MainActivity)
            val decision = resolveLocaleMirrorMigration(stored, mirrored)
            if (decision.writeMirror) {
                LocalePreferenceMirror.write(this@MainActivity, stored)
                if (decision.recreateActivity) recreate()
            }
        }
    }
}
