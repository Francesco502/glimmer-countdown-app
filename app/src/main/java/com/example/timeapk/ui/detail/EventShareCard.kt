package com.example.timeapk.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongPaperSurface
import com.example.timeapk.ui.theme.SongSealLabel

data class EventShareCardData(
    val title: String,
    val categoryLabel: String,
    val dateText: String,
    val timeText: String,
    val timeLabel: String,
    val accentColor: Color,
    val brandText: String
)

fun buildEventShareCardData(
    title: String,
    categoryLabel: String,
    dateText: String,
    timeText: String,
    timeLabel: String,
    accentColor: Color,
    brandText: String
): EventShareCardData {
    return EventShareCardData(
        title = title,
        categoryLabel = categoryLabel,
        dateText = dateText,
        timeText = timeText,
        timeLabel = timeLabel,
        accentColor = accentColor,
        brandText = brandText
    )
}

@Composable
fun EventShareCard(
    data: EventShareCardData,
    modifier: Modifier = Modifier
) {
    SongPaperSurface(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = data.accentColor.copy(alpha = SongDesignTokens.BorderAlphaStrong)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(data.accentColor.copy(alpha = 0.045f))
                .padding(horizontal = 34.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SongSealLabel(text = data.categoryLabel, color = data.accentColor)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = data.dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft)
                )
                Text(
                    text = data.timeText,
                    style = MaterialTheme.typography.displayLarge.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (data.timeLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = data.timeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = data.brandText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
