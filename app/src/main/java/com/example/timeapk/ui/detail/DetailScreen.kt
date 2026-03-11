package com.example.timeapk.ui.detail

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.material.icons.filled.PushPin
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
import com.example.timeapk.ui.utils.formatElapsedLiterary
import com.example.timeapk.ui.utils.formatElapsedDays
import com.example.timeapk.ui.utils.nextOccurrenceDate
import com.example.timeapk.ui.utils.ageInYears
import com.example.timeapk.ui.utils.agePeriod
import com.example.timeapk.ui.utils.constellationFromDate
import com.example.timeapk.ui.utils.zodiacAnimalFromDate
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.DisplayModes
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import com.example.timeapk.ui.utils.getNextLunarOccurrence
import com.example.timeapk.ui.utils.getLunarElapsedPeriod
import com.example.timeapk.ui.utils.formatLunarDateString
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
    val locale = context.resources.configuration.locales[0]
    val prefs = (context.applicationContext as TimeApplication).userPrefs
    val scope = rememberCoroutineScope()
    val showMilestone by prefs.showMilestoneFlow.collectAsState(initial = true)
    val dateFormatMode by prefs.dateFormatModeFlow.collectAsState(initial = 0)
    val dateDeltaDisplayMode by prefs.dateDeltaDisplayModeFlow.collectAsState(initial = 0)
    val perEventDateDeltaModes by prefs.perEventDateDeltaDisplayModesFlow.collectAsState(initial = emptyMap())
    val pinnedEventIds by prefs.pinnedEventIdsFlow.collectAsState(initial = emptyList())
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
    
    // 铻嶅叆涓婚鑹诧細浣挎祬鑹蹭笉閭ｄ箞鍒虹溂锛堝儚鏌撹壊鐨勫绾革級锛屾繁鑹蹭笌鑳屾櫙鏈変竴瀹氬尯鍒嗭紙鍍忓甫搴曡壊鐨勫ⅷ閿級
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val detailCardColor = androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.surface,
        detailBaseColor,
        if (isDark) 0.15f else 0.08f
    )
    val detailContentColor = MaterialTheme.colorScheme.onSurface

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // 涓庡叾浠栭〉闈㈣儗鏅竴鑷?
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
        // 淇锛歷isible=true 棣栨缁勫悎涓嶆挱鏀惧姩鐢伙紝闇€瑕佷粠 false鈫抰rue 杩囨浮
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
            // 澶嶅彜鍦鸿鏉?鑰侀粍鍘嗛鏍煎崱鐗?-> 瀹嬩唬涔︾敾鏍峰紡
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp), // 绋嶅井澧炲姞鍦嗚
                colors = CardDefaults.cardColors(
                    containerColor = detailCardColor
                ),
                border = BorderStroke(
                    width = 1.dp, // 鍔犳繁涓€鐐硅竟妗?
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // 澧炲姞闃村奖浠ュ寮鸿儗鏅姣?
            ) {
                Column(
                    modifier = Modifier.padding(32.dp), // 澧炲姞鍐呴儴鐣欑櫧
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 椤堕儴锛氭棩鏈熶笌绫诲埆锛堜笌璁剧疆涓棩鏈熸牸寮忎竴鑷达級
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
                            // 鍗扮珷缁勪欢灞曠ず鍒嗙被
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
                    
                    // 鍙ゅ吀涓ょ娓愰殣鍒嗛殧绾?
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

                    // 鏍稿績锛氭爣棰樹笌鍊掕鏃?
                    Text(
                        text = eventState.event.title,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                            letterSpacing = 2.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = detailContentColor.copy(alpha = 0.9f),
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))

                    // 鍊掕鏃?/ 宸插巻灞曠ず锛氭櫤鑳芥ā寮忚疆杞敮鎸?
                    val isToday = eventState.daysRemaining == 0L && !eventState.isPast
                    val todayLabel = stringResource(R.string.days_today_label)
                    val isRepeating = eventState.event.repeatType != REPEAT_NONE

                    val detailDisplayModeRaw = perEventDateDeltaModes[eventState.event.id] ?: dateDeltaDisplayMode
                    val availableModes = getAvailableDisplayModes(eventState, showMilestone = true)
                    val modeIndex = availableModes.indexOf(detailDisplayModeRaw)
                    val mode = if (modeIndex != -1) detailDisplayModeRaw else availableModes.first()

                    val labelText: String
                    val daysDisplay: String

                    when (mode) {
                        DisplayModes.PAST_DAYS -> {
                            val days = if (isRepeating) eventState.daysPassed else eventState.daysElapsed
                            daysDisplay = formatDaysSmart(days, false, locale) + stringResource(R.string.days_unit)
                            labelText = stringResource(R.string.days_past_label)
                        }
                        DisplayModes.PAST_YMD -> {
                            val start = targetLocalDate
                            val end = today
                            daysDisplay = formatBetweenAsYMD(start, end, locale)
                            labelText = stringResource(R.string.days_past_label)
                        }
                        DisplayModes.UNTIL_DAYS -> {
                            if (isToday) {
                                daysDisplay = todayLabel
                                labelText = ""
                            } else {
                                val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
                                daysDisplay = formatDaysSmart(days, false, locale) + stringResource(R.string.days_unit)
                                labelText = com.example.timeapk.ui.utils.getUntilLabel(androidx.compose.ui.platform.LocalContext.current, eventState)
                            }
                        }
                        DisplayModes.UNTIL_YMD -> {
                            if (isToday) {
                                daysDisplay = todayLabel
                                labelText = ""
                            } else {
                                val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
                                val end = today.plusDays(days)
                                daysDisplay = formatBetweenAsYMD(today, end, locale)
                                labelText = com.example.timeapk.ui.utils.getUntilLabel(androidx.compose.ui.platform.LocalContext.current, eventState)
                            }
                        }
                        DisplayModes.MILESTONE -> {
                            daysDisplay = formatDaysSmart(eventState.nextMilestoneDays ?: 0L, false, locale) + stringResource(R.string.days_unit)
                            val milestoneVal = eventState.nextMilestoneValue ?: 0L
                            val milestoneStr = milestoneLabel(milestoneVal)
                            labelText = stringResource(R.string.milestone_label_prefix, milestoneStr)
                        }
                        else -> {
                            daysDisplay = ""
                            labelText = ""
                        }
                    }
                    // 鍒囨崲鏄剧ず妯″紡鏃朵繚鎸佸瓧鍙蜂竴鑷达紝涓嶉殢鍐呭闀跨煭鍙樺寲
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = {
                                    scope.launch {
                                        val nextModeIndex = (availableModes.indexOf(mode) + 1) % availableModes.size
                                        val nextMode = availableModes[nextModeIndex]
                                        prefs.setDateDeltaDisplayModeForEvent(eventState.event.id, nextMode)
                                    }
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = daysDisplay,
                            style = MaterialTheme.typography.displayLarge,
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

                    // 鍙ゅ吀娓愰殣鍒嗛殧绾?
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

                    // 绾康鏃ワ細缂樿捣锝滃凡鍘嗭綔闈欏€?鍏锛堟姌椤垫帓鐗堬級
                    if (effectiveCategory == CATEGORY_ANNIVERSARY && isRepeating) {
                        val originDate = targetLocalDate
                        val isLunarAnniversary = eventState.event.isLunar && eventState.event.repeatType == REPEAT_YEARLY
                        val nextDate = if (isLunarAnniversary) {
                            getNextLunarOccurrence(originDate, today)
                        } else {
                            nextOccurrenceDate(originDate, today, eventState.event.repeatType)
                        }
                        val safeToday = if (originDate.isAfter(today)) originDate else today
                        val elapsedPeriod = if (isLunarAnniversary) {
                            getLunarElapsedPeriod(originDate, safeToday)
                        } else {
                            Period.between(originDate, safeToday)
                        }
                        val elapsedDays = ChronoUnit.DAYS.between(originDate, safeToday)
                        Spacer(modifier = Modifier.height(32.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // 缂樿捣
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
                                    if (isLunarAnniversary) {
                                        Text(
                                            text = formatLunarDateString(originDate, context),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = detailContentColor.copy(alpha = 0.95f)
                                        )
                                        Text(
                                            text = formatDateWithWeekday(originDate, context),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = detailContentColor.copy(alpha = 0.75f)
                                        )
                                    } else {
                                        Text(
                                            text = formatDateWithWeekday(originDate, context),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = detailContentColor.copy(alpha = 0.95f)
                                        )
                                        Text(
                                            text = formatLunarDateString(originDate, context),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = detailContentColor.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            // 宸插巻
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
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = detailContentColor.copy(alpha = 0.95f)
                                    )
                                    Text(
                                        text = formatElapsedDays(kotlin.math.abs(elapsedDays), context),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = detailContentColor.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            // 闈欏€?
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
                                    if (isLunarAnniversary) {
                                        Text(
                                            text = formatLunarDateString(nextDate, context),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = detailContentColor.copy(alpha = 0.95f)
                                        )
                                        Text(
                                            text = formatDateWithWeekday(nextDate, context),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = detailContentColor.copy(alpha = 0.75f)
                                        )
                                    } else {
                                        Text(
                                            text = formatDateWithWeekday(nextDate, context),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = detailContentColor.copy(alpha = 0.95f)
                                        )
                                        Text(
                                            text = formatLunarDateString(nextDate, context),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = detailContentColor.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 鍏朵粬锛氫粎鍏巻 + 鍐滃巻
                    if (effectiveCategory == CATEGORY_OTHER) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyLarge,
                                color = detailContentColor.copy(alpha = 0.95f)
                            )
                            Text(
                                text = formatLunarDateString(targetLocalDate, context),
                                style = MaterialTheme.typography.bodyMedium,
                                color = detailContentColor.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // 鐢熸棩锛氬啘鍘嗐€佸瞾鏁般€佸睘鐩搞€佹槦搴?
                    if (effectiveCategory == CATEGORY_BIRTHDAY) {
                        Spacer(modifier = Modifier.height(24.dp))
                        val lunarLine = formatLunarDateString(targetLocalDate, context)
                        val period = agePeriod(targetLocalDate, today)
                        val ageYmd = context.getString(
                            R.string.detail_birthday_age_format_ymd,
                            period.years,
                            period.months,
                            period.days
                        )
                        val zodiac = zodiacAnimalFromDate(targetLocalDate)
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
                            DetailLabelRow(stringResource(R.string.detail_birthday_constellation), constellation, detailContentColor)
                        }
                    }

                    if (eventState.event.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(48.dp))
                        // 澶囨敞鍖哄煙锛氱被浼兼姤绾稿紩瑷€ -> 瀹嬩唬棰樿穻椋庢牸
                        Text(
                            text = eventState.event.note, // 鍘绘帀寮曞彿锛屾洿骞插噣
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
            
            // 搴曢儴鎿嶄綔鍖猴細鏂瑰舰鎸夐挳锛堝甫鎸夊帇缂╂斁鍙嶉锛?
            DetailActionButtons(
                isPinned = eventState.event.id in pinnedEventIds,
                isReminderOrScheduleEnabled = eventState.event.remindEnabled || eventState.event.syncToScheduleEnabled,
                onReminderCalendarClick = onEditClick,
                onPinClick = { scope.launch { prefs.togglePinnedEventId(eventState.event.id) } },
                onEditClick = onEditClick,
                onDeleteClick = { showDeleteConfirm = true },
                onShareClick = {
                    val isYearlyShare = eventState.event.repeatType == REPEAT_YEARLY
                    val text = when {
                        eventState.isPast -> {
                            val elapsedDays = if (isYearlyShare) eventState.daysPassed else eventState.daysElapsed
                            context.resources.getQuantityString(
                                R.plurals.share_text_past,
                                elapsedDays.toInt(),
                                eventState.event.title,
                                elapsedDays.toInt()
                            )
                        }
                        eventState.daysRemaining == 0L ->
                            context.getString(R.string.share_text_today, eventState.event.title)
                        else -> {
                            val remainingDays = eventState.daysRemaining.toInt()
                            context.resources.getQuantityString(
                                R.plurals.share_text_countdown,
                                remainingDays,
                                eventState.event.title,
                                remainingDays
                            )
                        }
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
    isPinned: Boolean,
    isReminderOrScheduleEnabled: Boolean,
    onReminderCalendarClick: () -> Unit,
    onPinClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val reminderInteractionSource = remember { MutableInteractionSource() }
    val pinInteractionSource = remember { MutableInteractionSource() }
    val editInteractionSource = remember { MutableInteractionSource() }
    val deleteInteractionSource = remember { MutableInteractionSource() }
    val shareInteractionSource = remember { MutableInteractionSource() }
    val reminderPressed by reminderInteractionSource.collectIsPressedAsState()
    val pinPressed by pinInteractionSource.collectIsPressedAsState()
    val editPressed by editInteractionSource.collectIsPressedAsState()
    val deletePressed by deleteInteractionSource.collectIsPressedAsState()
    val sharePressed by shareInteractionSource.collectIsPressedAsState()
    val scaleReminder by animateFloatAsState(if (reminderPressed) 0.96f else 1f, AnimationSpecs.springButton, label = "reminder")
    val scalePin by animateFloatAsState(if (pinPressed) 0.96f else 1f, AnimationSpecs.springButton, label = "pin")
    val scaleEdit by animateFloatAsState(if (editPressed) 0.96f else 1f, AnimationSpecs.springButton, label = "edit")
    val scaleDelete by animateFloatAsState(if (deletePressed) 0.96f else 1f, AnimationSpecs.springButton, label = "delete")
    val scaleShare by animateFloatAsState(if (sharePressed) 0.96f else 1f, AnimationSpecs.springButton, label = "share")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp), // 涓婃柟鐣欏嚭澶ч噺绌虹櫧
        horizontalArrangement = Arrangement.SpaceEvenly, // 姘村钩鍒嗘暎瀵归綈
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = reminderInteractionSource,
                    indication = LocalIndication.current,
                    onClick = onReminderCalendarClick
                )
                .graphicsLayer { scaleX = scaleReminder; scaleY = scaleReminder }
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = stringResource(R.string.button_reminder_calendar),
                modifier = Modifier.size(24.dp),
                tint = if (isReminderOrScheduleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.button_reminder_calendar),
                style = MaterialTheme.typography.labelSmall,
                color = if (isReminderOrScheduleEnabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                }
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = pinInteractionSource,
                    indication = LocalIndication.current,
                    onClick = onPinClick
                )
                .graphicsLayer { scaleX = scalePin; scaleY = scalePin }
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = if (isPinned) stringResource(R.string.button_unpin) else stringResource(R.string.button_pin),
                modifier = Modifier.size(24.dp),
                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (isPinned) stringResource(R.string.button_unpin) else stringResource(R.string.button_pin),
                style = MaterialTheme.typography.labelSmall,
                color = if (isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = editInteractionSource,
                    indication = LocalIndication.current,
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
                    indication = LocalIndication.current,
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
                    indication = LocalIndication.current,
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
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = 0.75f),
            modifier = Modifier.width(80.dp) // 缁?Label 鐣欏嚭鍥哄畾绌洪棿锛岃惀閫犳姌椤甸敊钀芥劅
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = 0.95f),
            modifier = Modifier.weight(1f)
        )
    }
}




