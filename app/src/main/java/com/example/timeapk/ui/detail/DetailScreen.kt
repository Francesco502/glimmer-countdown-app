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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.timeapk.ui.utils.formatDays
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDaysSmart
import com.example.timeapk.ui.utils.getDisplayDateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    val showMilestone by prefs.showMilestoneFlow.collectAsState(initial = true)
    val dateFormatMode by prefs.dateFormatModeFlow.collectAsState(initial = 0)
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
    
    // Song Aesthetic: Surface color for card, primary/base color for accents
    val detailCardColor = MaterialTheme.colorScheme.surface
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
                shape = RoundedCornerShape(2.dp), // 极小圆角
                colors = CardDefaults.cardColors(
                    containerColor = detailCardColor
                ),
                border = BorderStroke(
                    width = 0.5.dp, // 极细边框
                    color = MaterialTheme.colorScheme.outline
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp), // 增加内部留白
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部：日期与类别（与设置中日期格式一致）
                    val today = LocalDate.now()
                    val targetLocalDate = Instant.ofEpochMilli(eventState.event.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    // Logic Migration: Use repeatType instead of category
                    val isYearly = eventState.event.repeatType == REPEAT_YEARLY
                    
                    val isAgeMode = !eventState.isPast && isYearly && !targetLocalDate.isAfter(today)
                    // Treat all Yearly events as Anniversaries for milestone logic
                    val isAnniversary = isYearly
                    
                    val dateStr = targetLocalDate.format(dateFormatter)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelMedium,
                            color = detailContentColor.copy(alpha = 0.5f)
                        )
                        // Removed category tag
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 24.dp),
                        thickness = 0.5.dp,
                        color = detailContentColor.copy(alpha = 0.1f)
                    )

                    // 核心：标题与倒计时
                    Text(
                        text = eventState.event.title,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                            letterSpacing = 2.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = detailContentColor.copy(alpha = 0.9f)
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))

                    // Display Mode Logic: 0 = Remaining, 1 = Elapsed(Days), 2 = Elapsed(YMD)
                    val initialMode = remember(eventState.event.id) {
                        if (isAgeMode) 2 else if (eventState.isPast) 1 else 0
                    }
                    var displayMode by remember(eventState.event.id) { mutableIntStateOf(initialMode) }
                    
                    fun cycleMode() {
                        displayMode = when (displayMode) {
                            0 -> if (isAgeMode) 2 else 1
                            1 -> 0
                            2 -> 1
                            else -> 0
                        }
                    }

                    val isToday = eventState.daysRemaining == 0L
                    val todayLabel = stringResource(R.string.days_today_label)
                    val daysDisplay = if (isToday) todayLabel else when (displayMode) {
                        2 -> formatBetweenAsYMD(targetLocalDate, today)
                        1 -> formatDays(eventState.daysPassed)
                        else -> formatDaysSmart(eventState.daysRemaining, false)
                    }
                    val isYMDLong = displayMode == 2 && daysDisplay.length > 6

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { cycleMode() }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = daysDisplay,
                            style = if (isYMDLong)
                                MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 48.sp)
                            else
                                MaterialTheme.typography.displayMedium.copy(fontSize = 72.sp, lineHeight = 80.sp), // 稍微减小字号，更雅致
                            color = detailBaseColor, // 使用强调色
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (isToday) ""
                                else when (displayMode) {
                                    2, 1 -> stringResource(R.string.days_past_label)
                                    else -> stringResource(R.string.days_left_label)
                                },
                            style = MaterialTheme.typography.titleMedium,
                            color = detailContentColor.copy(alpha = 0.4f),
                            letterSpacing = 4.sp
                        )
                    }

                    // Complementary Info Area
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Show complementary info based on mode
                    val secondaryText = if (isToday) {
                        null
                    } else if (displayMode == 0) {
                        if (isAgeMode) {
                            stringResource(R.string.days_past_label) + " " + formatBetweenAsYMD(targetLocalDate, today)
                        } else if (eventState.daysPassed > 0) {
                            stringResource(R.string.days_past_label) + " " + formatDays(eventState.daysPassed)
                        } else null
                    } else {
                        stringResource(R.string.days_left_label) + " " + formatDays(eventState.daysRemaining)
                    }

                    if (secondaryText != null) {
                        Text(
                            text = secondaryText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = detailContentColor.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (showMilestone && isAnniversary && eventState.nextMilestoneDays != null && eventState.nextMilestoneValue != null && displayMode == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.milestone_in_days,
                                milestoneLabel(eventState.nextMilestoneValue!!),
                                eventState.nextMilestoneDays!!.toInt()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = detailContentColor.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (eventState.event.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(48.dp))
                        // 备注区域：类似报纸引言 -> 宋代题跋风格
                        Text(
                            text = eventState.event.note, // 去掉引号，更干净
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                                letterSpacing = 0.5.sp
                            ),
                            color = detailContentColor.copy(alpha = 0.6f),
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
                    val text = when {
                        eventState.isPast ->
                            context.getString(R.string.share_text_past, eventState.event.title, eventState.daysElapsed)
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onEditClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .graphicsLayer { scaleX = scaleEdit; scaleY = scaleEdit },
            interactionSource = editInteractionSource,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
        ) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit), modifier = Modifier.size(20.dp))
        }
        OutlinedButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .graphicsLayer { scaleX = scaleDelete; scaleY = scaleDelete },
            interactionSource = deleteInteractionSource,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete), modifier = Modifier.size(20.dp))
        }
        Button(
            onClick = onShareClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .graphicsLayer { scaleX = scaleShare; scaleY = scaleShare },
            interactionSource = shareInteractionSource,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share), modifier = Modifier.size(20.dp))
        }
    }
}

private fun parseEventColorOrFallback(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

private fun contentColorFor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) Color(0xFF1A232C) else Color.White
}
