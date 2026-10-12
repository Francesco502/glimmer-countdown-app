package com.example.timeapk.ui.settings

import android.content.Context
import androidx.annotation.RawRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timeapk.R
import com.example.timeapk.ui.components.SongDialogButton
import com.example.timeapk.ui.components.SongFormDialog
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.update.DownloadProgress
import com.example.timeapk.update.renderReleaseNotesMarkdown

internal const val GLIMMER_SOURCE_URL = "https://github.com/Francesco502/glimmer-countdown-app"
internal const val GLIMMER_ISSUES_URL = "$GLIMMER_SOURCE_URL/issues"

/**
 * Thin (2dp) Song-style progress line: an outline-tinted track with a primary ink stroke.
 * [fraction] null renders an indeterminate sliding segment.
 */
@Composable
internal fun SongThinProgressBar(
    fraction: Float?,
    modifier: Modifier = Modifier
) {
    val track = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong)
    val ink = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "updateProgress")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "updateProgressPhase"
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .semantics {
                progressBarRangeInfo = if (fraction != null) {
                    ProgressBarRangeInfo(fraction, 0f..1f)
                } else {
                    ProgressBarRangeInfo.Indeterminate
                }
            }
    ) {
        val y = size.height / 2f
        val stroke = size.height
        drawLine(track, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke, cap = StrokeCap.Butt)
        if (fraction != null) {
            val end = size.width * fraction.coerceIn(0f, 1f)
            if (end > 0f) {
                drawLine(ink, Offset(0f, y), Offset(end, y), strokeWidth = stroke, cap = StrokeCap.Butt)
            }
        } else {
            val segment = size.width * 0.3f
            val start = (size.width + segment) * phase - segment
            drawLine(
                ink,
                Offset(start.coerceAtLeast(0f), y),
                Offset((start + segment).coerceAtMost(size.width), y),
                strokeWidth = stroke,
                cap = StrokeCap.Butt
            )
        }
    }
}

@Composable
internal fun UpdateDownloadProgressSection(progress: DownloadProgress?) {
    val fraction = progress?.fraction
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SongThinProgressBar(fraction = fraction)
        Text(
            text = when {
                fraction != null -> stringResource(R.string.update_download_progress, (fraction * 100).toInt())
                else -> stringResource(
                    R.string.update_download_progress_unknown,
                    formatDownloadedBytes(progress?.bytesRead ?: 0L)
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

internal fun formatDownloadedBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(java.util.Locale.US, "%d KB", bytes / 1024L)
    else -> "$bytes B"
}

@Composable
internal fun ReleaseNotesText(notes: String) {
    val headingSize = MaterialTheme.typography.titleSmall.fontSize
    val rendered = remember(notes, headingSize) {
        renderReleaseNotesMarkdown(
            notes,
            headingStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = headingSize),
            boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        )
    }
    Text(
        rendered,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState())
    )
}

@Composable
internal fun FontLicensesDialog(
    context: Context,
    onDismissRequest: () -> Unit
) {
    val licenseText = remember {
        listOf(R.raw.ofl_noto_serif_sc, R.raw.ofl_zcool_xiaowei)
            .joinToString("\n\n————————\n\n") { readRawText(context, it) }
    }
    SongFormDialog(
        title = stringResource(R.string.about_licenses),
        onDismissRequest = onDismissRequest,
        content = {
            Text(
                stringResource(R.string.about_licenses_fonts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                licenseText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            )
        },
        buttons = {
            SongDialogButton(
                text = stringResource(R.string.about_licenses_close),
                onClick = onDismissRequest
            )
        }
    )
}

private fun readRawText(context: Context, @RawRes id: Int): String =
    runCatching {
        context.resources.openRawResource(id).bufferedReader(Charsets.UTF_8).use { it.readText() }.trim()
    }.getOrDefault("")
