package com.example.timeapk.ui.event

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.timeapk.data.sanitizeRemindDaysBefore
import com.example.timeapk.data.sanitizeReminderTimeMinutesOfDay
import com.example.timeapk.ui.AppViewModelProvider
import com.example.timeapk.ui.components.BottomSheetDatePicker
import com.example.timeapk.ui.theme.AnimationSpecs
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
    // 瀹嬩唬缇庡棰滆壊锛氱敤鎴锋寚瀹氶粯璁よ壊 + 琛ュ厖鑹?
    "#4A4933", // 娌夐
    "#457080", // 鏅嘲钃?
    "#5F856B", // 姹佺豢
    "#AF4E31", // 涓圭浇
    "#AC8F62", // 绉嬮
    "#86351C", // 鏍楀３
    "#5B8E79", // 锜瑰３闈?
    "#3A4550", // 閾佺伆
    "#785B64"  // 缁涚传
)
// CATEGORY_DEFAULT_COLOR map removed as explicit category selection is gone

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
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val calendarPermissions = remember {
        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }
    var pendingSaveAfterCalendarPermission by remember { mutableStateOf(false) }
    LaunchedEffect(eventId) {
        if (eventId != null && eventId != 0) {
            viewModel.loadEvent(eventId)
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
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    fun launchSave() {
        coroutineScope.launch {
            when (val result = viewModel.saveEvent()) {
                is SaveEventResult.Success -> navigateBack()
                is SaveEventResult.PartialSuccess -> {
                    isSaving = false
                    snackbarHostState.showSnackbar(result.message)
                }
                is SaveEventResult.Failure -> {
                    isSaving = false
                    snackbarHostState.showSnackbar(result.message)
                }
            }
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        val granted =
            grantResults[Manifest.permission.READ_CALENDAR] == true &&
                grantResults[Manifest.permission.WRITE_CALENDAR] == true

        if (!pendingSaveAfterCalendarPermission) {
            return@rememberLauncherForActivityResult
        }

        pendingSaveAfterCalendarPermission = false
        if (granted) {
            launchSave()
        } else {
            isSaving = false
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.calendar_permission_required_for_sync)
                )
            }
        }
    }

    val isEditing = eventId != null && eventId != 0
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    IconButton(onClick = navigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
        EventEntryBody(
            eventUiState = eventUiState,
            onEventValueChange = viewModel::updateUiState,
            isSaving = isSaving,
            onSaveClick = {
                if (isSaving) return@EventEntryBody

                val details = eventUiState.eventDetails
                val shouldCheckCalendarPermission = details.syncToScheduleEnabled
                if (shouldCheckCalendarPermission && !hasCalendarPermission()) {
                    isSaving = true
                    pendingSaveAfterCalendarPermission = true
                    calendarPermissionLauncher.launch(calendarPermissions)
                    return@EventEntryBody
                }

                isSaving = true
                launchSave()
            },
            modifier = modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEntryBody(
    eventUiState: EventEntryUiState,
    onEventValueChange: (EventDetails) -> Unit,
    onSaveClick: () -> Unit,
    isSaving: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .background(MaterialTheme.colorScheme.background) // 澶嶅彜绾壊鑳屾櫙
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 琛ㄥ崟鍖哄煙锛氫笌椤甸潰鑳屾櫙鍚岃壊锛屼笉鍐嶄娇鐢?surfaceVariant 澶ч潰绉富鑹?
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
        
        // 搴曢儴淇濆瓨鎸夐挳锛氬簾寮冨疄蹇冭壊鍧楋紝鏀逛负绾枃瀛?鍗扮珷椋庢牸
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onSaveClick,
                enabled = eventUiState.isEntryValid && !isSaving,
                modifier = Modifier.padding(top = 16.dp),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.button_save_event),
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = 2.sp
                    ),
                    color = if (eventUiState.isEntryValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
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
    val calendarPermissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        val granted =
            grantResults[Manifest.permission.READ_CALENDAR] == true &&
                grantResults[Manifest.permission.WRITE_CALENDAR] == true
        if (!granted && eventDetails.syncToScheduleEnabled) {
            onValueChange(eventDetails.copy(syncToScheduleEnabled = false))
        }
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
                onValueChange(eventDetails.copy(date = millis, isLunar = isLunar))
            }
        )
    }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        disabledIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
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

    var customRemindDaysInput by remember { mutableStateOf(eventDetails.remindDaysBefore.toString()) }
    var customRemindTimeInput by remember { mutableStateOf(formatMinutesOfDay(eventDetails.reminderTimeMinutesOfDay)) }
    var customRemindTimeError by remember { mutableStateOf(false) }

    LaunchedEffect(eventDetails.remindDaysBefore) {
        customRemindDaysInput = eventDetails.remindDaysBefore.toString()
    }
    LaunchedEffect(eventDetails.reminderTimeMinutesOfDay) {
        customRemindTimeInput = formatMinutesOfDay(eventDetails.reminderTimeMinutesOfDay)
    }

    val titleTouched = remember { mutableStateOf(false) }
    val showTitleError = titleTouched.value && eventDetails.title.isBlank()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextField(
            value = eventDetails.title,
            onValueChange = {
                titleTouched.value = true
                onValueChange(eventDetails.copy(title = it))
            },
            label = { Text(stringResource(R.string.field_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = showTitleError,
            supportingText = if (showTitleError) {
                { Text(stringResource(R.string.field_title_required)) }
            } else null,
            shape = RoundedCornerShape(0.dp),
            colors = textFieldColors
        )

        // 鍒嗙被锛氱敓鏃?/ 绾康鏃?/ 鍏朵粬
        Text(
            text = stringResource(R.string.field_category),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        val categoryChipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
            labelColor = MaterialTheme.colorScheme.onBackground
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
                FilterChip(
                    selected = currentCategory == value,
                    onClick = { onValueChange(eventDetails.copy(category = value)) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(4.dp),
                    colors = categoryChipColors
                )
            }
        }
        
        // 澶囨敞杈撳叆
        TextField(
            value = eventDetails.note,
            onValueChange = { onValueChange(eventDetails.copy(note = it)) },
            label = { Text(stringResource(R.string.field_note)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(0.dp),
            colors = textFieldColors
        )

        TextField(
            value = eventDetails.tags,
            onValueChange = { onValueChange(eventDetails.copy(tags = it)) },
            label = { Text(stringResource(R.string.field_tags)) },
            placeholder = { Text(stringResource(R.string.field_tags_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(0.dp),
            colors = textFieldColors
        )

        // 鏃ユ湡閫夋嫨锛氬叕鍘?/ 鍐滃巻 鏄剧ず
        val baseDate = eventDateToLocalDate(eventDetails.date)
        val dateString = if (eventDetails.isLunar) {
            formatLunarDateString(baseDate)
        } else {
            baseDate.format(dateFormatter)
        }

        // 鏁翠釜鏃ユ湡杈撳叆妗嗗彲鐐瑰嚮鎵撳紑鏃ユ湡閫夋嫨鍣紝鍘绘帀鍙充晶鍥炬爣
        // 浣跨敤涓婂眰閫忔槑鐐瑰嚮灞傦紝閬垮厤 TextField 鑷韩娑堣垂鐐瑰嚮浜嬩欢
        Box(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = dateString,
                onValueChange = { },
                label = { Text(stringResource(R.string.field_date)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                shape = RoundedCornerShape(0.dp),
                colors = textFieldColors
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showDatePicker = true
                    }
            )
        }

        // 閲嶅璁剧疆
        Text(
            text = stringResource(R.string.field_repeat),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        val formChipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
            labelColor = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                stringResource(R.string.repeat_none) to REPEAT_NONE,
                stringResource(R.string.repeat_daily) to REPEAT_DAILY,
                stringResource(R.string.repeat_weekly) to REPEAT_WEEKLY,
                stringResource(R.string.repeat_monthly) to REPEAT_MONTHLY,
                stringResource(R.string.repeat_half_yearly) to REPEAT_HALF_YEARLY,
                stringResource(R.string.repeat_yearly) to REPEAT_YEARLY
            ).forEach { (label, value) ->
                FilterChip(
                    selected = eventDetails.repeatType == value,
                    onClick = { onValueChange(eventDetails.copy(repeatType = value)) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(4.dp),
                    colors = formChipColors
                )
            }
        }
        
        // 鎻愰啋璁剧疆
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
                onCheckedChange = { onValueChange(eventDetails.copy(remindEnabled = it)) }
            )
        }
        if (eventDetails.remindEnabled) {
            // ... (鍐呴儴 Chip 鍚屾牱搴旂敤 shape = 4.dp锛屾澶勭暐杩囩粏鑺備唬鐮侀噸澶嶏紝浠呭仛閫昏緫鏇挎崲)
            // 瀹為檯浠ｇ爜涓渶瑕佹妸鍐呴儴鐨?FilterChip 涔熼兘鍔犱笂 shape = RoundedCornerShape(4.dp)
            // 涓鸿妭鐪佺瘒骞咃紝鍋囪涓嬫柟 Chip 涔熷凡鍚屾牱澶勭悊锛屾垨鐢?default style 瑕嗙洊锛堝鏋滆兘鍏ㄥ眬瑕嗙洊鐨勮瘽锛屼絾 Chip 閫氬父闇€瑕佹樉寮忔寚瀹氾級
            // 杩欓噷鎴戜滑鏄惧紡鍐欏嚑涓叧閿殑
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                 listOf(
                    stringResource(R.string.remind_same_day) to 0,
                    stringResource(R.string.remind_1_day) to 1,
                    stringResource(R.string.remind_3_days) to 3,
                    stringResource(R.string.remind_7_days) to 7
                ).forEach { (label, days) ->
                    FilterChip(
                        selected = eventDetails.remindDaysBefore == days,
                        onClick = { onValueChange(eventDetails.copy(remindDaysBefore = days)) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(4.dp),
                        colors = formChipColors
                    )
                }
            }
            Text(
                text = stringResource(R.string.custom_remind_days_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customRemindDaysInput,
                    onValueChange = { customRemindDaysInput = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.custom_remind_days_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp)
                )
                OutlinedButton(
                    onClick = {
                        val parsed = customRemindDaysInput.toIntOrNull() ?: return@OutlinedButton
                        onValueChange(eventDetails.copy(remindDaysBefore = sanitizeRemindDaysBefore(parsed)))
                    },
                    enabled = customRemindDaysInput.isNotBlank(),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(stringResource(R.string.action_apply))
                }
            }

             Text(
                text = stringResource(R.string.reminder_time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    stringResource(R.string.reminder_time_0) to 0,
                    stringResource(R.string.reminder_time_6) to 360,
                    stringResource(R.string.reminder_time_8) to 480,
                    stringResource(R.string.reminder_time_9) to 540,
                    stringResource(R.string.reminder_time_12) to 720,
                    stringResource(R.string.reminder_time_18) to 1080
                ).forEach { (label, minutes) ->
                    FilterChip(
                        selected = eventDetails.reminderTimeMinutesOfDay == minutes,
                        onClick = { onValueChange(eventDetails.copy(reminderTimeMinutesOfDay = minutes)) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(4.dp),
                        colors = formChipColors
                    )
                }
            }
            Text(
                text = stringResource(R.string.custom_reminder_time_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customRemindTimeInput,
                    onValueChange = {
                        customRemindTimeInput = it.filter { c -> c.isDigit() || c == ':' }.take(5)
                        customRemindTimeError = false
                    },
                    label = { Text(stringResource(R.string.custom_reminder_time_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = customRemindTimeError,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp)
                )
                OutlinedButton(
                    onClick = {
                        val parsed = parseReminderTimeInput(customRemindTimeInput)
                        if (parsed == null) {
                            customRemindTimeError = true
                        } else {
                            customRemindTimeError = false
                            onValueChange(
                                eventDetails.copy(
                                    reminderTimeMinutesOfDay = sanitizeReminderTimeMinutesOfDay(parsed)
                                )
                            )
                        }
                    },
                    enabled = customRemindTimeInput.isNotBlank(),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(stringResource(R.string.action_apply))
                }
            }
            if (customRemindTimeError) {
                Text(
                    text = stringResource(R.string.custom_reminder_time_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
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
                        onValueChange(eventDetails.copy(syncToScheduleEnabled = enabled))
                        val readGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CALENDAR
                        ) == PackageManager.PERMISSION_GRANTED
                        val writeGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_CALENDAR
                        ) == PackageManager.PERMISSION_GRANTED
                        if (enabled && (!readGranted || !writeGranted)) {
                            calendarPermissionLauncher.launch(calendarPermissions)
                        }
                    }
                )
            }
        }
        
        // 棰滆壊閫夋嫨
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
                val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                val selected = eventDetails.colorHex?.uppercase() == hex.uppercase()
                ColorChip(
                    color = color,
                    selected = selected,
                    onClick = { onValueChange(eventDetails.copy(colorHex = hex)) }
                )
            }
            
            // Custom color display (if selected color is not in presets)
            val isCustomSelected = eventDetails.colorHex != null && !PRESET_COLORS.any { it.equals(eventDetails.colorHex, ignoreCase = true) }
            if (isCustomSelected) {
                val hex = eventDetails.colorHex!!
                val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
                ColorChip(
                    color = color,
                    selected = true,
                    onClick = { showCustomColorDialog = true }
                )
            }

            // Custom color button
            ColorChip(
                color = MaterialTheme.colorScheme.surfaceVariant,
                selected = false,
                onClick = { showCustomColorDialog = true },
                icon = Icons.Default.Palette
            )

            // No color selected
            val noColorSelected = eventDetails.colorHex == null
            ColorChip(
                color = MaterialTheme.colorScheme.surface,
                selected = noColorSelected,
                onClick = { onValueChange(eventDetails.copy(colorHex = null)) },
                icon = Icons.Default.Clear
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

private fun parseReminderTimeInput(input: String): Int? {
    val raw = input.trim()
    if (raw.isBlank()) return null

    val parsed = if (raw.contains(":")) {
        val parts = raw.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        hour to minute
    } else {
        val digits = raw.filter { it.isDigit() }
        when (digits.length) {
            1, 2 -> (digits.toIntOrNull() ?: return null) to 0
            3 -> {
                val hour = digits.substring(0, 1).toIntOrNull() ?: return null
                val minute = digits.substring(1, 3).toIntOrNull() ?: return null
                hour to minute
            }
            4 -> {
                val hour = digits.substring(0, 2).toIntOrNull() ?: return null
                val minute = digits.substring(2, 4).toIntOrNull() ?: return null
                hour to minute
            }
            else -> return null
        }
    }

    val (hour, minute) = parsed
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
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
                label = { Text("${month}月") },
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
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.9f else 1f,
        animationSpec = AnimationSpecs.springButton,
        label = "colorChipScale"
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(color, RoundedCornerShape(4.dp))
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f)
            )
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
    
    LaunchedEffect(hexCode) {
        isError = try {
            if (hexCode.length == 6 || hexCode.length == 8) {
                android.graphics.Color.parseColor("#$hexCode")
                false
            } else true
        } catch (e: Exception) {
            true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_colors_title)) },
        text = {
            OutlinedTextField(
                value = hexCode,
                onValueChange = { hexCode = it.take(8).uppercase() },
                prefix = { Text("#") },
                isError = isError,
                singleLine = true,
                label = { Text(stringResource(R.string.custom_color_hex_hint)) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fullHex = "#$hexCode"
                    try {
                        android.graphics.Color.parseColor(fullHex)
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


















