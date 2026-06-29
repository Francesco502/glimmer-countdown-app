package com.example.timeapk.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SongPaperSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
        color = backgroundColor,
        border = BorderStroke(SongDesignTokens.BorderWidth.dp, borderColor),
        shadowElevation = 0.dp,
        content = content
    )
}

@Composable
fun SongSealLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .border(BorderStroke(1.dp, color.copy(alpha = 0.72f)), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun <T> SongSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong)
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f), shape)
            .border(BorderStroke(SongDesignTokens.BorderWidth.dp, borderColor), shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, (value, label) ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(SongDesignTokens.BorderWidth.dp)
                        .background(borderColor)
                )
            }
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onSelected(value) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SongFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.56f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong)
    }
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 32.dp)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        },
        border = BorderStroke(SongDesignTokens.BorderWidth.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft)
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = SongDesignTokens.BorderWidth.dp,
        color = color
    )
}

@Composable
fun SongCalendarCell(
    dayText: String,
    lunarText: String?,
    previewText: String?,
    selected: Boolean,
    today: Boolean,
    hasEvents: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = when {
        selected -> primary.copy(alpha = 0.82f)
        today -> SongPalette.Gold.copy(alpha = 0.75f)
        hasEvents -> primary.copy(alpha = 0.46f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    }
    val backgroundColor = when {
        selected -> SongPalette.PaperDeep.copy(alpha = 0.72f)
        hasEvents -> SongPalette.PaperWarm.copy(alpha = 0.72f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    }
    val cellModifier = modifier
        .clip(RoundedCornerShape(SongDesignTokens.StandardRadius.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(
        modifier = cellModifier
            .background(backgroundColor, RoundedCornerShape(SongDesignTokens.StandardRadius.dp))
            .border(BorderStroke(SongDesignTokens.BorderWidth.dp, borderColor), RoundedCornerShape(SongDesignTokens.StandardRadius.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = dayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasEvents || selected) primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                if (today) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(SongPalette.Seal, RoundedCornerShape(1.dp))
                    )
                }
            }
            if (!lunarText.isNullOrBlank()) {
                Text(
                    text = lunarText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (hasEvents) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(primary, RoundedCornerShape(1.dp))
                    )
                    Text(
                        text = previewText.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
