package com.example.timeapk.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
        shadowElevation = 0.dp
    ) {
        Box {
            SongPaperTextureOverlay(
                modifier = Modifier.matchParentSize(),
                paperTextureAlpha = 0.020f
            )
            content()
        }
    }
}

@Composable
fun SongPaperTextureOverlay(
    modifier: Modifier = Modifier,
    paperTextureAlpha: Float = 0.016f,
    lineColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = paperTextureAlpha)
) {
    Canvas(modifier = modifier) {
        val fiberAlpha = paperTextureAlpha.coerceIn(0f, 0.05f)
        val horizontalStep = 20.dp.toPx()
        var y = horizontalStep * 0.42f
        var row = 0
        while (y < size.height) {
            val drift = ((row % 5) - 2) * 1.3.dp.toPx()
            drawLine(
                color = lineColor.copy(alpha = fiberAlpha * 0.72f),
                start = Offset(0f, y + drift),
                end = Offset(size.width, y - drift * 0.45f),
                strokeWidth = 0.36.dp.toPx()
            )
            y += horizontalStep
            row += 1
        }
        val verticalStep = 34.dp.toPx()
        var x = verticalStep * 0.35f
        var column = 0
        while (x < size.width) {
            val drift = ((column % 4) - 1.5f) * 1.1.dp.toPx()
            drawLine(
                color = lineColor.copy(alpha = fiberAlpha * 0.46f),
                start = Offset(x + drift, 0f),
                end = Offset(x - drift * 0.35f, size.height),
                strokeWidth = 0.28.dp.toPx()
            )
            x += verticalStep
            column += 1
        }

        val shortFiberColor = lineColor.copy(alpha = fiberAlpha * 0.82f)
        val fiberCount = (size.width / 120.dp.toPx()).toInt().coerceAtLeast(4) +
            (size.height / 180.dp.toPx()).toInt().coerceAtLeast(3)
        repeat(fiberCount.coerceAtMost(28)) { index ->
            val px = ((index * 37) % 100) / 100f * size.width
            val py = ((index * 53 + 17) % 100) / 100f * size.height
            val length = (8 + (index % 5) * 3).dp.toPx()
            val slope = ((index % 7) - 3) * 0.9.dp.toPx()
            drawLine(
                color = shortFiberColor,
                start = Offset(px, py),
                end = Offset((px + length).coerceAtMost(size.width), (py + slope).coerceIn(0f, size.height)),
                strokeWidth = 0.34.dp.toPx()
            )
        }

        if (size.width > 96.dp.toPx() && size.height > 96.dp.toPx()) {
            val crackColor = lineColor.copy(alpha = fiberAlpha * 0.58f)
            val crackStroke = Stroke(width = 0.42.dp.toPx())
            repeat(3) { index ->
                val baseX = size.width * (0.22f + index * 0.24f)
                val baseY = size.height * (0.18f + (index % 2) * 0.33f)
                drawPath(
                    Path().apply {
                        moveTo(baseX, baseY)
                        lineTo(baseX + 12.dp.toPx(), baseY + 9.dp.toPx())
                        lineTo(baseX + 7.dp.toPx(), baseY + 24.dp.toPx())
                        lineTo(baseX + 21.dp.toPx(), baseY + 36.dp.toPx())
                    },
                    color = crackColor,
                    style = crackStroke
                )
            }
        }
    }
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
    modifier: Modifier = Modifier,
    height: Dp = 40.dp
) {
    val shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong)
    Row(
        modifier = modifier
            .height(height)
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
fun <T> SongModeTabRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    Box(modifier = modifier.heightIn(min = 48.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (value, label) ->
                val isSelected = selected == value
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelected(value) }
                        )
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            options.forEach { (value, _) ->
                val isSelected = selected == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.74f)
                            } else {
                                dividerColor
                            }
                        )
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
    modifier: Modifier = Modifier,
    selectionRole: Role? = Role.RadioButton
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
            .then(
                if (selectionRole != null) {
                    Modifier.selectable(
                        selected = selected,
                        role = selectionRole,
                        onClick = onClick
                    )
                } else {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = onClick
                    )
                }
            ),
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
fun SongColorSwatch(
    color: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showBorder: Boolean = true,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    size: Dp = 36.dp
) {
    val shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp)
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
        showBorder -> MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .size(size)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .background(color, shape)
            .border(
                BorderStroke(
                    width = if (selected) 1.dp else SongDesignTokens.BorderWidth.dp,
                    color = borderColor
                ),
                shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            SongLineIcon(
                kind = SongLineIconKind.Seal,
                contentDescription = null,
                size = 16.dp,
                tint = if (color.luminance() > 0.5f) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                }
            )
        }
    }
}

@Composable
fun SongHexColorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    placeholder: String? = null,
    previewColor: Color? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { placeholderText -> { Text(placeholderText) } },
        prefix = { Text("#") },
        isError = isError,
        singleLine = true,
        trailingIcon = previewColor?.let { color ->
            {
                SongColorSwatch(
                    color = color,
                    size = 24.dp,
                    showBorder = true
                )
            }
        },
        shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong),
            disabledIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft)
        ),
        modifier = modifier
    )
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
    eventIndicatorText: String?,
    selected: Boolean,
    today: Boolean,
    hasEvents: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = when {
        selected -> primary.copy(alpha = 0.58f)
        today -> SongPalette.Gold.copy(alpha = 0.46f)
        hasEvents -> primary.copy(alpha = 0.24f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    }
    val backgroundColor = when {
        selected -> SongPalette.PaperDeep.copy(alpha = 0.54f)
        hasEvents -> SongPalette.PaperWarm.copy(alpha = 0.45f)
        else -> Color.Transparent
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
            Spacer(modifier = Modifier.weight(1f))
            if (hasEvents) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(primary, RoundedCornerShape(1.dp))
                    )
                    if (!eventIndicatorText.isNullOrBlank()) {
                        Text(
                            text = eventIndicatorText,
                            style = MaterialTheme.typography.labelSmall,
                            color = primary,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
