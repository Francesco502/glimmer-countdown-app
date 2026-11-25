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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timeapk.R
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.common.categoryColor
import com.example.timeapk.ui.common.categoryLabel
import com.example.timeapk.ui.common.homeCardTitleMaxLines
import com.example.timeapk.ui.common.rememberEventImage
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.InkRuleLine
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.SongPalette
import com.example.timeapk.ui.theme.SongPaperSurface
import com.example.timeapk.ui.theme.SongSeal
import com.example.timeapk.ui.theme.SongSealStyle
import com.example.timeapk.ui.theme.songSealInk
import com.example.timeapk.ui.utils.DisplayModes
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDaysSmart
import com.example.timeapk.ui.utils.formatLunarMonthDay
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import com.example.timeapk.ui.utils.getUntilLabel
import java.text.BreakIterator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.math.abs

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
    val locale = LocalConfiguration.current.locales[0]
    val isRepeating = eventState.event.repeatType != REPEAT_NONE
    val isToday = eventState.daysRemaining == 0L && !eventState.isPast
    val todayLabel = stringResource(R.string.home_today_seal)
    val targetLocalDate = remember(eventState.event.date) { eventDateToLocalDate(eventState.event.date) }
    fun daysUnit(days: Long) = days.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    fun todayTime(): HomeTimeText {
        val until = getUntilLabel(context, eventState, onOccurrence = true)
        return HomeTimeText(todayLabel, "", until.takeUnless { it == context.getString(R.string.days_until_label) }.orEmpty(), isToday = true)
    }
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
            isToday -> todayTime()
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
            todayTime()
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
                label = stringResource(R.string.home_until_milestone, milestoneStr),
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
    val shape = RoundedCornerShape(SongDesignTokens.RadiusXs.dp)
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(
                BorderStroke(SongDesignTokens.BorderWidth.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                shape
            )
            .padding(3.dp)
            .clip(shape)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NearestEventBanner(
    eventState: EventUiState,
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    showHours: Boolean,
    pinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val paper = colors.surface
    val time = resolveHomeTimeText(eventState, today, DisplayModes.UNTIL_DAYS, showHours)
    val nearestLabel = stringResource(R.string.home_nearest_label)
    val dateLine = eventDateLine(eventState, eventState.nextOccurrenceDate, dateFormatter)
    val description = listOf(eventState.event.title, nearestLabel, time.label, time.quantity(), dateLine)
        .filter(String::isNotBlank).joinToString(", ")
    SongPaperSurface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        backgroundColor = paper,
        borderColor = colors.onSurface.copy(alpha = SongDesignTokens.RuleAlpha)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val image = rememberEventImage(eventState.event.imagePath, 900)
            if (image != null) {
                Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                Box(
                    Modifier.matchParentSize().background(
                        Brush.horizontalGradient(
                            0f to paper,
                            0.55f to paper.copy(alpha = 0.96f),
                            1f to paper.copy(alpha = 0.80f)
                        )
                    )
                )
            }
            val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.3f
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(nearestLabel, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    if (pinned) PinnedMark()
                    Spacer(Modifier.weight(1f))
                    Text(dateLine, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val title: @Composable (Modifier) -> Unit = { titleModifier ->
                    Text(
                        text = eventState.event.title,
                        modifier = titleModifier,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (stacked) {
                    title(Modifier.fillMaxWidth())
                    CardCountdown(time, paper, Modifier.fillMaxWidth(), featured = true)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        title(Modifier.weight(1f))
                        CardCountdown(time, paper, Modifier.widthIn(min = 88.dp, max = 148.dp), featured = true)
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
    val colors = MaterialTheme.colorScheme
    val paper = colors.surface
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = AnimationSpecs.responsiveScale(if (isDragging) 1.02f else if (isPressed) 0.98f else 1f),
        animationSpec = AnimationSpecs.springItem,
        label = "cardScale"
    )
    val titleMaxLines = homeCardTitleMaxLines(LocalDensity.current.fontScale)
    val mode = eventDisplayMode(eventState, dateDeltaDisplayMode, showMilestone)
    val time = resolveHomeTimeText(eventState, today, mode, showHours)
    val category = eventState.event.category
    val categoryText = categoryLabel(category)
    val categoryStamp = when (category) {
        CATEGORY_BIRTHDAY -> stringResource(R.string.home_category_birthday_stamp)
        CATEGORY_ANNIVERSARY -> stringResource(R.string.home_category_anniversary_stamp)
        CATEGORY_OTHER -> stringResource(R.string.home_category_countdown_stamp)
        else -> {
            val locale = LocalConfiguration.current.locales[0]
            remember(categoryText, locale) {
                val characters = BreakIterator.getCharacterInstance(locale).apply { setText(categoryText) }
                categoryText.take(characters.next().coerceAtLeast(0))
            }
        }
    }
    val date = if (mode == DisplayModes.PAST_DAYS || mode == DisplayModes.PAST_YMD) {
        eventDateToLocalDate(eventState.event.date)
    } else eventState.nextOccurrenceDate
    val dateLine = eventDateLine(eventState, date, dateFormatter)
    val description = eventDescription(eventState, time, dateLine, pinned)
    val cardAuxiliaryLine = eventAuxiliaryText(eventState, categoryText)
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
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (selected) this.selected = true
                customActions = listOf(CustomAccessibilityAction(toggleDateDeltaDescription) {
                    onToggleDateDeltaDisplayMode()
                    true
                })
            },
        backgroundColor = paper,
        borderColor = when {
            selected -> songSealInk(paper).copy(alpha = 0.75f)
            isDragging -> colors.onSurface.copy(alpha = SongDesignTokens.BorderAlphaStrong)
            else -> colors.onSurface.copy(alpha = SongDesignTokens.RuleAlpha)
        }
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
            val stacked = maxWidth < 284.dp || LocalDensity.current.fontScale >= 1.3f
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SongSeal(
                        text = categoryStamp,
                        modifier = Modifier.clearAndSetSemantics { contentDescription = categoryText },
                        paperColor = paper,
                        textStyle = MaterialTheme.typography.labelSmall,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp)
                    )
                    if (pinned) {
                        Spacer(Modifier.weight(1f))
                        PinnedMark()
                    }
                }
                val details: @Composable (Modifier) -> Unit = { detailsModifier ->
                    Row(detailsModifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = eventState.event.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                                maxLines = if (stacked) maxOf(2, titleMaxLines) else titleMaxLines,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                dateLine, style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            if (showDetail) {
                                Text(
                                    cardAuxiliaryLine, style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        EventThumbnail(eventState.event.imagePath, size = 46)
                    }
                }
                if (stacked) {
                    details(Modifier.fillMaxWidth())
                    CardCountdown(time, paper, Modifier.fillMaxWidth(), onToggle = onToggleDateDeltaDisplayMode)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        details(Modifier.weight(1f))
                        CardCountdown(time, paper, Modifier.widthIn(min = 88.dp, max = 148.dp),
                            onToggle = onToggleDateDeltaDisplayMode)
                    }
                }
            }
        }
    }
}

private fun eventDisplayMode(
    eventState: EventUiState,
    displayMode: Int,
    showMilestone: Boolean
): Int {
    val available = getAvailableDisplayModes(eventState, showMilestone)
    return displayMode.takeIf { it in available } ?: available.first()
}

@Composable
private fun eventDateLine(event: EventUiState, date: LocalDate, formatter: DateTimeFormatter): String {
    val solar = date.format(formatter)
    if (!event.event.isLunar) return solar
    val lunar = formatLunarMonthDay(date, LocalContext.current)
    return if (lunar.isNullOrBlank()) solar else "$solar · $lunar"
}

@Composable
private fun HomeTimeText.quantity(): String =
    if (content.isEmpty() || unit.isEmpty()) content else stringResource(R.string.quantity_with_unit, content, unit)

@Composable
private fun eventDescription(eventState: EventUiState, time: HomeTimeText, date: String, pinned: Boolean): String =
    buildList {
        add(eventState.event.title)
        add(listOf(time.label, time.quantity()).filter(String::isNotBlank).joinToString(" "))
        add(date)
        if (pinned) add(stringResource(R.string.home_pinned))
    }.filter(String::isNotBlank).joinToString(", ")

@Composable
private fun eventAuxiliaryText(eventState: EventUiState, categoryText: String): String = buildList {
    add(categoryText)
    if (eventState.event.isLunar) add(stringResource(R.string.lunar_calendar))
    repeatLabelFor(eventState.event.repeatType)?.let(::add)
    if (eventState.event.remindEnabled) add(stringResource(R.string.field_remind))
}.joinToString(" · ")

@Composable
private fun Modifier.timeToggle(onToggle: (() -> Unit)?): Modifier {
    if (onToggle == null) return this
    val label = stringResource(R.string.cd_toggle_date_delta_display)
    return clickable(role = Role.Button, onClick = onToggle).semantics { contentDescription = label }
}

@Composable
private fun CardCountdown(
    time: HomeTimeText,
    paper: Color,
    modifier: Modifier = Modifier,
    featured: Boolean = false,
    compact: Boolean = false,
    onToggle: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.heightIn(min = 48.dp).timeToggle(onToggle),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        if (time.label.isNotBlank()) {
            Text(
                text = time.label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
        if (time.isToday) {
            SongSeal(
                text = time.content,
                style = if (featured) SongSealStyle.Intaglio else SongSealStyle.Relief,
                stampAnimation = true,
                paperColor = paper,
                textStyle = when {
                    compact -> MaterialTheme.typography.titleSmall
                    featured -> MaterialTheme.typography.headlineMedium
                    else -> MaterialTheme.typography.titleLarge
                }
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val displayStyle = MaterialTheme.typography.displayLarge
                Text(
                    text = time.content,
                    modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                    style = when {
                        compact && time.content.length > 8 -> MaterialTheme.typography.bodyMedium
                        compact -> MaterialTheme.typography.titleMedium
                        time.unit.isEmpty() && time.content.length > 6 -> MaterialTheme.typography.titleLarge
                        featured -> displayStyle.copy(fontSize = displayStyle.fontSize * 1.12f)
                        else -> displayStyle
                    },
                    color = colors.onSurface,
                    maxLines = if (time.unit.isEmpty()) 2 else 1,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis
                )
                if (time.unit.isNotEmpty()) {
                    Text(
                        time.unit, modifier = Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.labelMedium, color = colors.onSurface
                    )
                }
            }
        }
    }
}

@Composable
internal fun EventListItem(
    eventState: EventUiState,
    today: LocalDate,
    dateDeltaDisplayMode: Int,
    onToggleDateDeltaDisplayMode: (() -> Unit)?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    showHours: Boolean = true,
    showMilestone: Boolean = true,
    isDragging: Boolean = false,
    pinned: Boolean = false,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val date = eventState.nextOccurrenceDate
    val mode = eventDisplayMode(eventState, dateDeltaDisplayMode, showMilestone)
    EventLedgerRow(
        eventState = eventState,
        date = date,
        time = resolveHomeTimeText(eventState, today, mode, showHours),
        onClick = onClick,
        onLongClick = onLongClick,
        onToggleTime = onToggleDateDeltaDisplayMode,
        isDragging = isDragging,
        pinned = pinned,
        selected = selected,
        modifier = modifier
    )
}

@Composable
internal fun CalendarOccurrenceRow(
    occurrence: CalendarEventOccurrence,
    today: LocalDate,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val locale = LocalConfiguration.current.locales[0]
    val delta = ChronoUnit.DAYS.between(today, occurrence.date)
    val days = abs(delta)
    val time = if (delta == 0L) {
        HomeTimeText(stringResource(R.string.home_today_seal), "", "", isToday = true)
    } else {
        HomeTimeText(
            content = formatDaysSmart(days, false, locale),
            unit = pluralStringResource(R.plurals.days_unit, days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
            label = stringResource(if (delta > 0L) R.string.days_until_label else R.string.days_past_label),
            isToday = false
        )
    }
    EventLedgerRow(occurrence.eventState, occurrence.date, time, onClick, onLongClick)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventLedgerRow(
    eventState: EventUiState,
    date: LocalDate,
    time: HomeTimeText,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onToggleTime: (() -> Unit)? = null,
    isDragging: Boolean = false,
    pinned: Boolean = false,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = remember(date, locale) {
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
    val description = eventDescription(eventState, time, dateLabel, pinned)
    val category = eventState.event.category
    val auxiliary = eventAuxiliaryText(eventState, categoryLabel(category))
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = AnimationSpecs.responsiveScale(if (isDragging) 1.02f else if (pressed) 0.98f else 1f),
        animationSpec = AnimationSpecs.springItem,
        label = "listItemScale"
    )
    val rowBackground = when {
        isDragging -> colors.surface
        pressed -> colors.onBackground.copy(alpha = 0.05f)
        else -> Color.Transparent
    }
    val paper = HomeEventColorPolicy.compositeOver(rowBackground, colors.background)
    val categoryColor = categoryColor(category)
    val marker = remember(categoryColor, colors.onBackground, paper) {
        HomeEventColorPolicy.categoryMarker(categoryColor, colors.onBackground, paper)
    }
    val timeLabel = stringResource(R.string.cd_toggle_date_delta_display)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(rowBackground, MaterialTheme.shapes.small)
            .combinedClickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (selected) this.selected = true
                if (onToggleTime != null) {
                    customActions = listOf(CustomAccessibilityAction(timeLabel) {
                        onToggleTime()
                        true
                    })
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(SongDesignTokens.ListDateColumnWidth.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selected) {
                    SongSeal(
                        "${date.monthValue}/${date.dayOfMonth}", paperColor = paper,
                        textStyle = MaterialTheme.typography.titleSmall,
                        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 1.dp)
                    )
                } else {
                    Text("${date.monthValue}/${date.dayOfMonth}", style = MaterialTheme.typography.titleSmall,
                        color = colors.onBackground, maxLines = 1, softWrap = false)
                }
                Text(
                    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (pinned) PinnedMark()
                    Text(
                        eventState.event.title, style = MaterialTheme.typography.titleMedium,
                        color = colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(Modifier.size(5.dp).background(marker, CircleShape))
                    Text(
                        auxiliary, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            CardCountdown(time, paper, Modifier.widthIn(min = 88.dp, max = 132.dp), compact = true, onToggle = onToggleTime)
        }
        InkRuleLine(Modifier.padding(start = 48.dp))
    }
}
