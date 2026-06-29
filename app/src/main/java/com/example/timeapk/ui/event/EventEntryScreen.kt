package com.example.timeapk.ui.event

import android.Manifest
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.graphics.toColorInt
import com.example.timeapk.permissions.areAppNotificationsEnabledCompat
import com.example.timeapk.permissions.canPostAppNotifications
import com.example.timeapk.permissions.didGrantCalendarPermissionAfterRequest
import com.example.timeapk.permissions.didGrantNotificationPermissionAfterRequest
import com.example.timeapk.permissions.hasCalendarReadWritePermission
import com.example.timeapk.permissions.hasNotificationRuntimePermission
import com.example.timeapk.permissions.markCalendarPermissionRequested
import com.example.timeapk.permissions.markNotificationPermissionRequested
import com.example.timeapk.permissions.notificationRuntimePermissionName
import com.example.timeapk.permissions.openAppDetailsSettings
import com.example.timeapk.permissions.openAppNotificationSettings
import com.example.timeapk.permissions.shouldShowCalendarPermissionRationaleCompat
import com.example.timeapk.permissions.shouldShowNotificationPermissionRationaleCompat
import com.example.timeapk.permissions.wasCalendarPermissionRequestedBefore
import com.example.timeapk.permissions.wasNotificationPermissionRequestedBefore
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.sanitizeReminderTimeMinutesOfDay
import com.example.timeapk.ui.AppViewModelProvider
import com.example.timeapk.ui.components.BottomSheetDatePicker
import com.example.timeapk.ui.components.PermissionActionDialog
import com.example.timeapk.ui.components.PermissionDialogSpec
import com.example.timeapk.ui.components.SnapWheelPicker
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongFilterChip
import com.example.timeapk.ui.theme.SongPaperSurface
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.getDisplayDateFormatter
import com.example.timeapk.ui.utils.formatLunarDateString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

private val PRESET_COLORS = listOf(
    // Song-style preset palette for event cards.
    "#4A4933",
    "#457080",
    "#5F856B",
    "#AF4E31",
    "#AC8F62",
    "#86351C",
    "#5B8E79",
    "#3A4550",
    "#785B64"
)
// CATEGORY_DEFAULT_COLOR map removed as explicit category selection is gone

private val EventEntryContentMaxWidth = 720.dp

private enum class SaveRequestOrigin {
    Standard,
    PermissionFallback
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEntryScreen(
    eventId: Int?,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventEntryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val eventUiState by viewModel.eventUiState.collectAsState()
    val latestEventDetails by rememberUpdatedState(eventUiState.eventDetails)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val calendarPermissions = remember {
        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }
    var pendingSaveAfterNotificationPermission by remember { mutableStateOf(false) }
    var pendingSaveAfterCalendarPermission by remember { mutableStateOf(false) }
    var pendingSaveDetailsOverride by remember { mutableStateOf<EventDetails?>(null) }
    var pendingSaveOrigin by remember { mutableStateOf(SaveRequestOrigin.Standard) }
    var permissionDialog by remember { mutableStateOf<PermissionDialogSpec?>(null) }
    var showDiscardChangesDialog by remember { mutableStateOf(false) }
    var isEntryInitialized by remember(eventId) { mutableStateOf(false) }
    LaunchedEffect(eventId) {
        isEntryInitialized = false
        try {
            viewModel.prepareForEvent(eventId)
        } finally {
            isEntryInitialized = true
        }
    }
    LaunchedEffect(eventUiState.loadError) {
        if (eventUiState.loadError) {
            snackbarHostState.showSnackbar(context.getString(R.string.event_load_error))
            delay(1200)
            navigateBack()
        }
    }

    var isSaving by remember { mutableStateOf(false) }

    fun hasCalendarPermission(): Boolean {
        return context.hasCalendarReadWritePermission()
    }

    var saveWithoutReminder: (() -> Unit)? by remember { mutableStateOf(null) }
    var saveWithoutCalendarSync: (() -> Unit)? by remember { mutableStateOf(null) }
    var launchNotificationPermissionRequest: (() -> Unit)? by remember { mutableStateOf(null) }
    var launchCalendarPermissionRequest: (() -> Unit)? by remember { mutableStateOf(null) }

    fun showNotificationPermissionRationaleForSave() {
        permissionDialog = PermissionDialogSpec(
            title = context.getString(R.string.permission_dialog_title_notifications),
            message = context.getString(R.string.notification_permission_rationale_message),
            confirmText = context.getString(R.string.permission_dialog_button_continue),
            dismissText = context.getString(R.string.permission_dialog_button_save_without_reminder),
            onConfirm = {
                permissionDialog = null
                launchNotificationPermissionRequest?.invoke()
            },
            onDismiss = {
                permissionDialog = null
                saveWithoutReminder?.invoke()
            },
            onRequestDismiss = {
                permissionDialog = null
                pendingSaveOrigin = SaveRequestOrigin.Standard
                isSaving = false
            }
        )
    }

    fun showNotificationSettingsForSave() {
        permissionDialog = PermissionDialogSpec(
            title = context.getString(R.string.permission_dialog_title_notifications),
            message = context.getString(R.string.notification_permission_settings_message),
            confirmText = context.getString(R.string.permission_dialog_button_open_settings),
            dismissText = context.getString(R.string.permission_dialog_button_save_without_reminder),
            onConfirm = {
                permissionDialog = null
                pendingSaveOrigin = SaveRequestOrigin.Standard
                isSaving = false
                context.openAppNotificationSettings()
            },
            onDismiss = {
                permissionDialog = null
                saveWithoutReminder?.invoke()
            },
            onRequestDismiss = {
                permissionDialog = null
                pendingSaveOrigin = SaveRequestOrigin.Standard
                isSaving = false
            }
        )
    }

    fun showCalendarPermissionRationaleForSave() {
        permissionDialog = PermissionDialogSpec(
            title = context.getString(R.string.permission_dialog_title_calendar),
            message = context.getString(R.string.calendar_permission_rationale_message),
            confirmText = context.getString(R.string.permission_dialog_button_continue),
            dismissText = context.getString(R.string.permission_dialog_button_save_without_sync),
            onConfirm = {
                permissionDialog = null
                launchCalendarPermissionRequest?.invoke()
            },
            onDismiss = {
                permissionDialog = null
                saveWithoutCalendarSync?.invoke()
            },
            onRequestDismiss = {
                permissionDialog = null
                pendingSaveOrigin = SaveRequestOrigin.Standard
                isSaving = false
            }
        )
    }

    fun showCalendarSettingsForSave() {
        permissionDialog = PermissionDialogSpec(
            title = context.getString(R.string.permission_dialog_title_calendar),
            message = context.getString(R.string.calendar_permission_settings_message),
            confirmText = context.getString(R.string.permission_dialog_button_open_settings),
            dismissText = context.getString(R.string.permission_dialog_button_save_without_sync),
            onConfirm = {
                permissionDialog = null
                pendingSaveOrigin = SaveRequestOrigin.Standard
                isSaving = false
                context.openAppDetailsSettings()
            },
            onDismiss = {
                permissionDialog = null
                saveWithoutCalendarSync?.invoke()
            },
            onRequestDismiss = {
                permissionDialog = null
                pendingSaveOrigin = SaveRequestOrigin.Standard
                isSaving = false
            }
        )
    }

    var requestNotificationAccessForSave: ((EventDetails) -> Unit)? by remember { mutableStateOf(null) }

    fun launchSave(
        detailsOverride: EventDetails = pendingSaveDetailsOverride ?: latestEventDetails,
        saveOrigin: SaveRequestOrigin = pendingSaveOrigin
    ) {
        pendingSaveDetailsOverride = null
        viewModel.updateUiState(detailsOverride)
        coroutineScope.launch {
            when (val result = viewModel.saveEvent()) {
                is SaveEventResult.Success -> {
                    pendingSaveOrigin = SaveRequestOrigin.Standard
                    isSaving = false
                    navigateBack()
                }
                is SaveEventResult.PartialSuccess -> {
                    isSaving = false
                    val shouldNavigateBack = saveOrigin == SaveRequestOrigin.PermissionFallback
                    pendingSaveOrigin = SaveRequestOrigin.Standard
                    if (shouldNavigateBack) {
                        navigateBack()
                    } else {
                        snackbarHostState.showSnackbar(result.message)
                    }
                }
                is SaveEventResult.Failure -> {
                    pendingSaveOrigin = SaveRequestOrigin.Standard
                    isSaving = false
                    snackbarHostState.showSnackbar(result.message)
                }
            }
        }
    }

    fun continueSaveAfterPermissionChecks(
        details: EventDetails = pendingSaveDetailsOverride ?: latestEventDetails,
        saveOrigin: SaveRequestOrigin = pendingSaveOrigin
    ) {
        pendingSaveDetailsOverride = details
        pendingSaveOrigin = saveOrigin
        val shouldCheckCalendarPermission = details.syncToScheduleEnabled
        if (shouldCheckCalendarPermission && !hasCalendarPermission()) {
            when {
                context.shouldShowCalendarPermissionRationaleCompat() -> showCalendarPermissionRationaleForSave()
                context.wasCalendarPermissionRequestedBefore() -> showCalendarSettingsForSave()
                else -> launchCalendarPermissionRequest?.invoke()
            }
            return
        }
        launchSave(detailsOverride = details, saveOrigin = saveOrigin)
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        val granted = context.didGrantCalendarPermissionAfterRequest(grantResults)

        if (!pendingSaveAfterCalendarPermission) {
            return@rememberLauncherForActivityResult
        }

        pendingSaveAfterCalendarPermission = false
        if (granted) {
            continueSaveAfterPermissionChecks(saveOrigin = pendingSaveOrigin)
        } else {
            saveWithoutCalendarSync?.invoke()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!pendingSaveAfterNotificationPermission) {
            return@rememberLauncherForActivityResult
        }

        pendingSaveAfterNotificationPermission = false
        if (context.didGrantNotificationPermissionAfterRequest(granted)) {
            continueSaveAfterPermissionChecks(saveOrigin = pendingSaveOrigin)
        } else {
            saveWithoutReminder?.invoke()
        }
    }

    launchNotificationPermissionRequest = {
        pendingSaveAfterNotificationPermission = true
        context.markNotificationPermissionRequested()
        notificationPermissionLauncher.launch(notificationRuntimePermissionName())
    }

    launchCalendarPermissionRequest = {
        pendingSaveAfterCalendarPermission = true
        context.markCalendarPermissionRequested()
        calendarPermissionLauncher.launch(calendarPermissions)
    }

    saveWithoutReminder = {
        val downgradedDetails = (pendingSaveDetailsOverride ?: latestEventDetails).copy(
            remindEnabled = false
        )
        viewModel.updateUiState(downgradedDetails)
        pendingSaveOrigin = SaveRequestOrigin.PermissionFallback
        continueSaveAfterPermissionChecks(
            details = downgradedDetails,
            saveOrigin = SaveRequestOrigin.PermissionFallback
        )
    }

    saveWithoutCalendarSync = {
        val downgradedDetails = (pendingSaveDetailsOverride ?: latestEventDetails).copy(
            syncToScheduleEnabled = false
        )
        viewModel.updateUiState(downgradedDetails)
        pendingSaveOrigin = SaveRequestOrigin.PermissionFallback
        launchSave(
            detailsOverride = downgradedDetails,
            saveOrigin = SaveRequestOrigin.PermissionFallback
        )
    }

    requestNotificationAccessForSave = { details ->
        pendingSaveDetailsOverride = details
        pendingSaveOrigin = SaveRequestOrigin.Standard
        when {
            context.hasNotificationRuntimePermission() -> continueSaveAfterPermissionChecks(details)
            context.shouldShowNotificationPermissionRationaleCompat() -> showNotificationPermissionRationaleForSave()
            context.wasNotificationPermissionRequestedBefore() -> showNotificationSettingsForSave()
            else -> launchNotificationPermissionRequest?.invoke()
        }
    }

    val isEditing = eventId != null && eventId != 0
    val hasUnsavedChanges = eventUiState.hasUnsavedChanges()

    fun requestNavigateBack() {
        when {
            isSaving -> Unit
            hasUnsavedChanges -> showDiscardChangesDialog = true
            else -> navigateBack()
        }
    }

    fun requestSave() {
        if (isSaving) return

        val details = eventUiState.eventDetails
        pendingSaveOrigin = SaveRequestOrigin.Standard
        if (details.remindEnabled && !context.hasNotificationRuntimePermission()) {
            isSaving = true
            requestNotificationAccessForSave?.invoke(details) ?: run {
                isSaving = false
            }
            return
        }

        isSaving = true
        continueSaveAfterPermissionChecks(details)
    }

    BackHandler(enabled = !showDiscardChangesDialog && permissionDialog == null) {
        requestNavigateBack()
    }

    permissionDialog?.let { dialog ->
        PermissionActionDialog(spec = dialog)
    }
    if (showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardChangesDialog = false },
            shape = MaterialTheme.shapes.medium,
            title = { Text(stringResource(R.string.discard_changes_dialog_title)) },
            text = { Text(stringResource(R.string.discard_changes_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardChangesDialog = false
                        navigateBack()
                    }
                ) {
                    Text(stringResource(R.string.discard_changes_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChangesDialog = false }) {
                    Text(stringResource(R.string.discard_changes_dialog_dismiss))
                }
            }
        )
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isEntryInitialized) {
                EventEntrySaveBar(
                    enabled = eventUiState.isEntryValid && !isSaving,
                    isSaving = isSaving,
                    onSaveClick = { requestSave() }
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEditing) stringResource(R.string.title_edit_event)
                        else stringResource(R.string.title_new_event),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = ::requestNavigateBack,
                        enabled = !isSaving
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        if (!isEntryInitialized) {
            Box(
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            EventEntryBody(
                eventUiState = eventUiState,
                onEventValueChange = viewModel::updateUiState,
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEntryBody(
    eventUiState: EventEntryUiState,
    onEventValueChange: (EventDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = EventEntryContentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SongPaperSurface(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EventInputForm(
                        eventDetails = eventUiState.eventDetails,
                        onValueChange = onEventValueChange
                    )
                }
            }

        }
    }
}

@Composable
private fun EventEntrySaveBar(
    enabled: Boolean,
    isSaving: Boolean,
    onSaveClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(
                thickness = SongDesignTokens.BorderWidth.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onSaveClick,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = EventEntryContentMaxWidth),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.button_save_event),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventInputForm(
    eventDetails: EventDetails,
    onValueChange: (EventDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestEventDetails by rememberUpdatedState(eventDetails)
    val calendarPermissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    var pendingReminderEnableAfterPermission by remember { mutableStateOf(false) }
    var pendingScheduleEnableAfterPermission by remember { mutableStateOf(false) }
    var permissionDialog by remember { mutableStateOf<PermissionDialogSpec?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!pendingReminderEnableAfterPermission) {
            return@rememberLauncherForActivityResult
        }
        pendingReminderEnableAfterPermission = false
        if (context.didGrantNotificationPermissionAfterRequest(granted)) {
            onValueChange(latestEventDetails.copy(remindEnabled = true))
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        val granted = context.didGrantCalendarPermissionAfterRequest(grantResults)
        if (pendingScheduleEnableAfterPermission && granted) {
            onValueChange(latestEventDetails.copy(syncToScheduleEnabled = true))
        }
        pendingScheduleEnableAfterPermission = false
    }
    val dateFormatMode by (context.applicationContext as TimeApplication).userPrefs.dateFormatModeFlow.collectAsState(initial = 0)
    val dateFormatter = remember(dateFormatMode) { getDisplayDateFormatter(dateFormatMode) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        BottomSheetDatePicker(
            initialDateMillis = eventDetails.date,
            initialIsLunar = eventDetails.isLunar,
            title = stringResource(R.string.field_date),
            onDismissRequest = { showDatePicker = false },
            onConfirm = { millis, isLunar ->
                onValueChange(
                    eventDetails.copy(
                        date = millis,
                        isLunar = isLunar,
                        repeatType = sanitizeRepeatTypeForLunar(isLunar, eventDetails.repeatType)
                    )
                )
            }
        )
    }
    permissionDialog?.let { dialog ->
        PermissionActionDialog(spec = dialog)
    }

    fun launchNotificationPermissionRequest() {
        pendingReminderEnableAfterPermission = true
        context.markNotificationPermissionRequested()
        notificationPermissionLauncher.launch(notificationRuntimePermissionName())
    }

    fun launchCalendarPermissionRequest() {
        pendingScheduleEnableAfterPermission = true
        context.markCalendarPermissionRequested()
        calendarPermissionLauncher.launch(calendarPermissions)
    }

    fun requestNotificationAccessForToggle() {
        when {
            context.canPostAppNotifications() -> onValueChange(latestEventDetails.copy(remindEnabled = true))
            context.hasNotificationRuntimePermission() &&
                !context.areAppNotificationsEnabledCompat() -> {
                permissionDialog = PermissionDialogSpec(
                    title = context.getString(R.string.permission_dialog_title_notifications),
                    message = context.getString(R.string.notification_permission_settings_message),
                    confirmText = context.getString(R.string.permission_dialog_button_open_settings),
                    dismissText = context.getString(R.string.permission_dialog_button_not_now),
                    onConfirm = {
                        permissionDialog = null
                        context.openAppNotificationSettings()
                    },
                    onDismiss = { permissionDialog = null }
                )
            }
            context.shouldShowNotificationPermissionRationaleCompat() -> {
                permissionDialog = PermissionDialogSpec(
                    title = context.getString(R.string.permission_dialog_title_notifications),
                    message = context.getString(R.string.notification_permission_rationale_message),
                    confirmText = context.getString(R.string.permission_dialog_button_continue),
                    dismissText = context.getString(R.string.permission_dialog_button_not_now),
                    onConfirm = {
                        permissionDialog = null
                        launchNotificationPermissionRequest()
                    },
                    onDismiss = { permissionDialog = null }
                )
            }
            context.wasNotificationPermissionRequestedBefore() -> {
                permissionDialog = PermissionDialogSpec(
                    title = context.getString(R.string.permission_dialog_title_notifications),
                    message = context.getString(R.string.notification_permission_settings_message),
                    confirmText = context.getString(R.string.permission_dialog_button_open_settings),
                    dismissText = context.getString(R.string.permission_dialog_button_not_now),
                    onConfirm = {
                        permissionDialog = null
                        context.openAppNotificationSettings()
                    },
                    onDismiss = { permissionDialog = null }
                )
            }
            else -> launchNotificationPermissionRequest()
        }
    }

    fun requestCalendarAccessForToggle() {
        when {
            context.hasCalendarReadWritePermission() -> {
                onValueChange(latestEventDetails.copy(syncToScheduleEnabled = true))
            }
            context.shouldShowCalendarPermissionRationaleCompat() -> {
                permissionDialog = PermissionDialogSpec(
                    title = context.getString(R.string.permission_dialog_title_calendar),
                    message = context.getString(R.string.calendar_permission_rationale_message),
                    confirmText = context.getString(R.string.permission_dialog_button_continue),
                    dismissText = context.getString(R.string.permission_dialog_button_not_now),
                    onConfirm = {
                        permissionDialog = null
                        launchCalendarPermissionRequest()
                    },
                    onDismiss = { permissionDialog = null }
                )
            }
            context.wasCalendarPermissionRequestedBefore() -> {
                permissionDialog = PermissionDialogSpec(
                    title = context.getString(R.string.permission_dialog_title_calendar),
                    message = context.getString(R.string.calendar_permission_settings_message),
                    confirmText = context.getString(R.string.permission_dialog_button_open_settings),
                    dismissText = context.getString(R.string.permission_dialog_button_not_now),
                    onConfirm = {
                        permissionDialog = null
                        context.openAppDetailsSettings()
                    },
                    onDismiss = { permissionDialog = null }
                )
            }
            else -> launchCalendarPermissionRequest()
        }
    }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
    
    var showCustomColorDialog by remember { mutableStateOf(false) }
    
    if (showCustomColorDialog) {
        CustomColorDialog(
            initialColor = eventDetails.colorHex,
            onColorSelected = { 
                onValueChange(eventDetails.copy(colorHex = it))
                showCustomColorDialog = false
            },
            onDismiss = { showCustomColorDialog = false }
        )
    }

    val haptic = LocalHapticFeedback.current
    val titleTouched = remember { mutableStateOf(false) }
    val showTitleError = titleTouched.value && eventDetails.title.isBlank()
    val reminderDayOptions = remember { (0..3650).toList() }
    val reminderHourOptions = remember { (0..23).toList() }
    val reminderMinuteOptions = remember { (0..59).toList() }
    val selectedRemindDays = eventDetails.remindDaysBefore.coerceIn(
        reminderDayOptions.first(),
        reminderDayOptions.last()
    )
    val selectedRemindHour = (eventDetails.reminderTimeMinutesOfDay / 60).coerceIn(0, 23)
    val selectedRemindMinute = (eventDetails.reminderTimeMinutesOfDay % 60).coerceIn(0, 59)
    val repeatOptions = supportedRepeatTypes(eventDetails.isLunar).map { repeatType ->
        repeatType to when (repeatType) {
            REPEAT_NONE -> stringResource(R.string.repeat_none)
            REPEAT_YEARLY -> stringResource(R.string.repeat_yearly)
            REPEAT_HALF_YEARLY -> stringResource(R.string.repeat_half_yearly)
            REPEAT_MONTHLY -> stringResource(R.string.repeat_monthly)
            REPEAT_WEEKLY -> stringResource(R.string.repeat_weekly)
            REPEAT_DAILY -> stringResource(R.string.repeat_daily)
            else -> repeatType
        }
    }
    var reminderSettingsExpanded by remember { mutableStateOf(false) }
    var showCustomRepeatPicker by remember { mutableStateOf(false) }
    var showCustomRemindDaysPicker by remember { mutableStateOf(false) }
    var showCustomRemindTimePicker by remember { mutableStateOf(false) }

    if (showCustomRepeatPicker) {
        var draftRepeat by remember(eventDetails.repeatType) {
            mutableStateOf(
                repeatOptions.firstOrNull { it.first == eventDetails.repeatType }?.first ?: REPEAT_NONE
            )
        }
        var isRepeatPickerScrolling by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showCustomRepeatPicker = false },
            shape = MaterialTheme.shapes.medium,
            title = { Text(stringResource(R.string.category_custom)) },
            text = {
                SnapWheelPicker(
                    items = repeatOptions.map { it.first },
                    selectedItem = draftRepeat,
                    onItemSelected = { draftRepeat = it },
                    onScrollStateChanged = { isRepeatPickerScrolling = it },
                    modifier = Modifier.fillMaxWidth(),
                    itemLabel = { value ->
                        repeatOptions.firstOrNull { it.first == value }?.second ?: value
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(eventDetails.copy(repeatType = draftRepeat))
                        showCustomRepeatPicker = false
                    },
                    enabled = !isRepeatPickerScrolling
                ) {
                    Text(stringResource(R.string.date_picker_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRepeatPicker = false }) {
                    Text(stringResource(R.string.date_picker_cancel))
                }
            }
        )
    }

    if (showCustomRemindDaysPicker) {
        var draftDays by remember(eventDetails.remindDaysBefore) { mutableStateOf(selectedRemindDays) }
        var isDaysPickerScrolling by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showCustomRemindDaysPicker = false },
            shape = MaterialTheme.shapes.medium,
            title = { Text(stringResource(R.string.custom_remind_days_label)) },
            text = {
                SnapWheelPicker(
                    items = reminderDayOptions,
                    selectedItem = draftDays,
                    onItemSelected = { draftDays = it },
                    onScrollStateChanged = { isDaysPickerScrolling = it },
                    modifier = Modifier.fillMaxWidth(),
                    itemLabel = { days ->
                        if (days == 0) {
                            context.getString(R.string.remind_same_day)
                        } else {
                            context.resources.getQuantityString(
                                R.plurals.remind_days_before_format,
                                days,
                                days
                            )
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(eventDetails.copy(remindDaysBefore = draftDays))
                        showCustomRemindDaysPicker = false
                    },
                    enabled = !isDaysPickerScrolling
                ) {
                    Text(stringResource(R.string.date_picker_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRemindDaysPicker = false }) {
                    Text(stringResource(R.string.date_picker_cancel))
                }
            }
        )
    }

    if (showCustomRemindTimePicker) {
        var draftHour by remember(eventDetails.reminderTimeMinutesOfDay) {
            mutableStateOf(selectedRemindHour)
        }
        var draftMinute by remember(eventDetails.reminderTimeMinutesOfDay) {
            mutableStateOf(selectedRemindMinute)
        }
        var isHourPickerScrolling by remember { mutableStateOf(false) }
        var isMinutePickerScrolling by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showCustomRemindTimePicker = false },
            shape = MaterialTheme.shapes.medium,
            title = { Text(stringResource(R.string.custom_reminder_time_label)) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SnapWheelPicker(
                        items = reminderHourOptions,
                        selectedItem = draftHour,
                        onItemSelected = { draftHour = it },
                        onScrollStateChanged = { isHourPickerScrolling = it },
                        modifier = Modifier.weight(1f),
                        itemLabel = { value -> String.format(Locale.US, "%02d", value) }
                    )
                    SnapWheelPicker(
                        items = reminderMinuteOptions,
                        selectedItem = draftMinute,
                        onItemSelected = { draftMinute = it },
                        onScrollStateChanged = { isMinutePickerScrolling = it },
                        modifier = Modifier.weight(1f),
                        itemLabel = { value -> String.format(Locale.US, "%02d", value) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(
                            eventDetails.copy(reminderTimeMinutesOfDay = draftHour * 60 + draftMinute)
                        )
                        showCustomRemindTimePicker = false
                    },
                    enabled = !isHourPickerScrolling && !isMinutePickerScrolling
                ) {
                    Text(stringResource(R.string.date_picker_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRemindTimePicker = false }) {
                    Text(stringResource(R.string.date_picker_cancel))
                }
            }
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextField(
            value = eventDetails.title,
            onValueChange = {
                titleTouched.value = true
                onValueChange(eventDetails.copy(title = it))
            },
            label = { Text(stringResource(R.string.field_title)) },
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
            singleLine = true,
            isError = showTitleError,
            supportingText = if (showTitleError) {
                { Text(stringResource(R.string.field_title_required)) }
            } else null,
            shape = RoundedCornerShape(4.dp),
            colors = textFieldColors
        )

        // Category selection.
        Text(
            text = stringResource(R.string.field_category),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        val currentCategory = eventDetails.category.takeIf { it in listOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER) } ?: CATEGORY_OTHER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                stringResource(R.string.category_birthday) to CATEGORY_BIRTHDAY,
                stringResource(R.string.category_anniversary) to CATEGORY_ANNIVERSARY,
                stringResource(R.string.category_other) to CATEGORY_OTHER
            ).forEach { (label, value) ->
                SongFilterChip(
                    selected = currentCategory == value,
                    onClick = { onValueChange(eventDetails.copy(category = value)) },
                    label = label
                )
            }
        }
        
        // Date selection and display.
        val baseDate = eventDateToLocalDate(eventDetails.date)
        val dateString = if (eventDetails.isLunar) {
                                            formatLunarDateString(baseDate, context)
        } else {
            baseDate.format(dateFormatter)
        }

        // Use an overlay click target so the read-only field opens the picker reliably.
        Box(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp))) {
            TextField(
                value = dateString,
                onValueChange = { },
                label = { Text(stringResource(R.string.field_date)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                shape = RoundedCornerShape(4.dp),
                colors = textFieldColors
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        showDatePicker = true
                    }
            )
        }

        TextField(
            value = eventDetails.note,
            onValueChange = { onValueChange(eventDetails.copy(note = it)) },
            label = { Text(stringResource(R.string.field_note)) },
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(4.dp),
            colors = textFieldColors
        )

        // 重复设置
        Text(
            text = stringResource(R.string.field_repeat),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeatOptions.forEach { (value, label) ->
                SongFilterChip(
                    selected = eventDetails.repeatType == value,
                    onClick = { onValueChange(eventDetails.copy(repeatType = value)) },
                    label = label
                )
            }
            OutlinedButton(
                onClick = { showCustomRepeatPicker = true },
                shape = MaterialTheme.shapes.small
            ) {
                Text(stringResource(R.string.category_custom))
            }
        }
        
        // Reminder & schedule settings (collapsed secondary section)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { reminderSettingsExpanded = !reminderSettingsExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reminder_and_calendar_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.reminder_and_calendar_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (reminderSettingsExpanded) {
                    Icons.Outlined.KeyboardArrowUp
                } else {
                    Icons.Outlined.KeyboardArrowDown
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (reminderSettingsExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.field_remind),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = eventDetails.remindEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            requestNotificationAccessForToggle()
                        } else {
                            onValueChange(eventDetails.copy(remindEnabled = enabled))
                        }
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sync_to_schedule),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.sync_to_schedule_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = eventDetails.syncToScheduleEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            requestCalendarAccessForToggle()
                        } else {
                            onValueChange(eventDetails.copy(syncToScheduleEnabled = false))
                        }
                    }
                )
            }

            if (eventDetails.remindEnabled) {
                // 提前 X 天：点击整行进入滚轮选择
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomRemindDaysPicker = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.custom_remind_days_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (selectedRemindDays == 0) {
                                stringResource(R.string.remind_same_day)
                            } else {
                                context.resources.getQuantityString(
                                    R.plurals.remind_days_before_format,
                                    selectedRemindDays,
                                    selectedRemindDays
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 提醒时间（小时/分钟）：点击整行进入双列滚轮选择
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomRemindTimePicker = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.custom_reminder_time_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = formatMinutesOfDay(eventDetails.reminderTimeMinutesOfDay),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Color selection.
        Text(
            text = stringResource(R.string.field_card_color),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PRESET_COLORS.forEach { hex ->
                val color = try { Color(hex.toColorInt()) } catch (_: Exception) { Color.Gray }
                val selected = eventDetails.colorHex?.uppercase() == hex.uppercase()
                ColorChip(
                    color = color,
                    selected = selected,
                    onClick = { onValueChange(eventDetails.copy(colorHex = hex)) },
                    contentDescription = stringResource(R.string.cd_event_color_option, hex)
                )
            }
            
            // Custom color display (if selected color is not in presets)
            val isCustomSelected = eventDetails.colorHex != null && !PRESET_COLORS.any { it.equals(eventDetails.colorHex, ignoreCase = true) }
            if (isCustomSelected) {
                val hex = eventDetails.colorHex!!
                val color = try { Color(hex.toColorInt()) } catch (_: Exception) { Color.Gray }
                ColorChip(
                    color = color,
                    selected = true,
                    onClick = { showCustomColorDialog = true },
                    contentDescription = stringResource(R.string.cd_event_color_option, hex)
                )
            }

            // Custom color button
            ColorChip(
                color = MaterialTheme.colorScheme.surfaceVariant,
                selected = false,
                onClick = { showCustomColorDialog = true },
                icon = Icons.Outlined.Palette,
                contentDescription = stringResource(R.string.cd_custom_event_color)
            )

            // No color selected
            val noColorSelected = eventDetails.colorHex == null
            ColorChip(
                color = MaterialTheme.colorScheme.surface,
                selected = noColorSelected,
                onClick = { onValueChange(eventDetails.copy(colorHex = null)) },
                icon = Icons.Outlined.Clear,
                contentDescription = stringResource(R.string.cd_no_event_color)
            )
        }
    }
}

private fun formatMinutesOfDay(minutesOfDay: Int): String {
    val safe = sanitizeReminderTimeMinutesOfDay(minutesOfDay)
    val hour = safe / 60
    val minute = safe % 60
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthQuickSelector(datePickerState: DatePickerState) {
    val displayedDate = remember(datePickerState.displayedMonthMillis) {
        Instant.ofEpochMilli(datePickerState.displayedMonthMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
    }
    val currentYear = displayedDate.year
    val currentMonth = displayedDate.monthValue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..12).forEach { month ->
            val selected = month == currentMonth
            AssistChip(
                onClick = {
                    val targetMonthDate = displayedDate.withYear(currentYear).withMonth(month).withDayOfMonth(1)
                    val targetMillis = targetMonthDate
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                    datePickerState.displayedMonthMillis = targetMillis
                },
                label = { Text(java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    labelColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun ColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val accessibleDescription = if (selected) {
        stringResource(R.string.cd_event_color_selected, contentDescription)
    } else {
        contentDescription
    }
    val scale by animateFloatAsState(
        if (isPressed) 0.9f else 1f,
        animationSpec = AnimationSpecs.springButton,
        label = "colorChipScale"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = accessibleDescription }
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(color, RoundedCornerShape(4.dp))
                .then(
                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong), RoundedCornerShape(4.dp))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (color.luminance() > 0.5f) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun CustomColorDialog(
    initialColor: String?,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hexCode by remember { mutableStateOf(initialColor?.removePrefix("#") ?: "FFFFFF") }
    var isError by remember { mutableStateOf(false) }
    var r by remember { mutableFloatStateOf(1f) }
    var g by remember { mutableFloatStateOf(1f) }
    var b by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(initialColor) {
        val c = try { Color((initialColor ?: "#FFFFFF").toColorInt()) } catch (e: Exception) { Color.White }
        r = c.red
        g = c.green
        b = c.blue
        hexCode = String.format("%02X%02X%02X", (r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    fun updateHexFromRgb() {
        hexCode = String.format("%02X%02X%02X", (r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
        isError = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.medium,
        title = { Text(stringResource(R.string.custom_colors_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color(red = r, green = g, blue = b), RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Slider(value = r, onValueChange = { r = it; updateHexFromRgb() }, colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red.copy(alpha = 0.5f)))
                    Slider(value = g, onValueChange = { g = it; updateHexFromRgb() }, colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green.copy(alpha = 0.5f)))
                    Slider(value = b, onValueChange = { b = it; updateHexFromRgb() }, colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue.copy(alpha = 0.5f)))
                }
                OutlinedTextField(
                    value = hexCode,
                    onValueChange = { newHex ->
                        hexCode = newHex.take(8).uppercase()
                        try {
                            if (hexCode.length == 6 || hexCode.length == 8) {
                                val c = Color("#$hexCode".toColorInt())
                                r = c.red
                                g = c.green
                                b = c.blue
                                isError = false
                            } else {
                                isError = true
                            }
                        } catch (e: Exception) {
                            isError = true
                        }
                    },
                    prefix = { Text("#") },
                    isError = isError,
                    singleLine = true,
                    label = { Text(stringResource(R.string.custom_color_hex_hint)) }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fullHex = "#$hexCode"
                    try {
                        fullHex.toColorInt()
                        onColorSelected(fullHex)
                    } catch (e: Exception) {
                    }
                },
                enabled = !isError
            ) {
                Text(stringResource(R.string.date_picker_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.date_picker_cancel))
            }
        }
    )
}














