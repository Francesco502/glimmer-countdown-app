package com.example.timeapk.ui.event

import android.Manifest
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
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
import com.example.timeapk.ui.components.PermissionActionDialog
import com.example.timeapk.ui.components.PermissionDialogSpec
import com.example.timeapk.ui.components.SongDateWheelPickerDialog
import com.example.timeapk.ui.components.SongConfirmDialog
import com.example.timeapk.ui.components.SongDialogButton
import com.example.timeapk.ui.components.SongFormDialog
import com.example.timeapk.ui.components.SongWheelPickerDialog
import com.example.timeapk.ui.components.SnapWheelPicker
import com.example.timeapk.ui.common.SongEventPreviewCard
import com.example.timeapk.ui.settings.ClassicalToggle
import com.example.timeapk.ui.sound.SongSoundEffect
import com.example.timeapk.ui.sound.rememberSongSoundscape
import com.example.timeapk.ui.theme.SongColorSwatch
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongFilterChip
import com.example.timeapk.ui.theme.SongHexColorField
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.SongPaperSurface
import com.example.timeapk.ui.theme.SongPaperTextureOverlay
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.getDisplayDateFormatter
import com.example.timeapk.ui.utils.formatLunarDateString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Locale

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
                    snackbarHostState.showSnackbar(result.message)
                    isSaving = false
                    pendingSaveOrigin = SaveRequestOrigin.Standard
                    navigateBack()
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
        SongConfirmDialog(
            title = stringResource(R.string.discard_changes_dialog_title),
            message = stringResource(R.string.discard_changes_dialog_message),
            confirmText = stringResource(R.string.discard_changes_dialog_confirm),
            dismissText = stringResource(R.string.discard_changes_dialog_dismiss),
            onConfirm = {
                showDiscardChangesDialog = false
                navigateBack()
            },
            onDismiss = { showDiscardChangesDialog = false }
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
    val context = LocalContext.current
    val dateFormatMode by (context.applicationContext as TimeApplication).userPrefs.dateFormatModeFlow.collectAsState(initial = 0)
    val dateFormatter = remember(dateFormatMode) { getDisplayDateFormatter(dateFormatMode) }
    val previewDetails = eventUiState.eventDetails
    val previewDate = eventDateToLocalDate(previewDetails.date)
    val previewDateText = if (previewDetails.isLunar) {
        formatLunarDateString(previewDate, context)
    } else {
        previewDate.format(dateFormatter)
    }
    val previewCategoryLabel = when (previewDetails.category) {
        CATEGORY_BIRTHDAY -> stringResource(R.string.category_birthday)
        CATEGORY_ANNIVERSARY -> stringResource(R.string.category_anniversary)
        else -> stringResource(R.string.category_other)
    }
    val previewColor = remember(previewDetails.colorHex) {
        previewDetails.colorHex?.let { hex ->
            try { Color(hex.toColorInt()) } catch (_: Exception) { null }
        } ?: Color(0xFF83ACA2)
    }
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
            SongEventPreviewCard(
                title = previewDetails.title.ifBlank { stringResource(R.string.event_preview_untitled) },
                categoryLabel = previewCategoryLabel,
                dateText = previewDateText,
                color = previewColor,
                reminderText = if (previewDetails.remindEnabled) {
                    stringResource(R.string.cd_reminder_on)
                } else {
                    stringResource(R.string.cd_reminder_off)
                },
                modifier = Modifier.fillMaxWidth()
            )
            EventInputForm(
                eventDetails = eventUiState.eventDetails,
                onValueChange = onEventValueChange,
                modifier = Modifier.fillMaxWidth()
            )

        }
    }
}

@Composable
private fun EventEntrySaveBar(
    enabled: Boolean,
    isSaving: Boolean,
    onSaveClick: () -> Unit
) {
    val soundscape = rememberSongSoundscape()
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
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
                SongSaveSlip(
                    onClick = {
                        soundscape.play(SongSoundEffect.Commit)
                        onSaveClick()
                    },
                    enabled = enabled,
                    isSaving = isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = EventEntryContentMaxWidth)
                )
            }
        }
    }
}

@Composable
private fun SongSaveSlip(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val label = stringResource(R.string.button_save_event)
    SongPaperSurface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(
                enabled = enabled && !isSaving,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        backgroundColor = if (enabled) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)
        },
        borderColor = if (enabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 1.6.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                SongLineIcon(
                    kind = SongLineIconKind.Seal,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                    },
                    size = 17.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
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
        SongDateWheelPickerDialog(
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

    var showCustomColorDialog by remember { mutableStateOf(false) }
    
    if (showCustomColorDialog) {
        SongColorSpectrumDialog(
            initialColor = eventDetails.colorHex,
            onColorSelected = { 
                onValueChange(eventDetails.copy(colorHex = it))
                showCustomColorDialog = false
            },
            onDismiss = { showCustomColorDialog = false }
        )
    }

    val titleTouched = remember { mutableStateOf(false) }
    val showTitleError = titleTouched.value && eventDetails.title.isBlank()
    val reminderDayOptions = remember { (0..3650).toList() }
    val reminderDayPresets = remember { listOf(0, 1, 7, 30) }
    val reminderHourPresets = remember { listOf(0, 7, 10, 12, 18) }
    val reminderHourOptions = remember { (0..23).toList() }
    val reminderMinuteOptions = remember { (0..59).toList() }
    val selectedRemindDays = eventDetails.remindDaysBefore.coerceIn(0, 3650)
    val selectedRemindHour = (eventDetails.reminderTimeMinutesOfDay / 60).coerceIn(0, 23)
    val selectedRemindMinute = (eventDetails.reminderTimeMinutesOfDay % 60).coerceIn(0, 59)
    fun remindDaysLabel(days: Int): String = formatRemindDaysBefore(days, context)
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
    var showCustomRepeatPicker by remember { mutableStateOf(false) }
    var showCustomRemindDaysPicker by remember { mutableStateOf(false) }
    var showCustomRemindTimePicker by remember { mutableStateOf(false) }
    val currentCategory = eventDetails.category.takeIf {
        it in listOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER)
    } ?: CATEGORY_OTHER

    if (showCustomRepeatPicker) {
        var draftRepeat by remember(eventDetails.repeatType) {
            mutableStateOf(
                repeatOptions.firstOrNull { it.first == eventDetails.repeatType }?.first ?: REPEAT_NONE
            )
        }
        var isRepeatPickerScrolling by remember { mutableStateOf(false) }
        SongWheelPickerDialog(
            onDismissRequest = { showCustomRepeatPicker = false },
            title = stringResource(R.string.category_custom),
            confirmEnabled = !isRepeatPickerScrolling,
            onConfirm = {
                onValueChange(eventDetails.copy(repeatType = draftRepeat))
                showCustomRepeatPicker = false
            }
        ) {
            SnapWheelPicker(
                items = repeatOptions.map { it.first },
                selectedItem = draftRepeat,
                onItemSelected = { draftRepeat = it },
                accessibilityLabel = stringResource(R.string.field_repeat),
                onScrollStateChanged = { isRepeatPickerScrolling = it },
                modifier = Modifier.fillMaxWidth(),
                itemLabel = { value ->
                    repeatOptions.firstOrNull { it.first == value }?.second ?: value
                }
            )
        }
    }

    if (showCustomRemindDaysPicker) {
        var draftDays by remember(eventDetails.remindDaysBefore) {
            mutableStateOf(selectedRemindDays)
        }
        var isDaysPickerScrolling by remember { mutableStateOf(false) }
        SongWheelPickerDialog(
            onDismissRequest = { showCustomRemindDaysPicker = false },
            title = stringResource(R.string.custom_remind_days_label),
            confirmEnabled = !isDaysPickerScrolling,
            onConfirm = {
                onValueChange(eventDetails.copy(remindDaysBefore = draftDays))
                showCustomRemindDaysPicker = false
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReminderPresetChipRow(
                    items = reminderDayPresets,
                    isSelected = { days -> days == draftDays },
                    onSelected = { days -> draftDays = days },
                    itemLabel = { days -> remindDaysLabel(days) }
                )
                SnapWheelPicker(
                    items = reminderDayOptions,
                    selectedItem = draftDays,
                    onItemSelected = { draftDays = it },
                    accessibilityLabel = stringResource(R.string.custom_remind_days_label),
                    onScrollStateChanged = { isDaysPickerScrolling = it },
                    modifier = Modifier.fillMaxWidth(),
                    itemLabel = { days -> remindDaysLabel(days) }
                )
            }
        }
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
        SongWheelPickerDialog(
            onDismissRequest = { showCustomRemindTimePicker = false },
            title = stringResource(R.string.custom_reminder_time_label),
            confirmEnabled = !isHourPickerScrolling && !isMinutePickerScrolling,
            onConfirm = {
                onValueChange(
                    eventDetails.copy(reminderTimeMinutesOfDay = draftHour * 60 + draftMinute)
                )
                showCustomRemindTimePicker = false
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReminderPresetChipRow(
                    items = reminderHourPresets,
                    isSelected = { hour -> hour == draftHour && draftMinute == 0 },
                    onSelected = { hour ->
                        draftHour = hour
                        draftMinute = 0
                    },
                    itemLabel = { hour -> stringResource(R.string.reminder_hour_preset_format, hour) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SnapWheelPicker(
                        items = reminderHourOptions,
                        selectedItem = draftHour,
                        onItemSelected = { draftHour = it },
                        accessibilityLabel = stringResource(R.string.reminder_time_hour),
                        onScrollStateChanged = { isHourPickerScrolling = it },
                        modifier = Modifier.weight(1f),
                        itemLabel = { value -> String.format(Locale.US, "%02d", value) }
                    )
                    SnapWheelPicker(
                        items = reminderMinuteOptions,
                        selectedItem = draftMinute,
                        onItemSelected = { draftMinute = it },
                        accessibilityLabel = stringResource(R.string.reminder_time_minute),
                        onScrollStateChanged = { isMinutePickerScrolling = it },
                        modifier = Modifier.weight(1f),
                        itemLabel = { value -> String.format(Locale.US, "%02d", value) }
                    )
                }
            }
        }
    }

    val baseDate = eventDateToLocalDate(eventDetails.date)
    val dateString = if (eventDetails.isLunar) {
        formatLunarDateString(baseDate, context)
    } else {
        baseDate.format(dateFormatter)
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SongInkSection(
            title = stringResource(R.string.event_entry_section_time)
        ) {
            SongInkDateRow(
                value = dateString,
                contentDescription = stringResource(R.string.field_date),
                onClick = { showDatePicker = true }
            )
        }

        SongInkSection(
            title = stringResource(R.string.event_entry_section_content)
        ) {
            SongInkTextField(
                value = eventDetails.title,
                onValueChange = {
                    titleTouched.value = true
                    onValueChange(eventDetails.copy(title = it))
                },
                label = stringResource(R.string.field_title),
                singleLine = true,
                requestInitialFocus = eventDetails.id == 0 && eventDetails.title.isBlank(),
                isError = showTitleError,
                errorText = stringResource(R.string.field_title_required)
            )
            SongInkChoiceRow(
                label = stringResource(R.string.field_category),
                value = when (currentCategory) {
                    CATEGORY_BIRTHDAY -> stringResource(R.string.category_birthday)
                    CATEGORY_ANNIVERSARY -> stringResource(R.string.category_anniversary)
                    else -> stringResource(R.string.category_other)
                }
            ) {
                listOf(
                    stringResource(R.string.category_birthday) to CATEGORY_BIRTHDAY,
                    stringResource(R.string.category_anniversary) to CATEGORY_ANNIVERSARY,
                    stringResource(R.string.category_other) to CATEGORY_OTHER
                ).forEach { (label, value) ->
                    SongFilterChip(
                        selected = currentCategory == value,
                        onClick = { onValueChange(applyTemplateForCategory(eventDetails, value)) },
                        label = label
                    )
                }
            }
            SongInkTextField(
                value = eventDetails.note,
                onValueChange = { onValueChange(eventDetails.copy(note = it)) },
                label = stringResource(R.string.field_note),
                minLines = 2,
                maxLines = 4
            )
        }

        SongInkSection(
            title = stringResource(R.string.event_entry_section_reminder)
        ) {
            SongInkChoiceRow(
                label = stringResource(R.string.field_repeat),
                value = repeatOptions.firstOrNull { it.first == eventDetails.repeatType }?.second ?: eventDetails.repeatType
            ) {
                repeatOptions.forEach { (value, label) ->
                    SongFilterChip(
                        selected = eventDetails.repeatType == value,
                        onClick = { onValueChange(eventDetails.copy(repeatType = value)) },
                        label = label
                    )
                }
                SongFilterChip(
                    selected = false,
                    onClick = { showCustomRepeatPicker = true },
                    label = stringResource(R.string.category_custom),
                    selectionRole = null
                )
            }
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
                ClassicalToggle(
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
                }
                ClassicalToggle(
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
                SongInkChoiceRow(
                    label = stringResource(R.string.custom_remind_days_label),
                    value = remindDaysLabel(selectedRemindDays),
                    onClick = { showCustomRemindDaysPicker = true }
                )
                SongInkChoiceRow(
                    label = stringResource(R.string.custom_reminder_time_label),
                    value = formatMinutesOfDay(eventDetails.reminderTimeMinutesOfDay),
                    onClick = { showCustomRemindTimePicker = true }
                )
            }
        }

        SongInkSection(
            title = stringResource(R.string.event_entry_section_appearance)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                songNamedColors.forEach { namedColor ->
                    val hex = namedColor.hex
                    val color = try { Color(hex.toColorInt()) } catch (_: Exception) { Color.Gray }
                    val selected = eventDetails.colorHex?.uppercase() == hex.uppercase()
                    SongColorSwatch(
                        color = color,
                        selected = selected,
                        onClick = { onValueChange(eventDetails.copy(colorHex = hex)) },
                        contentDescription = stringResource(
                            R.string.cd_event_color_option,
                            songColorDisplayName(namedColor.nameKey)
                        )
                    )
                }

                val noColorSelected = eventDetails.colorHex == null
                SongColorSwatch(
                    color = MaterialTheme.colorScheme.surface,
                    selected = noColorSelected,
                    onClick = { onValueChange(eventDetails.copy(colorHex = null)) },
                    contentDescription = stringResource(R.string.cd_no_event_color)
                )
            }
            val isCustomSelected = eventDetails.colorHex != null && !songNamedColors.any { it.hex.equals(eventDetails.colorHex, ignoreCase = true) }
            if (isCustomSelected) {
                Text(
                    text = stringResource(R.string.event_entry_custom_color_active, eventDetails.colorHex.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = { showCustomColorDialog = true },
                modifier = Modifier.align(Alignment.Start)
            ) {
                SongLineIcon(
                    kind = SongLineIconKind.Palette,
                    contentDescription = null,
                    size = 16.dp,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.custom_colors_title))
            }
        }
    }
}

@Composable
private fun <T> ReminderPresetChipRow(
    items: List<T>,
    isSelected: (T) -> Boolean,
    onSelected: (T) -> Unit,
    itemLabel: @Composable (T) -> String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            SongFilterChip(
                selected = isSelected(item),
                onClick = { onSelected(item) },
                label = itemLabel(item)
            )
        }
    }
}

private fun formatRemindDaysBefore(days: Int, context: Context): String {
    val safeDays = days.coerceIn(0, 3650)
    return if (safeDays == 0) {
        context.getString(R.string.remind_same_day)
    } else if (safeDays == 1) {
        context.getString(R.string.remind_one_day_before)
    } else {
        context.resources.getQuantityString(
            R.plurals.remind_days_before_format,
            safeDays,
            safeDays
        )
    }
}

private fun formatMinutesOfDay(minutesOfDay: Int): String {
    val safe = sanitizeReminderTimeMinutesOfDay(minutesOfDay)
    val hour = safe / 60
    val minute = safe % 60
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}

@Composable
private fun songColorDisplayName(nameKey: String): String {
    return when (nameKey) {
        "ink" -> stringResource(R.string.song_color_ink)
        "dailan" -> stringResource(R.string.song_color_indigo)
        "pine_green" -> stringResource(R.string.song_color_pine_green)
        "celadon" -> stringResource(R.string.song_color_ru_celadon)
        "cinnabar" -> stringResource(R.string.song_color_cinnabar)
        "ochre" -> stringResource(R.string.song_color_ocher)
        "old_gold" -> stringResource(R.string.song_color_old_gold)
        "tea_brown" -> stringResource(R.string.song_color_tea_brown)
        "lotus_mauve" -> stringResource(R.string.song_color_lotus_mauve)
        else -> nameKey
    }
}

@Composable
private fun SongInkSection(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f), shape)
            .border(
                width = SongDesignTokens.BorderWidth.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                shape = shape
            )
    ) {
        SongPaperTextureOverlay(
            modifier = Modifier.matchParentSize(),
            paperTextureAlpha = 0.012f
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(SongDesignTokens.BorderWidth.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
        )
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 15.dp, bottom = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HorizontalDivider(
                thickness = SongDesignTokens.BorderWidth.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun SongInkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    requestInitialFocus: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val clickInteractionSource = remember { MutableInteractionSource() }
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    val requestInputFocus = {
        focusRequester.requestFocus()
        keyboardController?.show()
        view.post {
            val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            @Suppress("DEPRECATION")
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_FORCED)
        }
    }

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            requestInputFocus()
        }
    }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = clickInteractionSource,
                indication = null
            ) {
                requestInputFocus()
            }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = fieldValue,
            onValueChange = { nextValue ->
                fieldValue = nextValue
                if (nextValue.text != value) {
                    onValueChange(nextValue.text)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .border(
                    SongDesignTokens.BorderWidth.dp,
                    if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong),
                    RoundedCornerShape(SongDesignTokens.StandardRadius.dp)
                ),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            isError = isError,
            shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        if (isError && !errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SongInkDateRow(
    value: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SongInkChoiceRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    choices: (@Composable RowScope.() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        choices?.let { choiceContent ->
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = choiceContent
            )
        }
    }
}

@Composable
fun SongColorSpectrumDialog(
    initialColor: String?,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hexCode by remember { mutableStateOf(initialColor?.removePrefix("#") ?: "FFFFFF") }
    var isError by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialColor) {
        hexCode = initialColor?.removePrefix("#")?.uppercase()?.take(8) ?: "FFFFFF"
        isError = false
    }

    SongFormDialog(
        title = stringResource(R.string.custom_colors_title),
        onDismissRequest = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    songNamedColors.forEach { namedColor ->
                        val color = try { Color(namedColor.hex.toColorInt()) } catch (_: Exception) { Color.Gray }
                        SongColorSwatch(
                            color = color,
                            selected = namedColor.hex.equals(initialColor, ignoreCase = true),
                            onClick = { onColorSelected(namedColor.hex) },
                            contentDescription = stringResource(
                                R.string.cd_event_color_option,
                                songColorDisplayName(namedColor.nameKey)
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { advancedExpanded = !advancedExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.event_entry_custom_color_advanced),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SongLineIcon(
                        kind = if (advancedExpanded) SongLineIconKind.ChevronUp else SongLineIconKind.ChevronDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 18.dp
                    )
                }
                if (advancedExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SongHexColorField(
                            value = hexCode,
                            onValueChange = { newHex ->
                                hexCode = newHex.take(8).uppercase()
                                isError = try {
                                    hexCode.length !in listOf(6, 8) || run {
                                        Color("#$hexCode".toColorInt())
                                        false
                                    }
                                } catch (_: Exception) {
                                    true
                                }
                            },
                            isError = isError,
                            label = stringResource(R.string.custom_color_hex_hint),
                            previewColor = try {
                                Color("#$hexCode".toColorInt())
                            } catch (_: Exception) {
                                null
                            }
                        )
                    }
                }
            }
        },
        buttons = {
            SongDialogButton(
                text = stringResource(R.string.date_picker_cancel),
                onClick = onDismiss
            )
            Spacer(modifier = Modifier.width(4.dp))
            SongDialogButton(
                text = stringResource(R.string.date_picker_ok),
                onClick = {
                    val fullHex = "#$hexCode"
                    try {
                        fullHex.toColorInt()
                        onColorSelected(fullHex)
                    } catch (_: Exception) {
                        isError = true
                    }
                },
                enabled = advancedExpanded && !isError
            )
        }
    )
}
