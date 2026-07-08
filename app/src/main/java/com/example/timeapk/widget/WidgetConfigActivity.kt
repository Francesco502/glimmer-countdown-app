package com.example.timeapk.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.settings.WidgetConfigEditor
import com.example.timeapk.ui.theme.FONT_PRESET_NOTO_SERIF_SC
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.TimeAPKTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private val appWidgetId: Int
        get() = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val app = applicationContext as TimeApplication
            val prefs = app.userPrefs
            val themeMode by prefs.themeModeFlow.collectAsState(initial = 0)
            val customBackgroundHex by prefs.customBackgroundHexFlow.collectAsState(initial = null)
            val customSurfaceHex by prefs.customSurfaceHexFlow.collectAsState(initial = null)
            val customPrimaryHex by prefs.customPrimaryHexFlow.collectAsState(initial = null)
            val customOnBackgroundHex by prefs.customOnBackgroundHexFlow.collectAsState(initial = null)
            val fontPreset by prefs.fontPresetFlow.collectAsState(initial = FONT_PRESET_NOTO_SERIF_SC)
            val appBaseFontScale by prefs.appBaseFontScaleFlow.collectAsState(initial = 1f)

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
                WidgetConfigRoute(
                    appWidgetId = appWidgetId,
                    onCancel = { finish() },
                    onSaved = {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigRoute(
    appWidgetId: Int,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { WidgetConfigRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(WidgetConfig.default()) }

    LaunchedEffect(appWidgetId) {
        config = repository.getConfigForWidget(appWidgetId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.widget_config_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable(onClick = onCancel)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SongLineIcon(
                            kind = SongLineIconKind.Back,
                            contentDescription = stringResource(R.string.nav_back),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            WidgetConfigSaveBar(
                onSaveClick = {
                    scope.launch {
                        repository.setConfigForWidget(appWidgetId, config)
                        CountdownAppWidgetProvider.refreshAllWidgets(context)
                        onSaved()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            WidgetConfigEditor(
                config = config,
                onConfigChange = { config = it }
            )
        }
    }
}

@Composable
private fun WidgetConfigSaveBar(onSaveClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSaveClick)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.widget_config_save),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
