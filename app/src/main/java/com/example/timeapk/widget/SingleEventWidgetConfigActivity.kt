package com.example.timeapk.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.Event
import com.example.timeapk.ui.theme.FONT_PRESET_NOTO_SERIF_SC
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.TimeAPKTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

internal enum class SingleEventWidgetSaveFailure { PERSIST, REFRESH }

internal class SingleEventWidgetConfigViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val loadSelection: suspend () -> Int,
    private val eventFlow: Flow<List<Event>>,
    private val persistSelection: suspend (Int) -> Unit,
    private val refreshWidget: suspend () -> Unit,
    taskScope: CoroutineScope? = null
) : ViewModel() {
    private val scope = taskScope ?: viewModelScope
    private var loadJob: Job? = null
    private var selectionLoaded by mutableStateOf(savedStateHandle.get<Boolean>("loaded") ?: false)
    private var eventsLoaded by mutableStateOf(false)
    private var persistedSelection: Int? = savedStateHandle["persistedSelection"]
    var selection by mutableIntStateOf(savedStateHandle["selection"] ?: SINGLE_EVENT_FOLLOW_NEAREST)
        private set
    var events by mutableStateOf<List<Event>>(emptyList())
        private set
    var loadFailed by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var saveFailure by mutableStateOf<SingleEventWidgetSaveFailure?>(null)
        private set
    var saved by mutableStateOf(false)
        private set
    val ready: Boolean get() = selectionLoaded && eventsLoaded && !loadFailed
    val canSave: Boolean get() = ready && !saving && !saved

    init {
        retryLoad()
    }

    fun retryLoad() {
        if (saving) return
        loadJob?.cancel()
        loadFailed = false
        eventsLoaded = false
        loadJob = scope.launch {
            try {
                if (!selectionLoaded) {
                    val stored = loadSelection()
                    selection = stored
                    persistedSelection = stored
                    savedStateHandle["selection"] = stored
                    savedStateHandle["persistedSelection"] = stored
                    savedStateHandle["loaded"] = true
                    selectionLoaded = true
                }
                eventFlow.collect {
                    events = it
                    eventsLoaded = true
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loadFailed = true
            }
        }
    }

    fun select(eventId: Int) {
        if (!canSave) return
        selection = eventId
        savedStateHandle["selection"] = eventId
        saveFailure = null
    }

    fun save() {
        if (!canSave) return
        saving = true
        saveFailure = null
        val selected = selection
        scope.launch {
            try {
                if (persistedSelection != selected) {
                    try {
                        persistSelection(selected)
                        persistedSelection = selected
                        savedStateHandle["persistedSelection"] = selected
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        saveFailure = SingleEventWidgetSaveFailure.PERSIST
                        return@launch
                    }
                }
                try {
                    refreshWidget()
                    saved = true
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    saveFailure = SingleEventWidgetSaveFailure.REFRESH
                }
            } finally {
                saving = false
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
    val repository = remember(app) { WidgetConfigRepository(app) }
    val model: SingleEventWidgetConfigViewModel = viewModel(
        key = "single-event-widget-$appWidgetId",
        factory = remember(appWidgetId, app) {
            viewModelFactory {
                initializer {
                    SingleEventWidgetConfigViewModel(
                        savedStateHandle = createSavedStateHandle(),
                        loadSelection = { repository.getSingleEventSelection(appWidgetId) },
                        eventFlow = app.repository.getAllEvents(),
                        persistSelection = { repository.setSingleEventSelection(appWidgetId, it) },
                        refreshWidget = {
                            withContext(Dispatchers.IO) {
                                SingleEventWidgetProvider.refresh(
                                    app,
                                    AppWidgetManager.getInstance(app),
                                    intArrayOf(appWidgetId)
                                )
                                WidgetDateBoundaryScheduler.scheduleOrCancel(app)
                            }
                        }
                    )
                }
            }
        }
    )
    LaunchedEffect(model.saved) {
        if (model.saved) onSaved()
    }
    BackHandler(enabled = model.saving) { }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.single_widget_config_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = !model.saving) {
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
                onClick = model::save,
                enabled = model.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (model.canSave) 1f else 0.38f)
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (model.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp).size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = stringResource(
                                if (model.saveFailure == SingleEventWidgetSaveFailure.REFRESH) {
                                    R.string.widget_config_retry_refresh
                                } else {
                                    R.string.widget_config_save
                                }
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
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
            if (model.loadFailed) {
                item {
                    Text(
                        text = stringResource(R.string.widget_load_failed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable(role = Role.Button, onClick = model::retryLoad).padding(vertical = 16.dp)
                    )
                }
            } else if (!model.ready) {
                item { CircularProgressIndicator(modifier = Modifier.padding(24.dp)) }
            } else {
                model.saveFailure?.let { failure ->
                    item {
                        Text(
                            text = stringResource(
                                if (failure == SingleEventWidgetSaveFailure.REFRESH) {
                                    R.string.single_widget_refresh_failed
                                } else {
                                    R.string.widget_save_failed
                                }
                            ),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
                item {
                    SelectionRow(
                        title = stringResource(R.string.single_widget_follow_nearest),
                        subtitle = stringResource(R.string.single_widget_follow_nearest_desc),
                        selected = model.selection == SINGLE_EVENT_FOLLOW_NEAREST,
                        enabled = model.canSave,
                        onClick = { model.select(SINGLE_EVENT_FOLLOW_NEAREST) }
                    )
                }
                items(model.events, key = Event::id) { event ->
                    SelectionRow(
                        title = event.title,
                        subtitle = null,
                        selected = model.selection == event.id,
                        enabled = model.canSave,
                        onClick = { model.select(event.id) }
                    )
                }
            }
        }
    }
}
@Composable
private fun SelectionRow(title: String, subtitle: String?, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
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
