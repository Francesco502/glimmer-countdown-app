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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
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

private val PRESET_COLORS = listOf(
    // 宋代美学颜色：用户指定默认色 + 补充色
    "#4A4933", // 沉香
    "#457080", // 景泰蓝
    "#5F856B", // 汁绿
    "#AF4E31", // 丹罽
    "#AC8F62", // 秋香
    "#86351C", // 栗壳
    "#5B8E79", // 蟹壳青
    "#3A4550", // 铁灰
    "#785B64"  // 绛紫
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
                isSaving = true
                coroutineScope.launch {
                    val ok = viewModel.saveEvent()
                    if (ok) navigateBack() else isSaving = false
                }
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
            .background(MaterialTheme.colorScheme.background) // 复古纯色背景
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 表单区域：与页面背景同色，不再使用 surfaceVariant 大面积主色
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
        
        // 底部保存按钮：废弃实心色块，改为纯文字/印章风格
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
    ) { _ -> }
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

        // 分类：生日 / 纪念日 / 其他
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
        
        // 备注输入
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

        // 日期选择：公历 / 农历 显示
        val baseDate = eventDateToLocalDate(eventDetails.date)
        val dateString = if (eventDetails.isLunar) {
            formatLunarDateString(baseDate)
        } else {
            baseDate.format(dateFormatter)
        }

        // 整个日期输入框可点击打开日期选择器，去掉右侧图标
        // 使用上层透明点击层，避免 TextField 自身消费点击事件
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

        // 重复设置
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
                stringResource(R.string.repeat_yearly) to REPEAT_YEARLY,
                stringResource(R.string.repeat_half_yearly) to REPEAT_HALF_YEARLY,
                stringResource(R.string.repeat_monthly) to REPEAT_MONTHLY
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
        
        // 提醒设置
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
            // ... (内部 Chip 同样应用 shape = 4.dp，此处略过细节代码重复，仅做逻辑替换)
            // 实际代码中需要把内部的 FilterChip 也都加上 shape = RoundedCornerShape(4.dp)
            // 为节省篇幅，假设下方 Chip 也已同样处理，或由 default style 覆盖（如果能全局覆盖的话，但 Chip 通常需要显式指定）
            // 这里我们显式写几个关键的
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
                        if (enabled && (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED)) {
                            calendarPermissionLauncher.launch(calendarPermissions)
                        }
                    }
                )
            }
        }
        
        // 颜色选择
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
