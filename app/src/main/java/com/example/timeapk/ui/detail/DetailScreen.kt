package com.example.timeapk.ui.detail

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.REPEAT_NONE
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.border
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.milestoneLabel
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDaysSmart
import com.example.timeapk.ui.utils.getDisplayDateFormatter
import com.example.timeapk.ui.utils.parseEventColorOrFallback
import com.example.timeapk.ui.utils.formatDateWithWeekday
import com.example.timeapk.ui.utils.formatLunarLine
import com.example.timeapk.ui.utils.formatElapsedLiterary
import com.example.timeapk.ui.utils.formatElapsedDays
import com.example.timeapk.ui.utils.nextOccurrenceDate
import com.example.timeapk.ui.utils.ageInYears
import com.example.timeapk.ui.utils.agePeriod
import com.example.timeapk.ui.utils.constellationFromDate
import com.example.timeapk.ui.utils.zodiacAnimalFromDate
import com.example.timeapk.ui.utils.baziFromDate
import com.example.timeapk.ui.utils.wuxingFromDate
import com.example.timeapk.ui.utils.eventDateToLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.Period
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    eventState: EventUiState?,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as TimeApplication).userPrefs
    val scope = rememberCoroutineScope()
    val showMilestone by prefs.showMilestoneFlow.collectAsState(initial = true)
    val dateFormatMode by prefs.dateFormatModeFlow.collectAsState(initial = 0)
    val dateDeltaDisplayMode by prefs.dateDeltaDisplayModeFlow.collectAsState(initial = 0)
    val perEventDateDeltaModes by prefs.perEventDateDeltaDisplayModesFlow.collectAsState(initial = emptyMap())
    val dateFormatter = remember(dateFormatMode) { getDisplayDateFormatter(dateFormatMode) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (eventState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
        return
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message, eventState.event.title)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick()
                    showDeleteConfirm = false
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.delete_confirm_ok), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.delete_confirm_cancel))
                }
            }
        )
    }

    val detailBaseColor = parseEventColorOrFallback(
        hex = eventState.event.colorHex,
        fallback = MaterialTheme.colorScheme.primary
    )
    
    // 融入主题色：使浅色不那么刺眼（像染色的宣纸），深色与背景有一定区分（像带底色的墨锭）
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val detailCardColor = androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.surface,
        detailBaseColor,
        if (isDark) 0.15f else 0.08f
    )
    val detailContentColor = MaterialTheme.colorScheme.onSurface

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // 与其他页面背景一致
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.detail_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
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
        val scrollState = rememberScrollState()
        // 修复：visible=true 首次组合不播放动画，需要从 false→true 过渡
        var contentVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { contentVisible = true }
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = AnimationSpecs.mediumTween()) + slideInVertically(animationSpec = AnimationSpecs.mediumTweenIntOffset()) { it / 8 }
        ) {
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // 复古场记板/老黄历风格卡片 -> 宋代书画样式
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp), // 稍微增加圆角
                colors = CardDefaults.cardColors(
                    containerColor = detailCardColor
                ),
                border = BorderStroke(
                    width = 1.dp, // 加深一点边框
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // 增加阴影以增强背景对比
            ) {
                Column(
                    modifier = Modifier.padding(32.dp), // 增加内部留白
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部：日期与类别（与设置中日期格式一致）
                    val today = LocalDate.now()
                    val targetLocalDate = eventDateToLocalDate(eventState.event.date)
                    val isYearly = eventState.event.repeatType == REPEAT_YEARLY
                    val isAnniversary = isYearly
                    val effectiveCategory = eventState.event.category.takeIf { it in listOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER) } ?: CATEGORY_OTHER
                    val dateStr = targetLocalDate.format(dateFormatter)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 印章组件展示分类
                            val categoryName = when (eventState.event.category) {
                                CATEGORY_BIRTHDAY -> stringResource(R.string.category_birthday)
                                CATEGORY_ANNIVERSARY -> stringResource(R.string.category_anniversary)
                                CATEGORY_OTHER -> stringResource(R.string.category_other)
                                else -> eventState.event.category
                            }
                            Box(
                                modifier = Modifier
                                    .border(
                                        BorderStroke(1.dp, detailBaseColor.copy(alpha = 0.8f)),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = detailBaseColor
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = detailContentColor.copy(alpha = 0.75f)
                            )
                        }
                        
                        Icon(
                            imageVector = if (eventState.event.remindEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = if (eventState.event.remindEnabled) stringResource(R.string.cd_reminder_on) else stringResource(R.string.cd_reminder_off),
                            modifier = Modifier.size(20.dp),
                            tint = if (eventState.event.remindEnabled) detailBaseColor else detailContentColor.copy(alpha = 0.35f)
                        )
                    }
                    
                    // 古典两端渐隐分隔线
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .height(1.dp)
                    ) {
                        val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                detailContentColor.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                        drawLine(
                            brush = brush,
                            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                            strokeWidth = 1f
                        )
                    }

                    // 核心：标题与倒计时
                    Text(
                        text = eventState.event.title,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                            letterSpacing = 2.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = detailContentColor.copy(alpha = 0.9f),
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))

                    // 按类别区分：纪念日只显示「已历」，生日/其他只显示「尚余」（剩余按用户设定的重复计算）
                    val isToday = eventState.daysRemaining == 0L && !eventState.isPast
                    val todayLabel = stringResource(R.string.days_today_label)
                    val isRepeating = eventState.event.repeatType != REPEAT_NONE
                    val isAnniversaryCategory = effectiveCategory == CATEGORY_ANNIVERSARY

                    var labelText = ""
                    var dayCount = 0L
                    var ymdStart = today
                    var ymdEnd = today
                    when {
                        isToday -> {
                            labelText = ""
                            dayCount = 0L
                            ymdStart = today
                            ymdEnd = today
                        }
                        isAnniversaryCategory -> {
                            labelText = stringResource(R.string.detail_repeat_elapsed)
                            dayCount = eventState.daysPassed
                            ymdStart = targetLocalDate
                            ymdEnd = today
                        }
                        effectiveCategory == CATEGORY_BIRTHDAY || effectiveCategory == CATEGORY_OTHER -> {
                            if (!isRepeating && eventState.isPast) {
                                labelText = stringResource(R.string.days_past_label)
                                dayCount = eventState.daysElapsed
                                ymdStart = targetLocalDate
                                ymdEnd = today
                            } else {
                                labelText = stringResource(R.string.days_until_label)
                                dayCount = eventState.daysRemaining
                                ymdStart = today
                                ymdEnd = nextOccurrenceDate(targetLocalDate, today, eventState.event.repeatType)
                            }
                        }
                        else -> {
                            labelText = stringResource(R.string.days_until_label)
                            dayCount = eventState.daysRemaining
                            ymdStart = today
                            ymdEnd = today.plusDays(eventState.daysRemaining)
                        }
                    }

                    val detailDisplayMode = perEventDateDeltaModes[eventState.event.id] ?: dateDeltaDisplayMode
                    val daysDisplay = if (isToday) todayLabel else if (detailDisplayMode == 0) {
                        formatDaysSmart(dayCount, false) + stringResource(R.string.days_unit)
                    } else {
                        formatBetweenAsYMD(ymdStart, ymdEnd)
                    }
                    // 切换显示模式时保持字号一致，不随内容长短变化
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    scope.launch {
                                        prefs.setDateDeltaDisplayModeForEvent(eventState.event.id, if (detailDisplayMode == 0) 1 else 0)
                                    }
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = daysDisplay,
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp, lineHeight = 40.sp),
                            color = detailContentColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.titleLarge,
                            color = detailContentColor.copy(alpha = 0.65f),
                            letterSpacing = 4.sp
                        )
                    }

                    // 古典渐隐分隔线
                    Spacer(modifier = Modifier.height(32.dp))
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = 24.dp)
                    ) {
                        val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                detailContentColor.copy(alpha = 0.2f),
                                detailContentColor.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                        drawLine(
                            brush = brush,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = 1f
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    // 纪念日：缘起｜已历｜静候 六行（折页排版）
                    if (effectiveCategory == CATEGORY_ANNIVERSARY && isRepeating) {
                        val originDate = targetLocalDate
                        val nextDate = nextOccurrenceDate(originDate, today, eventState.event.repeatType)
                        val safeToday = if (originDate.isAfter(today)) originDate else today
                        val elapsedPeriod = Period.between(originDate, safeToday)
                        val elapsedDays = ChronoUnit.DAYS.between(originDate, safeToday)
                        Spacer(modifier = Modifier.height(32.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // 缘起
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = stringResource(R.string.detail_repeat_origin),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = detailContentColor.copy(alpha = 0.75f),
                                    modifier = Modifier.width(48.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = formatDateWithWeekday(originDate, context),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                        color = detailContentColor.copy(alpha = 0.95f)
                                    )
                                    Text(
                                        text = formatLunarLine(originDate),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                        color = detailContentColor.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            // 已历
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = stringResource(R.string.detail_repeat_elapsed),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = detailContentColor.copy(alpha = 0.75f),
                                    modifier = Modifier.width(48.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = formatElapsedLiterary(elapsedPeriod, context),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                        color = detailContentColor.copy(alpha = 0.95f)
                                    )
                                    Text(
                                        text = formatElapsedDays(kotlin.math.abs(elapsedDays), context),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                        color = detailContentColor.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            // 静候
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = stringResource(R.string.detail_repeat_next),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = detailContentColor.copy(alpha = 0.75f),
                                    modifier = Modifier.width(48.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = formatDateWithWeekday(nextDate, context),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                        color = detailContentColor.copy(alpha = 0.95f)
                                    )
                                    Text(
                                        text = formatLunarLine(nextDate),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                        color = detailContentColor.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }

                    // 其他：仅公历 + 农历
                    if (effectiveCategory == CATEGORY_OTHER) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = detailContentColor.copy(alpha = 0.95f)
                            )
                            Text(
                                text = formatLunarLine(targetLocalDate),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = detailContentColor.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // 生日：农历、岁数、属相、八字、五行、星座
                    if (effectiveCategory == CATEGORY_BIRTHDAY) {
                        Spacer(modifier = Modifier.height(24.dp))
                        val lunarLine = formatLunarLine(targetLocalDate)
                        val period = agePeriod(targetLocalDate, today)
                        val ageYmd = context.getString(R.string.detail_birthday_age_format_ymd, period.years, period.months, period.days)
                        val zodiac = zodiacAnimalFromDate(targetLocalDate)
                        val bazi = baziFromDate(targetLocalDate)
                        val wuxing = wuxingFromDate(targetLocalDate)
                        val constellation = constellationFromDate(targetLocalDate, context)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            DetailLabelRow(stringResource(R.string.detail_birthday_lunar), lunarLine, detailContentColor)
                            DetailLabelRow(stringResource(R.string.detail_birthday_age), ageYmd, detailContentColor)
                            if (zodiac != null) DetailLabelRow(stringResource(R.string.detail_birthday_zodiac), zodiac, detailContentColor)
                            if (bazi != null) DetailLabelRow(stringResource(R.string.detail_birthday_bazi), bazi, detailContentColor)
                            if (wuxing != null) DetailLabelRow(stringResource(R.string.detail_birthday_wuxing), wuxing, detailContentColor)
                            DetailLabelRow(stringResource(R.string.detail_birthday_constellation), constellation, detailContentColor)
                        }
                    }

                    if (showMilestone && isAnniversary && eventState.nextMilestoneDays != null && eventState.nextMilestoneValue != null && detailDisplayMode == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.milestone_in_days,
                                milestoneLabel(eventState.nextMilestoneValue!!),
                                eventState.nextMilestoneDays!!.toInt()
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = detailContentColor.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (eventState.event.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(48.dp))
                        // 备注区域：类似报纸引言 -> 宋代题跋风格
                        Text(
                            text = eventState.event.note, // 去掉引号，更干净
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                                letterSpacing = 0.5.sp
                            ),
                            color = detailContentColor.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 底部操作区：方形按钮（带按压缩放反馈）
            DetailActionButtons(
                onEditClick = onEditClick,
                onDeleteClick = { showDeleteConfirm = true },
                onShareClick = {
                    val isYearlyShare = eventState.event.repeatType == REPEAT_YEARLY
                    val text = when {
                        eventState.isPast ->
                            context.getString(R.string.share_text_past, eventState.event.title, if (isYearlyShare) eventState.daysPassed else eventState.daysElapsed)
                        eventState.daysRemaining == 0L ->
                            context.getString(R.string.share_text_today, eventState.event.title)
                        else ->
                            context.getString(R.string.share_text_countdown, eventState.event.title, eventState.daysRemaining)
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title)))
                }
            )
        }
        }
    }
}

@Composable
private fun DetailActionButtons(
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val editInteractionSource = remember { MutableInteractionSource() }
    val deleteInteractionSource = remember { MutableInteractionSource() }
    val shareInteractionSource = remember { MutableInteractionSource() }
    val editPressed by editInteractionSource.collectIsPressedAsState()
    val deletePressed by deleteInteractionSource.collectIsPressedAsState()
    val sharePressed by shareInteractionSource.collectIsPressedAsState()
    val scaleEdit by animateFloatAsState(if (editPressed) 0.96f else 1f, AnimationSpecs.springButton, label = "edit")
    val scaleDelete by animateFloatAsState(if (deletePressed) 0.96f else 1f, AnimationSpecs.springButton, label = "delete")
    val scaleShare by animateFloatAsState(if (sharePressed) 0.96f else 1f, AnimationSpecs.springButton, label = "share")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp), // 上方留出大量空白
        horizontalArrangement = Arrangement.SpaceEvenly, // 水平分散对齐
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = editInteractionSource,
                    indication = null,
                    onClick = onEditClick
                )
                .graphicsLayer { scaleX = scaleEdit; scaleY = scaleEdit }
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit), modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.button_edit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = shareInteractionSource,
                    indication = null,
                    onClick = onShareClick
                )
                .graphicsLayer { scaleX = scaleShare; scaleY = scaleShare }
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share), modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.button_share), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = deleteInteractionSource,
                    indication = null,
                    onClick = onDeleteClick
                )
                .graphicsLayer { scaleX = scaleDelete; scaleY = scaleDelete }
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete), modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.button_delete), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun DetailLabelRow(label: String, value: String, contentColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label ｜ ",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = contentColor.copy(alpha = 0.75f),
            modifier = Modifier.width(80.dp) // 给 Label 留出固定空间，营造折页错落感
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
            color = contentColor.copy(alpha = 0.95f),
            modifier = Modifier.weight(1f)
        )
    }
}
