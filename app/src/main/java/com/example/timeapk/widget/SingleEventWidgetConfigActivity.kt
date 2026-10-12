package com.example.timeapk.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.Event
import com.example.timeapk.ui.theme.FONT_PRESET_NOTO_SERIF_SC
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.TimeAPKTheme
import kotlinx.coroutines.launch

/** 单事件小组件配置：选择一个事件，或跟随最近事件。 */
class SingleEventWidgetConfigActivity : ComponentActivity() {
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
            val prefs = (applicationContext as TimeApplication).userPrefs
            val themeMode by prefs.themeModeFlow.collectAsState(initial = 0)
            val fontPreset by prefs.fontPresetFlow.collectAsState(initial = FONT_PRESET_NOTO_SERIF_SC)
            val appBaseFontScale by prefs.appBaseFontScaleFlow.collectAsState(initial = 1f)
            val dynamicColor by prefs.dynamicColorEnabledFlow.collectAsState(initial = false)
            TimeAPKTheme(
                themeMode = themeMode,
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = dynamicColor,
                fontPreset = fontPreset,
                baseFontScale = appBaseFontScale
            ) {
                SingleEventWidgetConfigRoute(
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
private fun SingleEventWidgetConfigRoute(
    appWidgetId: Int,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeApplication
    val repository = remember { WidgetConfigRepository(app) }
    val events by app.repository.getAllEvents().collectAsState(initial = emptyList())
    var selection by rememberSaveable { mutableIntStateOf(SINGLE_EVENT_FOLLOW_NEAREST) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(appWidgetId) {
        if (!loaded) {
            selection = repository.getSingleEventSelection(appWidgetId)
            loaded = true
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.single_widget_config_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        SongLineIcon(
                            kind = SongLineIconKind.Back,
                            contentDescription = stringResource(R.string.nav_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                onClick = {
                    if (saving) return@Surface
                    saving = true
                    scope.launch {
                        try {
                            repository.setSingleEventSelection(appWidgetId, selection)
                            SingleEventWidgetProvider.refresh(
                                app,
                                AppWidgetManager.getInstance(app),
                                intArrayOf(appWidgetId)
                            )
                            WidgetDateBoundaryScheduler.scheduleOrCancel(app)
                            onSaved()
                        } finally {
                            saving = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.widget_config_save),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .selectableGroup()
        ) {
            item {
                SelectionRow(
                    title = stringResource(R.string.single_widget_follow_nearest),
                    subtitle = stringResource(R.string.single_widget_follow_nearest_desc),
                    selected = selection == SINGLE_EVENT_FOLLOW_NEAREST,
                    onClick = { selection = SINGLE_EVENT_FOLLOW_NEAREST }
                )
            }
            items(events, key = Event::id) { event ->
                SelectionRow(
                    title = event.title,
                    subtitle = null,
                    selected = selection == event.id,
                    onClick = { selection = event.id }
                )
            }
        }
    }
}

@Composable
private fun SelectionRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Spacer(Modifier.width(12.dp))
                SongLineIcon(
                    kind = SongLineIconKind.Check,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 18.dp,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft))
    }
}
