package com.example.timeapk.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongPaperSurface
import com.example.timeapk.ui.theme.SongSealLabel
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

private val DetailContentMaxWidth = 760.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    eventState: EventUiState?,
    eventMissing: Boolean = false,
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (eventMissing) {
                    Text(
                        text = stringResource(R.string.event_load_error),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                TextButton(onClick = onNavigateBack) {
                    Text(stringResource(R.string.nav_back))
                }
            }
        }
        return
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = MaterialTheme.shapes.medium,
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message, eventState.event.title)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        onDeleteClick()
                        showDeleteConfirm = false
                        onNavigateBack()
                    }
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
    
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val detailCardColor = androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.surface,
        detailBaseColor,
        if (isDark) 0.15f else 0.08f
    )
    val detailContentColor = MaterialTheme.colorScheme.onSurface

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
        var contentVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { contentVisible = true }
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = AnimationSpecs.mediumTween()) + slideInVertically(animationSpec = AnimationSpecs.mediumTweenIntOffset()) { it / 8 }
        ) {
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
        ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = DetailContentMaxWidth)
                .padding(24.dp)
        ) {
            SongPaperSurface(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = detailCardColor,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                            val categoryName = when (eventState.event.category) {
                                CATEGORY_BIRTHDAY -> stringResource(R.string.category_birthday)
                                CATEGORY_ANNIVERSARY -> stringResource(R.string.category_anniversary)
                                CATEGORY_OTHER -> stringResource(R.string.category_other)
                                else -> eventState.event.category
                            }
                            SongSealLabel(
                                text = categoryName,
                                color = detailBaseColor
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = detailContentColor.copy(alpha = 0.75f)
                            )
                        }
                        
                        Icon(
                            imageVector = if (eventState.event.remindEnabled) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                            contentDescription = if (eventState.event.remindEnabled) stringResource(R.string.cd_reminder_on) else stringResource(R.string.cd_reminder_off),
                            modifier = Modifier.size(20.dp),
                            tint = if (eventState.event.remindEnabled) detailBaseColor else detailContentColor.copy(alpha = 0.35f)
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .height(SongDesignTokens.BorderWidth.dp),
                        color = detailContentColor.copy(alpha = SongDesignTokens.BorderAlphaSoft)
                    )

                    Text(
                        text = eventState.event.title,
                        style = MaterialTheme.typography.displayMedium.copy(
                            letterSpacing = 0.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = detailContentColor.copy(alpha = 0.9f),
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))

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
                            letterSpacing = 0.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(SongDesignTokens.BorderWidth.dp),
                        color = detailContentColor.copy(alpha = SongDesignTokens.BorderAlphaSoft)
                    )
                    Spacer(modifier = Modifier.height(32.dp))

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
                        Text(
                            text = eventState.event.note,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                                letterSpacing = 0.sp
                            ),
                            color = detailContentColor.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Bottom action row.
            ResponsiveDetailActionButtons(
                isPinned = eventState.event.id in pinnedEventIds,
                isReminderOrScheduleEnabled = eventState.event.remindEnabled || eventState.event.syncToScheduleEnabled,
                onReminderCalendarClick = onEditClick,
                onPinClick = { scope.launch { prefs.togglePinnedEventId(eventState.event.id) } },
                onEditClick = onEditClick,
                onDeleteClick = { showDeleteConfirm = true }
            )
        }
        }
        }
    }
}

@Composable
private fun ResponsiveDetailActionButtons(
    isPinned: Boolean,
    isReminderOrScheduleEnabled: Boolean,
    onReminderCalendarClick: () -> Unit,
    onPinClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val reminderInteractionSource = remember { MutableInteractionSource() }
    val pinInteractionSource = remember { MutableInteractionSource() }
    val editInteractionSource = remember { MutableInteractionSource() }
    val deleteInteractionSource = remember { MutableInteractionSource() }
    val reminderPressed by reminderInteractionSource.collectIsPressedAsState()
    val pinPressed by pinInteractionSource.collectIsPressedAsState()
    val editPressed by editInteractionSource.collectIsPressedAsState()
    val deletePressed by deleteInteractionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        ResponsiveDetailActionButton(
            modifier = Modifier.weight(1f),
            interactionSource = reminderInteractionSource,
            scale = animateFloatAsState(
                AnimationSpecs.responsiveScale(if (reminderPressed) 0.96f else 1f),
                AnimationSpecs.springButton,
                label = "responsiveReminder"
            ).value,
            onClick = onReminderCalendarClick,
            icon = Icons.Outlined.Notifications,
            label = stringResource(R.string.button_reminder_calendar),
            iconTint = if (isReminderOrScheduleEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            },
            textColor = if (isReminderOrScheduleEnabled) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            }
        )
        ResponsiveDetailActionButton(
            modifier = Modifier.weight(1f),
            interactionSource = pinInteractionSource,
            scale = animateFloatAsState(
                AnimationSpecs.responsiveScale(if (pinPressed) 0.96f else 1f),
                AnimationSpecs.springButton,
                label = "responsivePin"
            ).value,
            onClick = onPinClick,
            icon = Icons.Outlined.PushPin,
            label = if (isPinned) stringResource(R.string.button_unpin) else stringResource(R.string.button_pin),
            iconTint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textColor = if (isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        ResponsiveDetailActionButton(
            modifier = Modifier.weight(1f),
            interactionSource = editInteractionSource,
            scale = animateFloatAsState(
                AnimationSpecs.responsiveScale(if (editPressed) 0.96f else 1f),
                AnimationSpecs.springButton,
                label = "responsiveEdit"
            ).value,
            onClick = onEditClick,
            icon = Icons.Outlined.Edit,
            label = stringResource(R.string.button_edit),
            iconTint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            contentDescription = stringResource(R.string.cd_edit)
        )
        ResponsiveDetailActionButton(
            modifier = Modifier.weight(1f),
            interactionSource = deleteInteractionSource,
            scale = animateFloatAsState(
                AnimationSpecs.responsiveScale(if (deletePressed) 0.96f else 1f),
                AnimationSpecs.springButton,
                label = "responsiveDelete"
            ).value,
            onClick = onDeleteClick,
            icon = Icons.Outlined.Delete,
            label = stringResource(R.string.button_delete),
            iconTint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
            textColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
            contentDescription = stringResource(R.string.cd_delete)
        )
    }
}

@Composable
private fun ResponsiveDetailActionButton(
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
    scale: Float,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    iconTint: Color,
    textColor: Color,
    contentDescription: String = label
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
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
            modifier = Modifier.width(80.dp)
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
