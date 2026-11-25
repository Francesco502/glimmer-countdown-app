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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timeapk.BuildConfig
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.components.SongDialogButton
import com.example.timeapk.ui.components.SongFormDialog
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.update.DownloadProgress
import com.example.timeapk.update.AutomaticUpdateSettings
import com.example.timeapk.update.CheckUpdateResult
import com.example.timeapk.update.UpdateInstallOutcome
import com.example.timeapk.update.UpdateInstaller
import com.example.timeapk.update.renderReleaseNotesMarkdown

internal const val GLIMMER_SOURCE_URL = "https://github.com/Francesco502/glimmer-countdown-app"
internal const val GLIMMER_ISSUES_URL = "$GLIMMER_SOURCE_URL/issues"

@Composable
internal fun visibleAutomaticUpdate(app: TimeApplication): CheckUpdateResult? {
    if (!BuildConfig.DIRECT_APK_UPDATES_ENABLED) return null
    val result by app.availableUpdate.collectAsStateWithLifecycle()
    val settings by app.userPrefs.automaticUpdateSettings.collectAsStateWithLifecycle(AutomaticUpdateSettings())
    return result.takeIf { settings.shouldPrompt(it, System.currentTimeMillis()) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppUpdateFlowHost(app: TimeApplication, snackbarHostState: SnackbarHostState) {
    if (!BuildConfig.DIRECT_APK_UPDATES_ENABLED) return
    val context = LocalContext.current
    val result by app.requestedUpdate.collectAsStateWithLifecycle()
    val download by app.updateDownloadState.collectAsStateWithLifecycle()
    var needPermission by rememberSaveable { mutableStateOf(false) }
    var waitingForPermission by rememberSaveable { mutableStateOf(false) }
    var failureMessage by remember(result) { mutableStateOf<String?>(null) }

    fun startDownload() {
        if (download.downloading) return
        val release = result ?: return
        val url = release.downloadUrl ?: return
        failureMessage = null
        if (UpdateInstaller.canRequestPackageInstalls(context)) {
            app.startUpdateDownload(url, release.sha256)
        } else {
            needPermission = true
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (waitingForPermission) {
            waitingForPermission = false
            if (UpdateInstaller.canRequestPackageInstalls(context)) startDownload()
        }
    }

    LaunchedEffect(download.outcome) {
        val outcome = download.outcome ?: return@LaunchedEffect
        val message = when (outcome) {
            UpdateInstallOutcome.NEEDS_FOREGROUND -> R.string.update_needs_foreground
            UpdateInstallOutcome.CHECKSUM_MISMATCH -> R.string.update_checksum_failed
            UpdateInstallOutcome.DOWNLOAD_FAILED -> R.string.update_download_failed
            UpdateInstallOutcome.INSTALL_REQUESTED -> null
        }?.let(context::getString)
        app.clearUpdateOutcome()
        if (outcome == UpdateInstallOutcome.INSTALL_REQUESTED || outcome == UpdateInstallOutcome.NEEDS_FOREGROUND) {
            app.dismissUpdateDetails()
        }
        if (message != null) {
            if (result != null && outcome != UpdateInstallOutcome.NEEDS_FOREGROUND) failureMessage = message
            else snackbarHostState.showSnackbar(message, withDismissAction = true)
        }
    }

    result?.let { release ->
        if (needPermission) {
            SongFormDialog(
                title = stringResource(R.string.update_install_permission_title),
                onDismissRequest = { needPermission = false },
                content = { Text(stringResource(R.string.update_install_permission_message)) },
                buttons = {
                    SongDialogButton(stringResource(R.string.update_cancel), onClick = { needPermission = false })
                    SongDialogButton(
                        text = stringResource(R.string.update_install_permission_open),
                        onClick = {
                            needPermission = false
                            waitingForPermission = true
                            UpdateInstaller.openUnknownAppSourcesSettings(context)
                        }
                    )
                }
            )
        } else {
            SongFormDialog(
                title = stringResource(R.string.update_new_title, release.versionName.orEmpty()),
                onDismissRequest = { if (!download.downloading) app.dismissUpdateDetails() },
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        release.releaseNotes?.takeIf(String::isNotBlank)?.let { ReleaseNotesText(it) }
                        if (download.downloading) UpdateDownloadProgressSection(download.progress)
                        failureMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                enabled = !download.downloading,
                                onClick = {
                                    UpdateInstaller.openReleasePageInBrowser(
                                        context, release.releasePageUrl ?: "$GLIMMER_SOURCE_URL/releases"
                                    )
                                }
                            ) { Text(stringResource(R.string.update_open_browser)) }
                            release.versionName?.let { version ->
                                TextButton(enabled = !download.downloading, onClick = { app.skipUpdate(version) }) {
                                    Text(stringResource(R.string.update_skip_version))
                                }
                            }
                        }
                    }
                },
                buttons = {
                    SongDialogButton(
                        text = stringResource(R.string.update_later),
                        enabled = !download.downloading,
                        onClick = app::postponeUpdate
                    )
                    SongDialogButton(
                        text = stringResource(if (download.downloading) R.string.update_downloading else R.string.update_download_install),
                        enabled = !download.downloading && release.downloadUrl != null,
                        onClick = ::startDownload
                    )
                }
            )
        }
    }
}

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
