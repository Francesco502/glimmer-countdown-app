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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.AppViewModelProvider
import com.example.timeapk.ui.theme.AnimationSpecs
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PRESET_COLORS = listOf(
    "#2196F3", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
    "#00BCD4", "#009688", "#4CAF50", "#FF9800", "#795548"
)
private val CATEGORY_DEFAULT_COLOR = mapOf(
    "生日" to "#E91E63",
    "纪念日" to "#9C27B0",
    "考试" to "#2196F3",
    "节日" to "#4CAF50",
    "其他" to "#795548"
)

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

    LaunchedEffect(eventId) {
        if (eventId != null && eventId != 0) {
            viewModel.loadEvent(eventId)
        } else {
            (context.applicationContext as? TimeApplication)?.initialCategoryForAdd?.let { category ->
                viewModel.updateUiState(eventUiState.eventDetails.copy(category = category, colorHex = CATEGORY_DEFAULT_COLOR[category]))
                (context.applicationContext as? TimeApplication)?.initialCategoryForAdd = null
            }
        }
    }

    val isEditing = eventId != null && eventId != 0
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // 与首页背景一致
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
            onSaveClick = {
                coroutineScope.launch {
                    viewModel.saveEvent()
                    navigateBack()
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
        
        // 底部保存按钮：方形，强调色，按压缩放反馈
        val saveInteractionSource = remember { MutableInteractionSource() }
        val savePressed by saveInteractionSource.collectIsPressedAsState()
        val saveScale by animateFloatAsState(
            if (savePressed) 0.98f else 1f,
            animationSpec = AnimationSpecs.springButton,
            label = "saveScale"
        )
        Button(
            onClick = onSaveClick,
            enabled = eventUiState.isEntryValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer { scaleX = saveScale; scaleY = saveScale },
            interactionSource = saveInteractionSource,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.button_save_event))
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
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = eventDetails.date
    )
    // 修复：编辑事件时，loadEvent 异步完成后 eventDetails.date 变化，
    // 但 rememberDatePickerState 只捕获了初始值。用 LaunchedEffect 同步。
    LaunchedEffect(eventDetails.date) {
        if (datePickerState.selectedDateMillis != eventDetails.date) {
            datePickerState.selectedDateMillis = eventDetails.date
        }
    }

    if (showDatePicker) {
        // 统一 DatePicker 所有区域的背景 & 文字颜色，与首页背景保持一致
        val datePickerColors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            headlineContentColor = MaterialTheme.colorScheme.onBackground,
            weekdayContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            subheadContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            navigationContentColor = MaterialTheme.colorScheme.onBackground,
            yearContentColor = MaterialTheme.colorScheme.onBackground,
            currentYearContentColor = MaterialTheme.colorScheme.primary,
            selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
            selectedYearContainerColor = MaterialTheme.colorScheme.primary,
            dayContentColor = MaterialTheme.colorScheme.onBackground,
            selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
            selectedDayContainerColor = MaterialTheme.colorScheme.primary,
            todayContentColor = MaterialTheme.colorScheme.primary,
            todayDateBorderColor = MaterialTheme.colorScheme.primary,
            dividerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(4.dp), // 直角弹窗
            colors = datePickerColors,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onValueChange(eventDetails.copy(date = it))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.date_picker_ok), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.date_picker_cancel), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        ) {
            // 同样传入 datePickerColors，确保日历网格、头部导航等区域背景一致
            DatePicker(state = datePickerState, colors = datePickerColors)
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 标题输入：直角，透明底，强调边框（文字色由下方 LocalContentColor 提供）
        OutlinedTextField(
            value = eventDetails.title,
            onValueChange = { onValueChange(eventDetails.copy(title = it)) },
            label = { Text(stringResource(R.string.field_title), color = MaterialTheme.colorScheme.onBackground) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(4.dp),
            colors = textFieldColors
        )
        
        // 类别选择（与背景同色，不用主色块）
        Text(
            text = stringResource(R.string.field_category),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                stringResource(R.string.category_birthday) to "生日",
                stringResource(R.string.category_anniversary) to "纪念日",
                stringResource(R.string.category_exam) to "考试",
                stringResource(R.string.category_holiday) to "节日",
                stringResource(R.string.category_other) to "其他"
            ).forEach { (label, value) ->
                FilterChip(
                    selected = eventDetails.category == value,
                    onClick = {
                        val defaultColor = CATEGORY_DEFAULT_COLOR[value]
                        onValueChange(eventDetails.copy(category = value, colorHex = defaultColor ?: eventDetails.colorHex))
                    },
                    label = { Text(label) },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.heightIn(min = 40.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        labelColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
        
        // 自定义类别输入
        OutlinedTextField(
            value = eventDetails.category,
            onValueChange = { onValueChange(eventDetails.copy(category = it)) },
            label = { Text(stringResource(R.string.category_custom), color = MaterialTheme.colorScheme.onBackground) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(4.dp),
            colors = textFieldColors
        )
        
        // 备注输入
        OutlinedTextField(
            value = eventDetails.note,
            onValueChange = { onValueChange(eventDetails.copy(note = it)) },
            label = { Text(stringResource(R.string.field_note), color = MaterialTheme.colorScheme.onBackground) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(4.dp),
            colors = textFieldColors
        )

        // 日期选择
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateString = Instant.ofEpochMilli(eventDetails.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)

        // 整个日期输入框可点击打开日期选择器，去掉右侧图标
        // 使用上层透明点击层，避免 TextField 自身消费点击事件
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dateString,
                onValueChange = { },
                label = { Text(stringResource(R.string.field_date), color = MaterialTheme.colorScheme.onBackground) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                shape = RoundedCornerShape(4.dp),
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
                val selected = eventDetails.colorHex == hex
                ColorChip(
                    color = color,
                    selected = selected,
                    onClick = { onValueChange(eventDetails.copy(colorHex = hex)) }
                )
            }
            val noColorSelected = eventDetails.colorHex == null
            ColorChip(
                color = MaterialTheme.colorScheme.surface,
                selected = noColorSelected,
                onClick = { onValueChange(eventDetails.copy(colorHex = null)) }
            )
        }
    }
}

@Composable
private fun ColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
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
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    )
}
