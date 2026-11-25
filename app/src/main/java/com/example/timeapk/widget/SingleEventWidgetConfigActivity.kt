package com.example.timeapk.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.LocalePreferenceMirror
import com.example.timeapk.LocaleUtils
import com.example.timeapk.data.Event
import com.example.timeapk.data.LANG_ZH
import com.example.timeapk.ui.theme.FONT_PRESET_NOTO_SERIF_SC
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.SongModeTabRow
import com.example.timeapk.ui.common.categoryLabel
import com.example.timeapk.ui.home.toEventUiState
import com.example.timeapk.ui.settings.SettingsSwitchRow
import com.example.timeapk.ui.settings.WidgetAppearanceOptions
import com.example.timeapk.ui.theme.TimeAPKTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** 单事件小组件配置：选择一个事件，或跟随最近事件。 */
class SingleEventWidgetConfigActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val mode = LocalePreferenceMirror.read(newBase) ?: LANG_ZH
        super.attachBaseContext(LocaleUtils.wrapContext(newBase, mode))
    }

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
            val themeMode by prefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = 0)
            val fontPreset by prefs.fontPresetFlow.collectAsStateWithLifecycle(initialValue = FONT_PRESET_NOTO_SERIF_SC)
            val appBaseFontScale by prefs.appBaseFontScaleFlow.collectAsStateWithLifecycle(initialValue = 1f)
            val dynamicColor by prefs.dynamicColorEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
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
    private val loadConfiguration: suspend () -> SingleEventWidgetConfiguration,
    private val eventFlow: Flow<List<Event>>,
    private val persistConfiguration: suspend (SingleEventWidgetConfiguration) -> Unit,
    private val refreshWidget: suspend () -> Unit,
    taskScope: CoroutineScope? = null
) : ViewModel() {
    private val scope = taskScope ?: viewModelScope
    private var loadJob: Job? = null
    private var selectionLoaded by mutableStateOf(savedStateHandle.get<Boolean>("loaded") ?: false)
    private var eventsLoaded by mutableStateOf(false)
    private var persistedSelection: Int? = savedStateHandle["persistedSelection"]
    private var persistedAppearance: String? = savedStateHandle["persistedAppearance"]
    var selection by mutableIntStateOf(savedStateHandle["selection"] ?: SINGLE_EVENT_FOLLOW_NEAREST)
        private set
    var appearance by mutableStateOf(savedStateHandle.get<String>("appearance")?.let { WidgetConfig.fromJson(it) })
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
                    val stored = loadConfiguration()
                    selection = stored.selection
                    appearance = stored.appearance
                    persistedSelection = stored.selection
                    persistedAppearance = stored.appearance?.toJson()
                    savedStateHandle["selection"] = selection
                    savedStateHandle["appearance"] = persistedAppearance
                    savedStateHandle["persistedSelection"] = persistedSelection
                    savedStateHandle["persistedAppearance"] = persistedAppearance
                    savedStateHandle["loaded"] = true
                    selectionLoaded = true
                }
                eventFlow.collect {
                    events = it.filterNot(Event::isArchived)
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

    fun changeAppearance(config: WidgetConfig?) {
        if (!canSave) return
        appearance = config?.sanitize()
        savedStateHandle["appearance"] = appearance?.toJson()
        saveFailure = null
    }

    fun save() {
        if (!canSave) return
        saving = true
        saveFailure = null
        val selected = selection
        val selectedAppearance = appearance
        val appearanceJson = selectedAppearance?.toJson()
        scope.launch {
            try {
                if (persistedSelection != selected || persistedAppearance != appearanceJson) {
                    try {
                        persistConfiguration(SingleEventWidgetConfiguration(selected, selectedAppearance))
                        persistedSelection = selected
                        savedStateHandle["persistedSelection"] = selected
                        persistedAppearance = appearanceJson
                        savedStateHandle["persistedAppearance"] = appearanceJson
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
                        loadConfiguration = { repository.getSingleEventConfiguration(appWidgetId) },
                        eventFlow = app.repository.getAllEvents(),
                        persistConfiguration = { repository.setSingleEventConfiguration(appWidgetId, it) },
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
    val defaultConfig by repository.defaultConfigFlow.collectAsStateWithLifecycle(WidgetConfig.default())
    val pinned by app.userPrefs.pinnedEventIdsFlow.collectAsStateWithLifecycle(emptyList())
    val categories by app.categoryRepository.getAll().collectAsStateWithLifecycle(emptyList())
    val configuration = LocalConfiguration.current
    val options = remember(appWidgetId, configuration) { AppWidgetManager.getInstance(app).getAppWidgetOptions(appWidgetId) }
    val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110).coerceAtLeast(1)
    val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110).coerceAtLeast(1)
    val today = LocalDate.now()
    val states = remember(model.events, today) { model.events.map { it.toEventUiState(smartMilestonesEnabled = false) } }
    val previewTarget = remember(states, model.selection, today, pinned) {
        SingleEventWidgetPolicy.resolveTarget(states, model.selection, today, pinned)
    }
    val widgetAppearance = model.appearance ?: defaultConfig
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    val query = search.trim()
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                item {
                    SingleEventWidgetPreview(
                        appWidgetId,
                        previewTarget?.let { SingleEventWidgetPolicy.buildText(context, it) },
                        widgetAppearance,
                        widthDp,
                        heightDp
                    )
                    SongModeTabRow(
                        options = listOf(
                            0 to stringResource(R.string.single_widget_select_event),
                            1 to stringResource(R.string.widget_config_layer_appearance)
                        ),
                        selected = tab,
                        onSelected = { tab = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                if (tab == 1) {
                    item {
                        SettingsSwitchRow(
                            title = stringResource(R.string.single_widget_follow_appearance),
                            checked = model.appearance == null,
                            enabled = model.canSave,
                            onCheckedChange = { follow -> model.changeAppearance(if (follow) null else defaultConfig) }
                        )
                        if (model.appearance != null) {
                            WidgetAppearanceOptions(widgetAppearance, model::changeAppearance, includeAdvanced = false)
                        }
                    }
                } else {
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text(stringResource(R.string.single_widget_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    SelectionRow(
                        title = stringResource(R.string.single_widget_follow_nearest),
                        subtitle = stringResource(R.string.single_widget_follow_nearest_desc),
                        selected = model.selection == SINGLE_EVENT_FOLLOW_NEAREST,
                        enabled = model.canSave,
                        onClick = { model.select(SINGLE_EVENT_FOLLOW_NEAREST) }
                    )
                }
                val visible = states.map { state ->
                    val text = SingleEventWidgetPolicy.buildText(context, state)
                    val label = categoryLabel(context, state.event.category, categories)
                    val value = listOf(text.label, text.number, text.unit).filter(String::isNotBlank).joinToString(" ")
                    state.event to "$label · ${text.dateText} · $value"
                }.filter { (event, subtitle) ->
                    query.isBlank() || event.title.contains(query, ignoreCase = true) ||
                        subtitle.contains(query, ignoreCase = true)
                }
                items(visible, key = { it.first.id }) { (event, subtitle) ->
                    SelectionRow(
                        title = event.title,
                        subtitle = subtitle,
                        selected = model.selection == event.id,
                        enabled = model.canSave,
                        onClick = { model.select(event.id) }
                    )
                }
                if (visible.isEmpty() && query.isNotEmpty()) item {
                    Text(stringResource(R.string.home_empty_no_matches), modifier = Modifier.padding(vertical = 12.dp))
                }
                }
            }
        }
    }
}

@Composable
private fun SingleEventWidgetPreview(
    widgetId: Int,
    text: SingleEventWidgetText?,
    config: WidgetConfig,
    widthDp: Int,
    heightDp: Int
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val theme = WidgetThemeResolver.resolve(context)
    val views = remember(context, widgetId, text, config, theme, widthDp, heightDp) {
        SingleEventWidgetProvider.buildViews(context, widgetId, text, config, theme, widthDp, heightDp)
    }
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val factor = minOf(1f, maxWidth.value / widthDp, 320f / heightDp)
        AndroidView(
            modifier = Modifier.width((widthDp * factor).dp).height((heightDp * factor).dp),
            factory = { FrameLayout(it) },
            update = { host ->
                if (host.tag !== views) {
                    host.tag = views
                    host.removeAllViews()
                    val preview = views.apply(host.context, host)
                    preview.findViewById<android.view.View>(R.id.widget_root)?.isClickable = false
                    val size = with(density) {
                        FrameLayout.LayoutParams(widthDp.dp.roundToPx(), heightDp.dp.roundToPx(), Gravity.CENTER)
                    }
                    host.addView(preview, size)
                }
                host.getChildAt(0).apply {
                    scaleX = factor
                    scaleY = factor
                }
            }
        )
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
