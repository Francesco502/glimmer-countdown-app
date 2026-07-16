package com.example.timeapk.ui.detail

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.REPEAT_NONE
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.permissions.areAppNotificationsEnabledCompat
import com.example.timeapk.permissions.hasCalendarReadWritePermission
import com.example.timeapk.permissions.openAppDetailsSettings
import com.example.timeapk.permissions.openAppNotificationSettings
import com.example.timeapk.ui.common.SongBottomAction
import com.example.timeapk.ui.common.SongBottomActionBar
import com.example.timeapk.ui.common.SongReminderStatusStrip
import com.example.timeapk.ui.components.SongConfirmDialog
import com.example.timeapk.ui.components.SongDialogButton
import com.example.timeapk.ui.components.SongFormDialog
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.milestoneLabel
import com.example.timeapk.ui.reminder.ReminderStatusAction
import com.example.timeapk.ui.reminder.ReminderStatusSummary
import com.example.timeapk.ui.reminder.buildReminderStatus
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
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
import com.example.timeapk.widget.WidgetUpdater
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DetailContentMaxWidth = 760.dp
private val DetailSupplementContentMaxWidth = 300.dp
private val DetailSupplementLabelWidth = 64.dp
private val DetailSupplementValueMaxWidth = 228.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    eventState: EventUiState?,
    eventMissing: Boolean = false,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: suspend () -> Boolean,
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
    var isDeleteInProgress by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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
        SongConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = stringResource(R.string.delete_confirm_message, eventState.event.title),
            confirmText = stringResource(
                if (isDeleteInProgress) R.string.delete_in_progress else R.string.delete_confirm_ok
            ),
            dismissText = stringResource(R.string.delete_confirm_cancel),
            destructiveConfirm = true,
            confirmEnabled = !isDeleteInProgress,
            onConfirm = {
                if (!isDeleteInProgress) {
                    isDeleteInProgress = true
                    scope.launch {
                        val deleted = try {
                            onDeleteClick()
                        } finally {
                            isDeleteInProgress = false
                        }
                        if (deleted) {
                            showDeleteConfirm = false
                            onNavigateBack()
                        }
                    }
                }
            },
            onDismiss = {
                if (!isDeleteInProgress) showDeleteConfirm = false
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
    val today = LocalDate.now()
    val targetLocalDate = eventDateToLocalDate(eventState.event.date)
    val dateStr = targetLocalDate.format(dateFormatter)
    val categoryName = when (eventState.event.category) {
        CATEGORY_BIRTHDAY -> stringResource(R.string.category_birthday)
        CATEGORY_ANNIVERSARY -> stringResource(R.string.category_anniversary)
        CATEGORY_OTHER -> stringResource(R.string.category_other)
        else -> eventState.event.category
    }
    val repeatLabel = when (eventState.event.repeatType) {
        REPEAT_DAILY -> stringResource(R.string.repeat_daily)
        REPEAT_WEEKLY -> stringResource(R.string.repeat_weekly)
        REPEAT_MONTHLY -> stringResource(R.string.repeat_monthly)
        REPEAT_HALF_YEARLY -> stringResource(R.string.repeat_half_yearly)
        REPEAT_YEARLY -> stringResource(R.string.repeat_yearly)
        else -> null
    }
    val calendarMetaLine = buildList {
        if (eventState.event.isLunar) add(formatLunarDateString(targetLocalDate, context))
        repeatLabel?.let(::add)
        if (eventState.event.remindEnabled) add(stringResource(R.string.field_remind))
        if (eventState.event.syncToScheduleEnabled) add(stringResource(R.string.sync_to_schedule))
    }.joinToString(" · ")
    val detailDisplayModeRaw = perEventDateDeltaModes[eventState.event.id] ?: dateDeltaDisplayMode
    val availableModes = getAvailableDisplayModes(eventState, showMilestone = showMilestone)
    val modeIndex = availableModes.indexOf(detailDisplayModeRaw)
    val mode = if (modeIndex != -1) detailDisplayModeRaw else availableModes.first()
    val timeDisplay = detailTimeDisplay(
        eventState = eventState,
        mode = mode,
        targetLocalDate = targetLocalDate,
        today = today,
        locale = locale
    )
    val reminderStatus = buildReminderStatus(
        event = eventState.event,
        notificationsEnabled = context.areAppNotificationsEnabledCompat(),
        calendarPermissionGranted = context.hasCalendarReadWritePermission(),
        hasWritableCalendar = true
    )
    val shareData = buildEventShareCardData(
        title = eventState.event.title,
        categoryLabel = categoryName,
        dateText = dateStr,
        timeText = timeDisplay.value,
        timeLabel = timeDisplay.label,
        accentColor = detailBaseColor,
        brandText = stringResource(R.string.app_name)
    )
    val shareImageName = remember(eventState.event.title) {
        ShareImageStore.shareImageName(eventState.event.title)
    }

    if (showShareDialog) {
        SongFormDialog(
            title = stringResource(R.string.share_card_title),
            onDismissRequest = { showShareDialog = false },
            content = {
                EventShareCard(
                    data = shareData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                )
            },
            buttons = {
                SongDialogButton(
                    text = stringResource(R.string.delete_confirm_cancel),
                    onClick = { showShareDialog = false }
                )
                Spacer(modifier = Modifier.width(4.dp))
                SongDialogButton(
                    text = stringResource(R.string.share_save_image),
                    onClick = {
                        scope.launch {
                            val savedUri = runCatching {
                                withRenderedShareImage(shareData) { bitmap ->
                                    ShareImageStore.saveShareImage(context, bitmap, shareImageName)
                                }
                            }.getOrNull()
                            snackbarHostState.showSnackbar(
                                context.getString(
                                    if (savedUri != null) {
                                        R.string.share_image_saved
                                    } else {
                                        R.string.share_image_failed
                                    }
                                )
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                SongDialogButton(
                    text = stringResource(R.string.share_send_image),
                    onClick = {
                        scope.launch {
                            val shared = runCatching {
                                val uri = withRenderedShareImage(shareData) { bitmap ->
                                    ShareImageStore.cacheShareImage(context, bitmap, shareImageName)
                                }
                                ShareImageStore.shareImage(
                                    context = context,
                                    imageUri = uri,
                                    chooserTitle = context.getString(R.string.share_chooser_title)
                                )
                            }.isSuccess
                            if (shared) {
                                showShareDialog = false
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.share_image_failed))
                            }
                        }
                    }
                )
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.detail_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DetailHeroCard(
                eventState = eventState,
                categoryName = categoryName,
                dateStr = dateStr,
                timeDisplay = timeDisplay,
                detailBaseColor = detailBaseColor,
                detailCardColor = detailCardColor,
                detailContentColor = detailContentColor,
                availableModes = availableModes,
                mode = mode,
                onTimeDisplayClick = {
                    scope.launch {
                        val nextModeIndex = (availableModes.indexOf(mode) + 1) % availableModes.size
                        val nextMode = availableModes[nextModeIndex]
                        prefs.setDateDeltaDisplayModeForEvent(eventState.event.id, nextMode)
                    }
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            DetailSupplementSections(
                eventState = eventState,
                targetLocalDate = targetLocalDate,
                today = today,
                calendarMetaLine = calendarMetaLine,
                reminderStatus = reminderStatus,
                detailContentColor = detailContentColor,
                onReminderActionClick = detailReminderStatusAction(
                    context,
                    reminderStatus.primaryAction,
                    onEditClick
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            DetailBottomActions(
                isPinned = eventState.event.id in pinnedEventIds,
                onPinClick = {
                    scope.launch {
                        prefs.togglePinnedEventId(eventState.event.id)
                        WidgetUpdater.refreshCountdownWidgets(context)
                    }
                },
                onEditClick = onEditClick,
                onShareClick = { showShareDialog = true },
                onDeleteClick = { showDeleteConfirm = true }
            )
        }
        }
        }
    }
}

private suspend fun <T> withRenderedShareImage(
    data: EventShareCardData,
    storeImage: (Bitmap) -> T
): T {
    val bitmap = withContext(Dispatchers.Default) {
        EventShareImageRenderer().render(data)
    }
    return try {
        withContext(Dispatchers.IO) {
            storeImage(bitmap)
        }
    } finally {
        bitmap.recycle()
    }
}

private data class DetailTimeDisplay(
    val value: String,
    val label: String
)

@Composable
private fun detailTimeDisplay(
    eventState: EventUiState,
    mode: Int,
    targetLocalDate: LocalDate,
    today: LocalDate,
    locale: Locale
): DetailTimeDisplay {
    val isToday = eventState.daysRemaining == 0L && !eventState.isPast
    val todayLabel = stringResource(R.string.days_today_label)
    val isRepeating = eventState.event.repeatType != REPEAT_NONE
    return when (mode) {
        DisplayModes.PAST_DAYS -> {
            val days = if (isRepeating) eventState.daysPassed else eventState.daysElapsed
            DetailTimeDisplay(
                value = formatDaysSmart(days, false, locale) + stringResource(R.string.days_unit),
                label = stringResource(R.string.days_past_label)
            )
        }
        DisplayModes.PAST_YMD -> {
            DetailTimeDisplay(
                value = formatBetweenAsYMD(targetLocalDate, today, locale),
                label = stringResource(R.string.days_past_label)
            )
        }
        DisplayModes.UNTIL_DAYS -> {
            if (isToday) {
                DetailTimeDisplay(value = todayLabel, label = "")
            } else {
                val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
                DetailTimeDisplay(
                    value = formatDaysSmart(days, false, locale) + stringResource(R.string.days_unit),
                    label = com.example.timeapk.ui.utils.getUntilLabel(LocalContext.current, eventState)
                )
            }
        }
        DisplayModes.UNTIL_YMD -> {
            if (isToday) {
                DetailTimeDisplay(value = todayLabel, label = "")
            } else {
                val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
                DetailTimeDisplay(
                    value = formatBetweenAsYMD(today, today.plusDays(days), locale),
                    label = com.example.timeapk.ui.utils.getUntilLabel(LocalContext.current, eventState)
                )
            }
        }
        DisplayModes.MILESTONE -> {
            val milestoneVal = eventState.nextMilestoneValue ?: 0L
            DetailTimeDisplay(
                value = formatDaysSmart(eventState.nextMilestoneDays ?: 0L, false, locale) + stringResource(R.string.days_unit),
                label = stringResource(R.string.milestone_label_prefix, milestoneLabel(milestoneVal))
            )
        }
        else -> DetailTimeDisplay(value = "", label = "")
    }
}

@Composable
private fun DetailHeroCard(
    eventState: EventUiState,
    categoryName: String,
    dateStr: String,
    timeDisplay: DetailTimeDisplay,
    detailBaseColor: Color,
    detailCardColor: Color,
    detailContentColor: Color,
    availableModes: List<Int>,
    mode: Int,
    onTimeDisplayClick: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongSealLabel(
                    text = categoryName,
                    color = detailBaseColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodyLarge,
                    color = detailContentColor.copy(alpha = 0.75f)
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .height(SongDesignTokens.BorderWidth.dp),
                color = detailContentColor.copy(alpha = SongDesignTokens.BorderAlphaSoft)
            )

            val titleStyle = when {
                eventState.event.title.length > 18 -> MaterialTheme.typography.headlineMedium
                eventState.event.title.length > 10 -> MaterialTheme.typography.displaySmall
                else -> MaterialTheme.typography.displayMedium
            }.copy(letterSpacing = 0.sp)
            Text(
                text = eventState.event.title,
                style = titleStyle,
                textAlign = TextAlign.Center,
                color = detailContentColor.copy(alpha = 0.9f),
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(40.dp))

            val daysStyle = when {
                timeDisplay.value.length > 12 -> MaterialTheme.typography.headlineLarge
                timeDisplay.value.length > 8 -> MaterialTheme.typography.displayMedium
                else -> MaterialTheme.typography.displayLarge
            }.copy(letterSpacing = 0.sp)
            val canCycleMode = availableModes.size > 1 && availableModes.contains(mode)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        enabled = canCycleMode,
                        onClick = onTimeDisplayClick
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeDisplay.value,
                    style = daysStyle,
                    color = detailContentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                if (timeDisplay.label.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = timeDisplay.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = detailContentColor.copy(alpha = 0.65f),
                        letterSpacing = 0.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSupplementSections(
    eventState: EventUiState,
    targetLocalDate: LocalDate,
    today: LocalDate,
    calendarMetaLine: String,
    reminderStatus: ReminderStatusSummary,
    detailContentColor: Color,
    onReminderActionClick: (() -> Unit)?
) {
    val context = LocalContext.current
    val effectiveCategory = eventState.event.category
        .takeIf { it in listOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER) }
        ?: CATEGORY_OTHER
    val isRepeating = eventState.event.repeatType != REPEAT_NONE

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DetailSupplementTable {
            if (calendarMetaLine.isNotBlank()) {
                Text(
                    text = calendarMetaLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = detailContentColor.copy(alpha = 0.62f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            val lunarText = formatLunarDateString(targetLocalDate, context)
            DetailLabelRow(
                stringResource(R.string.detail_birthday_lunar),
                lunarText,
                detailContentColor
            )

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
                DetailLabelRow(
                    stringResource(R.string.detail_repeat_origin),
                    if (isLunarAnniversary) {
                        "${formatLunarDateString(originDate, context)} · ${formatDateWithWeekday(originDate, context)}"
                    } else {
                        "${formatDateWithWeekday(originDate, context)} · ${formatLunarDateString(originDate, context)}"
                    },
                    detailContentColor
                )
                DetailLabelRow(
                    stringResource(R.string.detail_repeat_elapsed),
                    "${formatElapsedLiterary(elapsedPeriod, context)} · ${formatElapsedDays(kotlin.math.abs(elapsedDays), context)}",
                    detailContentColor
                )
                DetailLabelRow(
                    stringResource(R.string.detail_repeat_next),
                    if (isLunarAnniversary) {
                        "${formatLunarDateString(nextDate, context)} · ${formatDateWithWeekday(nextDate, context)}"
                    } else {
                        "${formatDateWithWeekday(nextDate, context)} · ${formatLunarDateString(nextDate, context)}"
                    },
                    detailContentColor
                )
            }

            if (effectiveCategory == CATEGORY_BIRTHDAY) {
                val period = agePeriod(targetLocalDate, today)
                val ageYmd = context.getString(
                    R.string.detail_birthday_age_format_ymd,
                    period.years,
                    period.months,
                    period.days
                )
                val zodiac = zodiacAnimalFromDate(targetLocalDate)
                val constellationText = constellationDisplayText(context, targetLocalDate)
                DetailLabelRow(
                    stringResource(R.string.detail_birthday_age),
                    ageYmd,
                    detailContentColor
                )
                if (zodiac != null) {
                    val zodiacText = zodiacDisplayText(context, zodiac)
                    DetailLabelRow(
                        stringResource(R.string.detail_birthday_zodiac),
                        zodiacText,
                        detailContentColor
                    )
                }
                DetailLabelRow(
                    stringResource(R.string.detail_birthday_constellation),
                    constellationText,
                    detailContentColor
                )
            }

            if (eventState.event.note.isNotBlank()) {
                DetailLabelRow(stringResource(R.string.field_note), eventState.event.note, detailContentColor)
            }
        }

        SongReminderStatusStrip(
            status = reminderStatus,
            title = reminderStatusTitle(context, reminderStatus),
            modifier = Modifier
                .widthIn(max = DetailSupplementContentMaxWidth)
                .fillMaxWidth(),
            actionLabel = reminderStatusActionLabel(context, reminderStatus),
            onActionClick = onReminderActionClick
        )
    }
}

@Composable
private fun DetailSupplementTable(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = DetailSupplementContentMaxWidth)
            .width(IntrinsicSize.Max)
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun DetailBottomActions(
    isPinned: Boolean,
    onPinClick: () -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    SongBottomActionBar(
        actions = listOf(
            SongBottomAction(
                label = if (isPinned) stringResource(R.string.button_unpin) else stringResource(R.string.button_pin),
                icon = SongLineIconKind.Pin,
                tint = if (isPinned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = onPinClick
            ),
            SongBottomAction(
                label = stringResource(R.string.button_edit),
                icon = SongLineIconKind.Edit,
                contentDescription = stringResource(R.string.cd_edit),
                onClick = onEditClick
            ),
            SongBottomAction(
                label = stringResource(R.string.button_share),
                icon = SongLineIconKind.Share,
                onClick = onShareClick
            ),
            SongBottomAction(
                label = stringResource(R.string.button_delete),
                icon = SongLineIconKind.Delete,
                tint = MaterialTheme.colorScheme.error,
                onClick = onDeleteClick
            )
        ),
        outlined = false
    )
}

private fun detailReminderStatusAction(
    context: android.content.Context,
    action: ReminderStatusAction,
    onEditClick: () -> Unit
): (() -> Unit)? {
    return when (action) {
        ReminderStatusAction.None -> null
        ReminderStatusAction.EnableReminder,
        ReminderStatusAction.DisableScheduleSync,
        ReminderStatusAction.RebuildScheduleSync -> onEditClick
        ReminderStatusAction.OpenNotificationSettings -> {
            { context.openAppNotificationSettings() }
        }
        ReminderStatusAction.OpenCalendarSettings -> {
            { context.openAppDetailsSettings() }
        }
    }
}

private fun zodiacDisplayText(
    context: android.content.Context,
    zodiac: String
): String {
    val labels = when (zodiac) {
        "鼠" -> R.string.zodiac_animal_rat to R.string.zodiac_branch_rat
        "牛" -> R.string.zodiac_animal_ox to R.string.zodiac_branch_ox
        "虎" -> R.string.zodiac_animal_tiger to R.string.zodiac_branch_tiger
        "兔" -> R.string.zodiac_animal_rabbit to R.string.zodiac_branch_rabbit
        "龙" -> R.string.zodiac_animal_dragon to R.string.zodiac_branch_dragon
        "蛇" -> R.string.zodiac_animal_snake to R.string.zodiac_branch_snake
        "马" -> R.string.zodiac_animal_horse to R.string.zodiac_branch_horse
        "羊" -> R.string.zodiac_animal_goat to R.string.zodiac_branch_goat
        "猴" -> R.string.zodiac_animal_monkey to R.string.zodiac_branch_monkey
        "鸡" -> R.string.zodiac_animal_rooster to R.string.zodiac_branch_rooster
        "狗" -> R.string.zodiac_animal_dog to R.string.zodiac_branch_dog
        "猪" -> R.string.zodiac_animal_pig to R.string.zodiac_branch_pig
        else -> null
    } ?: return zodiac
    return context.getString(
        R.string.detail_zodiac_value_format,
        context.getString(labels.first),
        context.getString(labels.second)
    )
}

private fun constellationDisplayText(
    context: android.content.Context,
    date: LocalDate
): String {
    return context.getString(
        R.string.detail_constellation_value_format,
        constellationFromDate(date, context).removeSuffix("座"),
        context.getString(constellationElementResId(date))
    )
}

private fun constellationElementResId(date: LocalDate): Int {
    val month = date.monthValue
    val day = date.dayOfMonth
    return when {
        month == 3 && day >= 21 || month == 4 && day <= 19 -> R.string.constellation_element_fire
        month == 4 && day >= 20 || month == 5 && day <= 20 -> R.string.constellation_element_earth
        month == 5 && day >= 21 || month == 6 && day <= 21 -> R.string.constellation_element_air
        month == 6 && day >= 22 || month == 7 && day <= 22 -> R.string.constellation_element_water
        month == 7 && day >= 23 || month == 8 && day <= 22 -> R.string.constellation_element_fire
        month == 8 && day >= 23 || month == 9 && day <= 22 -> R.string.constellation_element_earth
        month == 9 && day >= 23 || month == 10 && day <= 23 -> R.string.constellation_element_air
        month == 10 && day >= 24 || month == 11 && day <= 22 -> R.string.constellation_element_water
        month == 11 && day >= 23 || month == 12 && day <= 21 -> R.string.constellation_element_fire
        month == 12 && day >= 22 || month == 1 && day <= 19 -> R.string.constellation_element_earth
        month == 1 && day >= 20 || month == 2 && day <= 18 -> R.string.constellation_element_air
        else -> R.string.constellation_element_water
    }
}

private fun reminderStatusTitle(
    context: android.content.Context,
    status: ReminderStatusSummary
): String {
    val resId = when (status.messageKey) {
        "reminder_status_off" -> R.string.reminder_status_off
        "reminder_status_notification_permission_needed" -> R.string.reminder_status_notification_permission_needed
        "reminder_status_calendar_permission_needed" -> R.string.reminder_status_calendar_permission_needed
        "reminder_status_no_writable_calendar" -> R.string.reminder_status_no_writable_calendar
        "reminder_status_schedule_sync_failed" -> R.string.reminder_status_schedule_sync_failed
        "reminder_status_app_and_schedule_ready" -> R.string.reminder_status_app_and_schedule_ready
        "reminder_status_app_ready" -> R.string.reminder_status_app_ready
        else -> R.string.reminder_status_schedule_pending
    }
    return context.getString(resId)
}

private fun reminderStatusActionLabel(
    context: android.content.Context,
    status: ReminderStatusSummary
): String? {
    val resId = when (status.primaryAction) {
        ReminderStatusAction.None -> return null
        ReminderStatusAction.EnableReminder -> R.string.reminder_status_action_edit
        ReminderStatusAction.OpenNotificationSettings -> R.string.reminder_status_action_open_settings
        ReminderStatusAction.OpenCalendarSettings -> R.string.reminder_status_action_open_settings
        ReminderStatusAction.DisableScheduleSync -> R.string.reminder_status_action_edit
        ReminderStatusAction.RebuildScheduleSync -> R.string.reminder_status_action_edit
    }
    return context.getString(resId)
}

@Composable
private fun DetailLabelRow(
    label: String,
    value: String,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .widthIn(max = DetailSupplementContentMaxWidth)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = 0.75f),
            modifier = Modifier.width(DetailSupplementLabelWidth)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = 0.95f),
            textAlign = TextAlign.Start,
            modifier = Modifier.widthIn(max = DetailSupplementValueMaxWidth)
        )
    }
}
