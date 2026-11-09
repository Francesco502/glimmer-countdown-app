package com.example.timeapk.ui.home

import android.os.Build
import android.view.View
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.common.categoryLabel
import com.example.timeapk.ui.common.categoryOptions
import com.example.timeapk.ui.components.SongConfirmDialog
import com.example.timeapk.ui.sound.SongSoundEffect
import com.example.timeapk.ui.sound.rememberSongSoundscape
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.SongModeTabRow
import com.example.timeapk.ui.theme.SongPaperSurface
import com.example.timeapk.ui.theme.SongSealLabel
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import com.example.timeapk.ui.utils.getDisplayDateFormatter
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.time.LocalDate
import java.time.ZoneOffset

private val HomeOverflowActionItemHeight = 48.dp

// Compose 1.6's Popup handles legacy Back key events. Register on the popup's
// own native window as well, so Android 13+ predictive Back dismisses it.
@RequiresApi(33)
@Composable
internal fun HomeToolsPopupBackHandler(onDismiss: () -> Unit) {
    val popupView = LocalView.current
    val currentDismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(popupView) {
        var registeredDispatcher: OnBackInvokedDispatcher? = null
        val callback = OnBackInvokedCallback { currentDismiss() }
        fun unregister() {
            registeredDispatcher?.unregisterOnBackInvokedCallback(callback)
            registeredDispatcher = null
        }
        fun register() {
            if (registeredDispatcher == null) {
                popupView.findOnBackInvokedDispatcher()?.let { dispatcher ->
                    dispatcher.registerOnBackInvokedCallback(
                        OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                        callback
                    )
                    registeredDispatcher = dispatcher
                }
            }
        }
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = register()
            override fun onViewDetachedFromWindow(view: View) = unregister()
        }
        popupView.addOnAttachStateChangeListener(listener)
        if (popupView.isAttachedToWindow) register()
        onDispose {
            popupView.removeOnAttachStateChangeListener(listener)
            unregister()
        }
    }
}

@Composable
internal fun HomeDisplayModeSegmentedControl(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        0 to stringResource(R.string.display_mode_card),
        1 to stringResource(R.string.display_mode_list),
        2 to stringResource(R.string.display_mode_calendar)
    )
    SongModeTabRow(
        options = modes,
        selected = selectedMode,
        onSelected = onModeSelected,
        modifier = modifier
    )
}

/** Seal-styled add button: a solid vermilion square that reads as the primary action. */
@Composable
internal fun HomeAddButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(SongDesignTokens.FabSize.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            SongLineIcon(
                kind = SongLineIconKind.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                size = SongDesignTokens.IconLarge.dp,
                strokeWidth = 1.8.dp
            )
        }
    }
}

@Composable
internal fun HomeOneTimeHint(text: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.home_hint_dismiss))
        }
    }
}

@Composable
internal fun HomeEventActionMenu(
    expanded: Boolean,
    pinned: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(if (pinned) R.string.button_unpin else R.string.button_pin)) },
            leadingIcon = { SongLineIcon(SongLineIconKind.Pin, size = SongDesignTokens.IconSmall.dp) },
            onClick = onTogglePin
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.button_edit)) },
            leadingIcon = { SongLineIcon(SongLineIconKind.Edit, size = SongDesignTokens.IconSmall.dp) },
            onClick = onEdit
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.button_delete), color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
                SongLineIcon(
                    SongLineIconKind.Delete,
                    size = SongDesignTokens.IconSmall.dp,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = onDelete
        )
    }
}

@Composable
internal fun HomeOverflowActionMenu(
    selectedBucket: TimelineBucketType?,
    categoryFilter: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    // Sort order is a persistent preference, not a filter, so it never lights the icon.
    val hasActiveTool = selectedBucket != null || categoryFilter != null
    val soundscape = rememberSongSoundscape()
    Box {
        InlineActionIconButton(
            icon = SongLineIconKind.Filter,
            contentDescription = stringResource(R.string.home_tools_action),
            active = hasActiveTool || expanded,
            onClick = {
                soundscape.play(SongSoundEffect.Action)
                onExpandedChange(!expanded)
            }
        )
    }
}

@Composable
internal fun HomeOverflowPanel(
    digest: HomeTimelineDigest,
    selectedBucket: TimelineBucketType?,
    categoryFilter: String?,
    sortType: SortType,
    onBucketClick: (TimelineBucketType) -> Unit,
    onCategoryClick: (String?) -> Unit,
    onSortClick: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    val soundscape = rememberSongSoundscape()
    fun playActionThen(action: () -> Unit) {
        soundscape.play(SongSoundEffect.Action)
        action()
    }

    SongActionSlip(
        modifier = modifier,
        content = {
            HomeMenuSectionLabel(text = stringResource(R.string.home_timeline_action_menu))
            TimelineActionTileGrid(
                items = listOf(
                    TimelineActionTileSpec(
                        label = stringResource(R.string.home_timeline_today),
                        bucket = digest.today,
                        selected = selectedBucket == TimelineBucketType.Today,
                        onClick = { playActionThen { onBucketClick(TimelineBucketType.Today) } }
                    ),
                    TimelineActionTileSpec(
                        label = stringResource(R.string.home_timeline_seven_days),
                        bucket = digest.sevenDays,
                        selected = selectedBucket == TimelineBucketType.SevenDays,
                        onClick = { playActionThen { onBucketClick(TimelineBucketType.SevenDays) } }
                    ),
                    TimelineActionTileSpec(
                        label = stringResource(R.string.home_timeline_month),
                        bucket = digest.month,
                        selected = selectedBucket == TimelineBucketType.Month,
                        onClick = { playActionThen { onBucketClick(TimelineBucketType.Month) } }
                    ),
                    TimelineActionTileSpec(
                        label = stringResource(R.string.home_timeline_milestone),
                        bucket = digest.milestone,
                        selected = selectedBucket == TimelineBucketType.Milestone,
                        onClick = { playActionThen { onBucketClick(TimelineBucketType.Milestone) } }
                    )
                )
            )
            SongActionSlipDivider()
            HomeMenuSectionLabel(text = stringResource(R.string.home_filter_panel_toggle))
            SongActionOptionGrid(
                items = listOf(
                    SongActionOptionSpec(
                        label = stringResource(R.string.filter_all),
                        selected = categoryFilter == null,
                        onClick = { playActionThen { onCategoryClick(null) } }
                    )
                ) + categoryOptions().map { option ->
                    SongActionOptionSpec(
                        label = option.label,
                        selected = categoryFilter == option.id,
                        onClick = { playActionThen { onCategoryClick(option.id) } }
                    )
                }
            )
            SongActionSlipDivider()
            HomeMenuSectionLabel(text = stringResource(R.string.sort_menu))
            SongActionOptionGrid(
                items = listOf(
                    SongActionOptionSpec(
                        label = stringResource(R.string.sort_custom),
                        selected = sortType == SortType.Custom,
                        onClick = { playActionThen { onSortClick(SortType.Custom) } }
                    ),
                    SongActionOptionSpec(
                        label = stringResource(R.string.sort_by_days),
                        selected = sortType == SortType.ByDays,
                        onClick = { playActionThen { onSortClick(SortType.ByDays) } }
                    ),
                    SongActionOptionSpec(
                        label = stringResource(R.string.sort_by_date),
                        selected = sortType == SortType.ByDate,
                        onClick = { playActionThen { onSortClick(SortType.ByDate) } }
                    )
                )
            )
        }
    )
}

@Composable
internal fun HomeSearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp)
    val textColor = MaterialTheme.colorScheme.onSurface
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .focusRequester(focusRequester)
            .border(
                BorderStroke(
                    SongDesignTokens.BorderWidth.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
                ),
                shape
            )
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(horizontal = 10.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SongLineIcon(
                    kind = SongLineIconKind.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    size = SongDesignTokens.IconSmall.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = { onSearchQueryChange("") }),
                        contentAlignment = Alignment.Center
                    ) {
                        SongLineIcon(
                            kind = SongLineIconKind.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = SongDesignTokens.IconSmall.dp
                        )
                    }
                }
            }
        }
    )
}

private data class TimelineActionTileSpec(
    val label: String,
    val bucket: TimelineBucket,
    val selected: Boolean,
    val onClick: () -> Unit
)

internal data class SongActionOptionSpec(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun SongActionSlip(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    SongPaperSurface(
        modifier = modifier
            .widthIn(min = 228.dp, max = 276.dp)
            .heightIn(max = 460.dp)
            .padding(end = 8.dp),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
    ) {
        Box {
            SongActionSlipFoldDecoration(modifier = Modifier.matchParentSize())
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, top = 12.dp, end = 18.dp, bottom = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SongActionSlipFoldDecoration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier) {
        val fold = 30.dp.toPx()
        val right = size.width
        val path = Path().apply {
            moveTo(right - fold, 0f)
            lineTo(right, 0f)
            lineTo(right, fold)
            close()
        }
        drawPath(path, primary.copy(alpha = 0.055f))
        drawLine(
            color = primary.copy(alpha = 0.22f),
            start = Offset(right - fold, 0f),
            end = Offset(right, fold),
            strokeWidth = SongDesignTokens.BorderWidth.dp.toPx()
        )
        drawLine(
            color = outline.copy(alpha = 0.10f),
            start = Offset(right - 8.dp.toPx(), fold + 8.dp.toPx()),
            end = Offset(right - 8.dp.toPx(), size.height - 10.dp.toPx()),
            strokeWidth = SongDesignTokens.BorderWidth.dp.toPx()
        )
        drawPath(
            Path().apply {
                moveTo(14.dp.toPx(), 8.dp.toPx())
                cubicTo(
                    64.dp.toPx(), 4.dp.toPx(),
                    size.width - 72.dp.toPx(), 10.dp.toPx(),
                    size.width - 44.dp.toPx(), 6.dp.toPx()
                )
            },
            color = outline.copy(alpha = 0.12f),
            style = Stroke(width = SongDesignTokens.BorderWidth.dp.toPx())
        )
    }
}

@Composable
private fun SongActionSlipDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.72f),
            thickness = SongDesignTokens.BorderWidth.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f)
        )
    }
}

@Composable
private fun HomeMenuSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TimelineActionTileGrid(items: List<TimelineActionTileSpec>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    TimelineActionTile(
                        spec = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TimelineActionTile(
    spec: TimelineActionTileSpec,
    modifier: Modifier = Modifier
) {
    val color = if (spec.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp)
    Column(
        modifier = modifier
            .border(
                BorderStroke(
                    SongDesignTokens.BorderWidth.dp,
                    if (spec.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                ),
                shape
            )
            .background(
                if (spec.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.075f)
                else MaterialTheme.colorScheme.background,
                shape
            )
            .clickable(onClick = spec.onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = spec.label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            SongSealLabel(
                text = pluralStringResource(
                    R.plurals.home_timeline_count_format,
                    spec.bucket.count,
                    spec.bucket.count
                ),
                color = color.copy(alpha = if (spec.selected) 0.88f else 0.62f)
            )
            spec.bucket.topItem?.event?.title?.let { title ->
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun SongActionOptionGrid(items: List<SongActionOptionSpec>) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    SongActionOptionTile(
                        spec = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SongActionOptionTile(
    spec: SongActionOptionSpec,
    modifier: Modifier = Modifier
) {
    val color = if (spec.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp)
    Row(
        modifier = modifier
            .heightIn(min = HomeOverflowActionItemHeight)
            .border(
                BorderStroke(
                    SongDesignTokens.BorderWidth.dp,
                    if (spec.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                ),
                shape
            )
            .background(
                if (spec.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                else MaterialTheme.colorScheme.background,
                shape
            )
            .selectable(
                selected = spec.selected,
                onClick = spec.onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = spec.label,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (spec.selected) {
            Spacer(modifier = Modifier.width(8.dp))
            SongLineIcon(
                kind = SongLineIconKind.Check,
                tint = MaterialTheme.colorScheme.primary,
                size = 14.dp
            )
        }
    }
}

@Composable
internal fun InlineActionIconButton(
    icon: SongLineIconKind,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        SongLineIcon(
            kind = icon,
            contentDescription = null,
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f)
            },
            size = SongDesignTokens.IconDefault.dp
        )
    }
}
