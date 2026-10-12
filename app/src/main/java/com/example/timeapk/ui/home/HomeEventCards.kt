package com.example.timeapk.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timeapk.R
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.common.categoryLabel
import com.example.timeapk.ui.common.homeCardTitleMaxLines
import com.example.timeapk.ui.common.rememberEventImage
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.ColorContrastGuardrail
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.SongPalette
import com.example.timeapk.ui.theme.SongPaperSurface
import com.example.timeapk.ui.theme.SongSealLabel
import com.example.timeapk.ui.utils.DisplayModes
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDaysSmart
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import com.example.timeapk.ui.utils.getUntilLabel
import com.example.timeapk.ui.utils.parseEventColorOrFallback
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Resolved text for the time column of a card or list row. */
private data class HomeTimeText(
    val content: String,
    val unit: String,
    val label: String,
    val isToday: Boolean
)

@Composable
private fun resolveHomeTimeText(
    eventState: EventUiState,
    today: LocalDate,
    mode: Int,
    showHours: Boolean
): HomeTimeText {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val isRepeating = eventState.event.repeatType != REPEAT_NONE
    val isToday = eventState.daysRemaining == 0L && !eventState.isPast
    val todayLabel = stringResource(R.string.home_today_seal)
    val targetLocalDate = remember(eventState.event.date) { eventDateToLocalDate(eventState.event.date) }
    fun daysUnit(days: Long) = days.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    return when (mode) {
        DisplayModes.PAST_DAYS -> {
            val days = if (isRepeating) eventState.daysPassed else eventState.daysElapsed
            HomeTimeText(
                content = formatDaysSmart(days, false, locale),
                unit = pluralStringResource(R.plurals.days_unit, daysUnit(days)),
                label = stringResource(R.string.days_past_label),
                isToday = false
            )
        }
        DisplayModes.PAST_YMD -> HomeTimeText(
            content = formatBetweenAsYMD(targetLocalDate, today, locale),
            unit = "",
            label = stringResource(R.string.days_past_label),
            isToday = false
        )
        DisplayModes.UNTIL_DAYS -> when {
            showHours && eventState.hoursRemaining != null -> HomeTimeText(
                content = String.format(locale, "%d", eventState.hoursRemaining),
                unit = pluralStringResource(R.plurals.hours_unit, eventState.hoursRemaining.toInt()),
                label = getUntilLabel(context, eventState),
                isToday = false
            )
            isToday -> HomeTimeText(todayLabel, "", "", isToday = true)
            else -> {
                val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
                HomeTimeText(
                    content = formatDaysSmart(days, false, locale),
                    unit = pluralStringResource(R.plurals.days_unit, daysUnit(days)),
                    label = getUntilLabel(context, eventState),
                    isToday = false
                )
            }
        }
        DisplayModes.UNTIL_YMD -> if (isToday) {
            HomeTimeText(todayLabel, "", "", isToday = true)
        } else {
            val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
            HomeTimeText(
                content = formatBetweenAsYMD(today, today.plusDays(days), locale),
                unit = "",
                label = getUntilLabel(context, eventState),
                isToday = false
            )
        }
        DisplayModes.MILESTONE -> if (eventState.nextMilestoneDays != null && eventState.nextMilestoneValue != null) {
            val milestoneStr = milestoneLabel(eventState.nextMilestoneValue, eventState.nextMilestoneReason, eventState.nextMilestoneYears)
            HomeTimeText(
                content = formatDaysSmart(eventState.nextMilestoneDays, false, locale),
                unit = pluralStringResource(R.plurals.days_unit, daysUnit(eventState.nextMilestoneDays)),
                label = stringResource(R.string.milestone_label_prefix, milestoneStr),
                isToday = false
            )
        } else {
            HomeTimeText("", "", stringResource(R.string.milestone_none), isToday = false)
        }
        else -> HomeTimeText("", "", "", isToday = false)
    }
}

@Composable
private fun repeatLabelFor(repeatType: String): String? = when (repeatType) {
    REPEAT_DAILY -> stringResource(R.string.repeat_daily)
    REPEAT_WEEKLY -> stringResource(R.string.repeat_weekly)
    REPEAT_MONTHLY -> stringResource(R.string.repeat_monthly)
    REPEAT_HALF_YEARLY -> stringResource(R.string.repeat_half_yearly)
    REPEAT_YEARLY -> stringResource(R.string.repeat_yearly)
    else -> null
}

/** 朱印式“今天”：实心朱红方印，比纯文字更醒目。 */
@Composable
internal fun TodaySeal(
    text: String,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val sealColor = MaterialTheme.colorScheme.primary
    val textColor = ColorContrastGuardrail.ensureReadableText(
        preferred = MaterialTheme.colorScheme.onPrimary,
        background = sealColor
    )
    Box(
        modifier = modifier
            .background(sealColor, RoundedCornerShape(SongDesignTokens.RadiusSm.dp))
            .border(
                BorderStroke(1.dp, textColor.copy(alpha = 0.35f)),
                RoundedCornerShape(SongDesignTokens.RadiusSm.dp)
            )
            .padding(
                horizontal = if (large) 16.dp else 12.dp,
                vertical = if (large) 8.dp else 5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun PinnedMark(modifier: Modifier = Modifier) {
    val pinnedDescription = stringResource(R.string.home_pinned)
    SongLineIcon(
        kind = SongLineIconKind.Pin,
        contentDescription = pinnedDescription,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.86f),
        size = 14.dp,
        modifier = modifier
    )
}

@Composable
private fun EventThumbnail(imagePath: String?, size: Int, modifier: Modifier = Modifier) {
    val sizePx = with(LocalDensity.current) { size.dp.roundToPx() * 2 }
    val image = rememberEventImage(imagePath, sizePx) ?: return
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(SongDesignTokens.RadiusSm.dp))
            .border(
                BorderStroke(SongDesignTokens.BorderWidth.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                RoundedCornerShape(SongDesignTokens.RadiusSm.dp)
            )
    )
}

@Composable
internal fun EmptyState(
    kind: HomeEmptyStateKind,
    onFirstEventClick: () -> Unit,
    onClearConstraints: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        when (kind) {
            HomeEmptyStateKind.FirstEvent -> {
                val firstEventCta = stringResource(R.string.home_empty_first_event_cta)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SongDesignTokens.SpaceLg.dp)
                ) {
                    EmptyPaperIllustration(modifier = Modifier.size(width = 132.dp, height = 96.dp))
                    Text(
                        text = stringResource(R.string.home_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.home_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(SongDesignTokens.SpaceXs.dp))
                    Surface(
                        onClick = onFirstEventClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = firstEventCta
                            },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SongLineIcon(
                                kind = SongLineIconKind.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                size = 24.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = firstEventCta,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            HomeEmptyStateKind.NoMatches -> {
                val noMatches = stringResource(R.string.home_empty_no_matches)
                val clearConstraints = stringResource(R.string.home_empty_clear_constraints)
                Surface(
                    onClick = onClearConstraints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = clearConstraints
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        SongDesignTokens.BorderWidth.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = SongDesignTokens.BorderAlphaStrong)
                    ),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SongLineIcon(
                            kind = SongLineIconKind.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 24.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = noMatches,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = clearConstraints,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/** 线描纸笺与朱印：空状态插画，纯矢量绘制，随主题着色。 */
@Composable
private fun EmptyPaperIllustration(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f)
    val faint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
    val seal = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.2.dp.toPx())
        val w = size.width
        val h = size.height
        // Back slip, slightly rotated by offset corners.
        drawPath(
            Path().apply {
                moveTo(w * 0.22f, h * 0.12f)
                lineTo(w * 0.86f, h * 0.06f)
                lineTo(w * 0.90f, h * 0.80f)
                lineTo(w * 0.26f, h * 0.86f)
                close()
            },
            color = faint,
            style = stroke
        )
        // Front slip.
        drawRect(
            color = ink,
            topLeft = Offset(w * 0.12f, h * 0.18f),
            size = androidx.compose.ui.geometry.Size(w * 0.62f, h * 0.76f),
            style = stroke
        )
        listOf(0.38f, 0.52f, 0.66f).forEachIndexed { index, y ->
            drawLine(
                color = faint,
                start = Offset(w * 0.20f, h * y),
                end = Offset(w * (if (index == 2) 0.46f else 0.64f), h * y),
                strokeWidth = 1.dp.toPx()
            )
        }
        // Seal.
        drawRect(
            color = seal.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.58f, h * 0.70f),
            size = androidx.compose.ui.geometry.Size(w * 0.13f, w * 0.13f)
        )
    }
}

/** 首页顶部“最近”横幅：最近一个未来事件，数字更大，作为首屏焦点。 */
@Composable
internal fun NearestEventBanner(
    eventState: EventUiState,
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    showHours: Boolean,
    pinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val time = resolveHomeTimeText(eventState, today, DisplayModes.UNTIL_DAYS, showHours)
    val accent = parseEventColorOrFallback(eventState.event.colorHex, MaterialTheme.colorScheme.primary)
    val nearestLabel = stringResource(R.string.home_nearest_label)
    val dateLine = eventState.nextOccurrenceDate.format(dateFormatter)
    val description = buildString {
        append(nearestLabel).append(", ").append(eventState.event.title).append(", ")
        if (time.isToday) append(time.content)
        else append(time.label).append(" ").append(
            if (time.unit.isEmpty()) time.content else stringResource(R.string.quantity_with_unit, time.content, time.unit)
        )
    }
    SongPaperSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        backgroundColor = lerp(MaterialTheme.colorScheme.surface, accent, 0.06f),
        borderColor = accent.copy(alpha = 0.36f)
    ) {
        Box {
            val image = rememberEventImage(eventState.event.imagePath, 900)
            if (image != null) {
                androidx.compose.foundation.Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.16f,
                    modifier = Modifier.matchParentSize()
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(SongDesignTokens.SpaceSm.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SongSealLabel(text = nearestLabel, color = accent)
                    if (pinned) {
                        Spacer(Modifier.width(SongDesignTokens.SpaceSm.dp))
                        PinnedMark()
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = dateLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = eventState.event.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(bottom = 6.dp)
                    )
                    Spacer(Modifier.width(SongDesignTokens.SpaceMd.dp))
                    if (time.isToday) {
                        TodaySeal(text = time.content, large = true)
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            if (time.label.isNotBlank()) {
                                Text(
                                    text = time.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = time.content,
                                    style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.25f),
                                    color = accent.let { if (it.luminance() > 0.6f) MaterialTheme.colorScheme.onSurface else it },
                                    maxLines = 1
                                )
                                if (time.unit.isNotEmpty()) {
                                    Spacer(Modifier.width(if (locale.language == "en") 6.dp else 3.dp))
                                    Text(
                                        text = time.unit,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(
    eventState: EventUiState,
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    sortType: SortType,
    dateDeltaDisplayMode: Int,
    onToggleDateDeltaDisplayMode: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    showHours: Boolean = true,
    showMilestone: Boolean = true,
    showDetail: Boolean = true,
    isDragging: Boolean = false,
    pinned: Boolean = false,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isPast = eventState.isPast
    val baseCardColor = parseEventColorOrFallback(
        hex = eventState.event.colorHex,
        fallback = MaterialTheme.colorScheme.primary
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = AnimationSpecs.responsiveScale(if (isDragging) 1.02f else if (isPressed) 0.98f else 1f),
        animationSpec = AnimationSpecs.springItem,
        label = "cardScale"
    )
    val lightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val cardContainerColor = when {
        isPast && lightSurface -> SongPalette.PaperMuted
        isPast -> MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
        else -> MaterialTheme.colorScheme.surface
    }
    val cardContentColor = lerp(MaterialTheme.colorScheme.onSurface, baseCardColor, 0.06f)
    val effectiveCardBackground = HomeEventColorPolicy.compositeOver(
        foreground = cardContainerColor,
        background = MaterialTheme.colorScheme.background
    )
    val cardOnSurfaceColor = MaterialTheme.colorScheme.onSurface
    val cardAuxiliaryColor = if (lightSurface) {
        baseCardColor.copy(alpha = if (isPast) 0.54f else 0.74f)
    } else {
        remember(baseCardColor, cardOnSurfaceColor, effectiveCardBackground) {
            HomeEventColorPolicy.ensureTextContrast(
                eventColor = baseCardColor,
                onSurface = cardOnSurfaceColor,
                background = effectiveCardBackground
            )
        }
    }
    val cardAccentColor = baseCardColor.copy(alpha = if (isPast) 0.30f else 0.62f)
    val cardBorderColor = baseCardColor.copy(alpha = if (isPast) 0.16f else 0.22f)

    val locale = LocalContext.current.resources.configuration.locales[0]
    val titleMaxLines = homeCardTitleMaxLines(LocalDensity.current.fontScale)
    val targetLocalDate = remember(eventState.event.date) {
        eventDateToLocalDate(eventState.event.date)
    }

    val resolvedDateDeltaDisplayMode = resolveHomeDateDeltaDisplayMode(
        sortType = sortType,
        requestedMode = dateDeltaDisplayMode,
        eventState = eventState
    )
    val availableModes = getAvailableDisplayModes(eventState, showMilestone)
    val modeIndex = availableModes.indexOf(resolvedDateDeltaDisplayMode)
    val mode = if (modeIndex != -1) resolvedDateDeltaDisplayMode else availableModes.first()
    val dateDeltaToggleEnabled = sortType != SortType.ByDays
    val time = resolveHomeTimeText(eventState, today, mode, showHours)
    val isToday = time.isToday

    val cardDescription = buildString {
        append(eventState.event.title)
        append(", ")
        if (isToday) append(time.content)
        else append(time.label).append(" ").append(
            if (time.unit.isEmpty()) time.content
            else stringResource(R.string.quantity_with_unit, time.content, time.unit)
        )
        if (pinned) append(", ").append(stringResource(R.string.home_pinned))
    }
    val categoryText = categoryLabel(eventState.event.category)
    val repeatLabel = repeatLabelFor(eventState.event.repeatType)
    val cardAuxiliaryLine = buildList {
        add(categoryText)
        if (eventState.event.isLunar) add(stringResource(R.string.lunar_calendar))
        repeatLabel?.let(::add)
        if (eventState.event.remindEnabled) add(stringResource(R.string.field_remind))
    }.joinToString(" · ")
    val toggleDateDeltaDescription = stringResource(R.string.cd_toggle_date_delta_display)

    SongPaperSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics(mergeDescendants = true) { contentDescription = cardDescription },
        backgroundColor = cardContainerColor,
        borderColor = when {
            isDragging || selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
            else -> cardBorderColor
        }
    ) {
        Box {
            if (pinned) {
                PinnedMark(modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 10.dp))
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.3f
                val accentWidth = if (isToday) 4.dp else 3.dp
                val titleContent: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = eventState.event.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = cardContentColor.copy(alpha = if (isPast) 0.8f else 1.0f),
                            maxLines = if (stacked) maxOf(2, titleMaxLines) else titleMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )

                        val dateLine = targetLocalDate.format(dateFormatter)
                        val dateLineStyle = if (dateLine.length > 12) {
                            MaterialTheme.typography.labelSmall
                        } else {
                            MaterialTheme.typography.bodySmall
                        }
                        Text(
                            text = dateLine,
                            style = dateLineStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (showDetail && cardAuxiliaryLine.isNotBlank()) {
                            Text(
                                text = cardAuxiliaryLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = cardAuxiliaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                val timeContent: @Composable () -> Unit = {
                    val timeColor = if (isPast) cardContentColor.copy(alpha = 0.85f) else cardContentColor
                    Box(
                        modifier = Modifier
                            .then(if (stacked) Modifier.fillMaxWidth() else Modifier.widthIn(min = 72.dp, max = maxWidth * 0.48f))
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .then(
                                if (dateDeltaToggleEnabled) {
                                    Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = LocalIndication.current,
                                            onClick = onToggleDateDeltaDisplayMode
                                        )
                                        .semantics {
                                            role = Role.Button
                                            contentDescription = toggleDateDeltaDescription
                                        }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (isToday) {
                            TodaySeal(text = time.content)
                        } else {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.heightIn(min = 52.dp)
                            ) {
                                if (time.label.isNotBlank()) {
                                    Text(
                                        text = time.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = timeColor.copy(alpha = 0.72f),
                                        letterSpacing = 0.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = time.content,
                                        style = if (time.unit.isEmpty() && time.content.length > 6) {
                                            MaterialTheme.typography.titleLarge
                                        } else {
                                            MaterialTheme.typography.displayLarge
                                        },
                                        color = timeColor,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = if (stacked && time.unit.isEmpty()) 2 else 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (time.unit.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(if (locale.language == "en") 6.dp else 3.dp))
                                        Text(
                                            text = time.unit,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = timeColor,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val thumbnail: @Composable () -> Unit = {
                    EventThumbnail(eventState.event.imagePath, size = 44)
                }
                if (stacked) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(accentWidth).height(44.dp).background(cardAccentColor, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(12.dp))
                            Box(Modifier.weight(1f)) { titleContent() }
                            thumbnail()
                        }
                        timeContent()
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(accentWidth).height(52.dp).background(cardAccentColor, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(16.dp))
                        if (eventState.event.imagePath != null) {
                            thumbnail()
                            Spacer(Modifier.width(12.dp))
                        }
                        Box(Modifier.weight(1f)) { titleContent() }
                        Spacer(Modifier.width(16.dp))
                        timeContent()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EventListItem(
    eventState: EventUiState,
    today: LocalDate,
    sortType: SortType,
    dateDeltaDisplayMode: Int,
    onToggleDateDeltaDisplayMode: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    showHours: Boolean = true,
    showMilestone: Boolean = true,
    isDragging: Boolean = false,
    pinned: Boolean = false,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = AnimationSpecs.responsiveScale(if (isDragging) 1.02f else if (isPressed) 0.98f else 1f),
        animationSpec = AnimationSpecs.springItem,
        label = "listItemScale"
    )
    val isPast = eventState.isPast

    val resolvedDateDeltaDisplayMode = resolveHomeDateDeltaDisplayMode(
        sortType = sortType,
        requestedMode = dateDeltaDisplayMode,
        eventState = eventState
    )
    val availableModes = getAvailableDisplayModes(eventState, showMilestone = showMilestone)
    val modeIndex = availableModes.indexOf(resolvedDateDeltaDisplayMode)
    val mode = if (modeIndex != -1) resolvedDateDeltaDisplayMode else availableModes.first()
    val dateDeltaToggleEnabled = sortType != SortType.ByDays
    val time = resolveHomeTimeText(eventState, today, mode, showHours)
    val isToday = time.isToday
    val labelText = time.label
    val daysDisplay = when {
        time.content.isEmpty() -> ""
        time.unit.isEmpty() -> time.content
        else -> stringResource(R.string.quantity_with_unit, time.content, time.unit)
    }

    val eventColor = parseEventColorOrFallback(
        hex = eventState.event.colorHex,
        fallback = MaterialTheme.colorScheme.primary
    )
    val itemDescription = buildString {
        append(eventState.event.title)
        append(", ")
        if (isToday) append(time.content)
        else append(labelText).append(" ").append(daysDisplay)
        if (pinned) append(", ").append(stringResource(R.string.home_pinned))
    }
    val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val rowBackground = when {
        isDragging -> MaterialTheme.colorScheme.surface
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else -> Color.Transparent
    }
    val effectiveListBackground = HomeEventColorPolicy.compositeOver(
        foreground = rowBackground,
        background = MaterialTheme.colorScheme.background
    )
    val listOnBackgroundColor = MaterialTheme.colorScheme.onBackground
    val displayColor = if (isLightSurface) {
        lerp(eventColor, listOnBackgroundColor, 0.4f)
    } else {
        remember(eventColor, listOnBackgroundColor, effectiveListBackground) {
            HomeEventColorPolicy.ensureTextContrast(
                eventColor = eventColor,
                onSurface = listOnBackgroundColor,
                background = effectiveListBackground
            )
        }
    }

    val baseTextColorListItem = MaterialTheme.colorScheme.onSurface
    val itemContentColor = lerp(baseTextColorListItem, eventColor, 0.04f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(color = rowBackground, shape = MaterialTheme.shapes.medium)
            .then(
                if (isDragging) Modifier.border(SongDesignTokens.BorderWidth.dp, MaterialTheme.colorScheme.primary.copy(alpha = SongDesignTokens.BorderAlphaStrong), MaterialTheme.shapes.medium)
                else Modifier
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics(mergeDescendants = true) { contentDescription = itemDescription }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(if (isToday) 3.dp else 2.dp)
                    .height(18.dp)
                    .background(
                        color = eventColor.copy(alpha = if (isPast) 0.26f else 0.50f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (pinned) {
                PinnedMark()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = eventState.event.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = 0.sp
                ),
                color = itemContentColor.copy(alpha = if (isPast) 0.84f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            if (isToday) {
                TodaySeal(text = time.content)
            } else {
                CompactEventTime(
                    daysDisplay = daysDisplay,
                    labelText = labelText,
                    timeColor = if (isPast && isLightSurface) {
                        displayColor.copy(alpha = 0.82f)
                    } else {
                        displayColor
                    },
                    labelColor = itemContentColor.copy(alpha = 0.64f),
                    contentDescription = stringResource(R.string.cd_toggle_date_delta_display),
                    enabled = dateDeltaToggleEnabled,
                    onClick = onToggleDateDeltaDisplayMode
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 24.dp),
            thickness = SongDesignTokens.BorderWidth.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun CompactEventTime(
    daysDisplay: String,
    labelText: String,
    timeColor: Color,
    labelColor: Color,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 82.dp, max = 138.dp)
            .heightIn(min = 42.dp)
            .then(
                if (enabled) {
                    Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = onClick
                        )
                        .semantics {
                            role = Role.Button
                            this.contentDescription = contentDescription
                        }
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        if (labelText.isNotBlank()) {
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (daysDisplay.isNotBlank()) {
            Text(
                text = daysDisplay,
                style = when {
                    daysDisplay.length > 12 -> MaterialTheme.typography.bodySmall
                    daysDisplay.length > 8 -> MaterialTheme.typography.bodyMedium
                    else -> MaterialTheme.typography.titleLarge
                },
                color = timeColor,
                maxLines = if (daysDisplay.length > 12) 3 else 2,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
