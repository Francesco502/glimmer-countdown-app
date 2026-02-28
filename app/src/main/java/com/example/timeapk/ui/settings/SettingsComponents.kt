package com.example.timeapk.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.timeapk.R
import com.example.timeapk.ui.components.rememberPressScale

enum class SettingsCategory(
    val titleRes: Int,
    val icon: ImageVector
) {
    APPEARANCE(R.string.theme_title, Icons.Default.ColorLens),
    DISPLAY(R.string.settings_category_display_title, Icons.Default.Style),
    DATA(R.string.export_import, Icons.Default.Backup),
    ABOUT(R.string.settings_title, Icons.Default.Info)
}

@Composable
fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(category.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
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
        currentHex?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null } }
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
        text = title.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 12.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

