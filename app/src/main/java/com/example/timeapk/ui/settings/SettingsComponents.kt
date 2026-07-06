package com.example.timeapk.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.timeapk.R
import com.example.timeapk.ui.components.rememberPressScale
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.SongSettingMark
import com.example.timeapk.ui.theme.SongSettingMarkKind

enum class SettingsCategory(
    val titleRes: Int,
    val descriptionRes: Int,
    val mark: SongSettingMarkKind
) {
    APPEARANCE(
        R.string.theme_title,
        R.string.settings_entry_appearance_desc,
        SongSettingMarkKind.Appearance
    ),
    DISPLAY(
        R.string.settings_category_display_title,
        R.string.settings_entry_display_desc,
        SongSettingMarkKind.Display
    ),
    MILESTONE(
        R.string.settings_milestone_entry_title,
        R.string.settings_entry_milestone_desc,
        SongSettingMarkKind.Milestone
    ),
    DATA(
        R.string.export_import,
        R.string.settings_entry_data_desc,
        SongSettingMarkKind.Data
    ),
    ABOUT(
        R.string.settings_about_entry_title,
        R.string.settings_entry_about_desc,
        SongSettingMarkKind.About
    )
}

@Composable
fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (pressModifier, interactionSource) = rememberPressScale()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(pressModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongSettingMark(
            kind = category.mark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
            size = 18.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        SongLineIcon(
            kind = SongLineIconKind.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            size = 18.dp
        )
    }
}



@Composable
fun SettingsPressableRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val (pressModifier, interactionSource) = rememberPressScale()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(pressModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
fun SongToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val stateDescriptionText = stringResource(if (checked) R.string.toggle_on else R.string.toggle_off)
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clickable(
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
            .semantics {
                role = Role.Switch
                stateDescription = stateDescriptionText
            }
            .border(
                width = SongDesignTokens.BorderWidth.dp,
                color = if (checked) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = SongDesignTokens.BorderAlphaSoft)
                },
                shape = RoundedCornerShape(SongDesignTokens.RadiusXs.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stateDescriptionText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            }
        )
    }
}

@Composable
fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    labelStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    supportingContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (supportingContent != null) {
                supportingContent()
            } else {
                supportingText?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SettingsActionRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    destructive: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            supportingText?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SongLineIcon(
            kind = SongLineIconKind.ChevronRight,
            contentDescription = null,
            tint = if (destructive) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.78f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            },
            size = 18.dp
        )
    }
}

@Composable
fun CustomColorRow(
    label: String,
    currentHex: String?,
    defaultColor: Color,
    showBorder: Boolean = false,
    onPick: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentColor = remember(currentHex) {
        currentHex?.let { try { Color(it.toColorInt()) } catch (_: Exception) { null } }
            ?: defaultColor
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 100.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(currentColor, RoundedCornerShape(4.dp))
                .then(if (showBorder) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)) else Modifier)
        )
        Spacer(modifier = Modifier.width(12.dp))
        TextButton(onClick = onPick) {
            Text(stringResource(R.string.custom_color_pick))
        }
        TextButton(onClick = onReset) {
            Text(stringResource(R.string.custom_color_reset))
        }
    }
}

@Composable
fun SettingsGroupHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 12.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
}

@Composable
fun SettingsExpandableSection(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val expandedStateDescription = stringResource(
        if (expanded) R.string.settings_section_state_expanded else R.string.settings_section_state_collapsed
    )
    val toggleContentDescription = stringResource(
        if (expanded) R.string.settings_section_collapse else R.string.settings_section_expand
    )

    fun toggleExpanded() {
        expanded = !expanded
        onExpandedChange?.invoke(expanded)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClick = { toggleExpanded() }
                )
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    stateDescription = expandedStateDescription
                }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SongLineIcon(
                kind = if (expanded) SongLineIconKind.ChevronUp else SongLineIconKind.ChevronDown,
                contentDescription = toggleContentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 22.dp
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                content = content
            )
        }
    }
}
