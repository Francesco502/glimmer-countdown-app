package com.example.timeapk.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.example.timeapk.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val rendered by produceState<Result<android.graphics.Bitmap>?>(initialValue = null, key1 = data) {
        value = withContext(Dispatchers.Default) {
            runCatching { EventShareImageRenderer().render(data) }
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bitmap = rendered?.getOrNull()
        if (bitmap != null) {
            Image(
                bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                contentDescription = listOf(data.categoryLabel, data.title, data.dateText, data.timeText, data.timeLabel)
                    .filter(String::isNotBlank).joinToString(", "),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else if (rendered == null) {
            CircularProgressIndicator()
        } else {
            Text(stringResource(R.string.share_image_failed))
        }
    }
}
