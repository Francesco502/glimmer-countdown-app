package com.example.timeapk.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timeapk.ui.reminder.ReminderStatusLevel
import com.example.timeapk.ui.reminder.ReminderStatusSummary
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.SongPaperSurface

@Composable
fun SongSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!summary.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongReminderStatusStrip(
    status: ReminderStatusSummary,
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val accent = when (status.level) {
        ReminderStatusLevel.Ready -> MaterialTheme.colorScheme.primary
        ReminderStatusLevel.Warning -> MaterialTheme.colorScheme.tertiary
        ReminderStatusLevel.Error -> MaterialTheme.colorScheme.error
        ReminderStatusLevel.Off -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = accent.copy(alpha = 0.08f),
                shape = RoundedCornerShape(3.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(accent, RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            status.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable(onClick = onActionClick)
                    .semantics {
                        role = Role.Button
                        contentDescription = actionLabel
                    }
            )
        }
    }
}

@Composable
fun SongEventPreviewCard(
    title: String,
    categoryLabel: String,
    dateText: String,
    color: Color,
    modifier: Modifier = Modifier,
    reminderText: String? = null
) {
    SongPaperSurface(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = color.copy(alpha = SongDesignTokens.BorderAlphaStrong)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!reminderText.isNullOrBlank()) {
                Text(
                    text = reminderText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class SongBottomAction(
    val label: String,
    val icon: SongLineIconKind,
    val contentDescription: String = label,
    val tint: Color? = null,
    val onClick: () -> Unit
)

@Composable
fun SongBottomActionBar(
    actions: List<SongBottomAction>,
    modifier: Modifier = Modifier,
    outlined: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        border = if (outlined) {
            BorderStroke(
                SongDesignTokens.BorderWidth.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft)
            )
        } else {
            null
        },
        shape = RoundedCornerShape(3.dp),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = action.onClick)
                        .semantics {
                            role = Role.Button
                            contentDescription = action.contentDescription
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val tint = action.tint ?: MaterialTheme.colorScheme.onSurfaceVariant
                    SongLineIcon(
                        kind = action.icon,
                        contentDescription = action.contentDescription,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SongMiniPreviewSurface(
    modifier: Modifier = Modifier,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(3.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            SongDesignTokens.BorderWidth.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft)
        ),
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}
