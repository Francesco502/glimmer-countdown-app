package com.example.timeapk.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SongLineIconKind {
    Search,
    More,
    Add,
    Check,
    Back,
    Forward,
    ChevronLeft,
    ChevronRight,
    ChevronUp,
    ChevronDown,
    Pin,
    Edit,
    Share,
    Delete,
    Reminder,
    ReminderOff,
    Palette,
    Close,
    Seal,
    Scroll,
    Cloud,
    Info,
    Fan,
    Ruyi,
    Plum,
    Book
}

enum class SongSettingMarkKind {
    Appearance,
    Display,
    Milestone,
    Data,
    About
}

@Composable
fun SongLineIcon(
    kind: SongLineIconKind,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 20.dp,
    strokeWidth: Dp = 1.35.dp
) {
    val semanticModifier = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics { this.contentDescription = contentDescription }
    }
    Box(modifier = semanticModifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            drawSongLineIcon(kind = kind, tint = tint, strokeWidth = strokeWidth.toPx())
        }
    }
}

@Composable
fun SongSettingMark(
    kind: SongSettingMarkKind,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 20.dp
) {
    val iconKind = when (kind) {
        SongSettingMarkKind.Appearance -> SongLineIconKind.Fan
        SongSettingMarkKind.Display -> SongLineIconKind.Scroll
        SongSettingMarkKind.Milestone -> SongLineIconKind.Plum
        SongSettingMarkKind.Data -> SongLineIconKind.Book
        SongSettingMarkKind.About -> SongLineIconKind.Seal
    }
    SongLineIcon(
        kind = iconKind,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        size = size,
        strokeWidth = 1.25.dp
    )
}

private fun DrawScope.drawSongLineIcon(
    kind: SongLineIconKind,
    tint: Color,
    strokeWidth: Float
) {
    val unit = size.minDimension / 24f
    val stroke = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        drawLine(tint, p(x1, y1), p(x2, y2), strokeWidth, StrokeCap.Round)
    }
    fun circle(x: Float, y: Float, radius: Float, style: Stroke = stroke) {
        drawCircle(tint, radius * unit, p(x, y), style = style)
    }
    fun path(block: Path.() -> Unit) {
        drawPath(Path().apply(block), tint, style = stroke)
    }

    when (kind) {
        SongLineIconKind.Search -> {
            path {
                moveTo(7.2f * unit, 12.2f * unit)
                cubicTo(7.2f * unit, 7.8f * unit, 10.1f * unit, 5.6f * unit, 13f * unit, 6.4f * unit)
                cubicTo(16.2f * unit, 7.3f * unit, 17.6f * unit, 10.8f * unit, 15.6f * unit, 14f * unit)
                cubicTo(13.4f * unit, 17.2f * unit, 8.2f * unit, 16.3f * unit, 7.2f * unit, 12.2f * unit)
            }
            line(12.8f, 15.8f, 11.2f, 20f)
            line(9.2f, 12f, 16.2f, 12f)
            line(10.1f, 9.1f, 15.2f, 14.2f)
        }
        SongLineIconKind.More -> {
            path {
                moveTo(8.2f * unit, 5.8f * unit)
                lineTo(15.8f * unit, 5.8f * unit)
                cubicTo(17.1f * unit, 6.9f * unit, 17.6f * unit, 9f * unit, 17.1f * unit, 11.1f * unit)
                cubicTo(18.3f * unit, 12.6f * unit, 17.8f * unit, 15.5f * unit, 15.8f * unit, 18.2f * unit)
                lineTo(8.2f * unit, 18.2f * unit)
                cubicTo(6.2f * unit, 15.5f * unit, 5.7f * unit, 12.6f * unit, 6.9f * unit, 11.1f * unit)
                cubicTo(6.4f * unit, 9f * unit, 6.9f * unit, 6.9f * unit, 8.2f * unit, 5.8f * unit)
            }
            line(9.2f, 10f, 14.8f, 10f)
            line(9.2f, 13.2f, 14.8f, 13.2f)
            line(11f, 7.8f, 11f, 16.2f)
        }
        SongLineIconKind.Add -> {
            line(12f, 5.4f, 12f, 18.6f)
            line(5.4f, 12f, 18.6f, 12f)
        }
        SongLineIconKind.Check -> {
            line(5.5f, 12.2f, 9.6f, 16.2f)
            line(9.6f, 16.2f, 18.8f, 7.6f)
        }
        SongLineIconKind.Back -> {
            path {
                moveTo(16.8f * unit, 6.2f * unit)
                cubicTo(12.2f * unit, 7f * unit, 8.8f * unit, 9.1f * unit, 7.2f * unit, 12f * unit)
                cubicTo(8.8f * unit, 14.9f * unit, 12.2f * unit, 17f * unit, 16.8f * unit, 17.8f * unit)
            }
            line(8.1f, 12f, 13.4f, 8.3f)
            line(8.1f, 12f, 13.4f, 15.7f)
        }
        SongLineIconKind.Forward -> {
            path {
                moveTo(7.2f * unit, 6.2f * unit)
                cubicTo(11.8f * unit, 7f * unit, 15.2f * unit, 9.1f * unit, 16.8f * unit, 12f * unit)
                cubicTo(15.2f * unit, 14.9f * unit, 11.8f * unit, 17f * unit, 7.2f * unit, 17.8f * unit)
            }
            line(15.9f, 12f, 10.6f, 8.3f)
            line(15.9f, 12f, 10.6f, 15.7f)
        }
        SongLineIconKind.ChevronLeft -> {
            line(14.6f, 6f, 8.6f, 12f)
            line(8.6f, 12f, 14.6f, 18f)
        }
        SongLineIconKind.ChevronRight -> {
            line(9.4f, 6f, 15.4f, 12f)
            line(15.4f, 12f, 9.4f, 18f)
        }
        SongLineIconKind.ChevronUp -> {
            line(6f, 14.4f, 12f, 8.4f)
            line(12f, 8.4f, 18f, 14.4f)
        }
        SongLineIconKind.ChevronDown -> {
            line(6f, 9.6f, 12f, 15.6f)
            line(12f, 15.6f, 18f, 9.6f)
        }
        SongLineIconKind.Pin -> {
            path {
                moveTo(9f * unit, 5.5f * unit)
                lineTo(15.2f * unit, 5.5f * unit)
                lineTo(14f * unit, 11.2f * unit)
                lineTo(17.2f * unit, 14.4f * unit)
                lineTo(6.8f * unit, 14.4f * unit)
                lineTo(10f * unit, 11.2f * unit)
                close()
            }
            line(12f, 14.5f, 12f, 20f)
        }
        SongLineIconKind.Edit -> {
            path {
                moveTo(14.8f * unit, 4.8f * unit)
                cubicTo(16.3f * unit, 6.4f * unit, 16.4f * unit, 8.6f * unit, 15.2f * unit, 10.6f * unit)
                lineTo(10.2f * unit, 18.8f * unit)
                lineTo(7.2f * unit, 20f * unit)
                lineTo(8f * unit, 16.8f * unit)
                lineTo(13f * unit, 8.6f * unit)
                cubicTo(13.6f * unit, 7.4f * unit, 14f * unit, 6.1f * unit, 14.8f * unit, 4.8f * unit)
            }
            line(11.2f, 7.8f, 15.8f, 10.6f)
            line(6.2f, 20.4f, 12.8f, 19.5f)
        }
        SongLineIconKind.Share -> {
            circle(8f, 12f, 2.1f)
            circle(16.2f, 7.2f, 2.1f)
            circle(16.2f, 16.8f, 2.1f)
            line(9.8f, 10.9f, 14.4f, 8.3f)
            line(9.8f, 13.1f, 14.4f, 15.7f)
        }
        SongLineIconKind.Delete -> {
            path {
                moveTo(7.5f * unit, 6.2f * unit)
                lineTo(16.5f * unit, 6.2f * unit)
                lineTo(17.4f * unit, 17.2f * unit)
                lineTo(12f * unit, 19.4f * unit)
                lineTo(6.6f * unit, 17.2f * unit)
                close()
            }
            line(9.2f, 9.3f, 14.8f, 14.9f)
            line(14.8f, 9.3f, 9.2f, 14.9f)
        }
        SongLineIconKind.Reminder -> {
            path {
                moveTo(7.4f * unit, 14.2f * unit)
                cubicTo(7.8f * unit, 9.2f * unit, 9.7f * unit, 6.8f * unit, 12f * unit, 6.8f * unit)
                cubicTo(14.3f * unit, 6.8f * unit, 16.2f * unit, 9.2f * unit, 16.6f * unit, 14.2f * unit)
                lineTo(18f * unit, 16.4f * unit)
                lineTo(6f * unit, 16.4f * unit)
                close()
            }
            line(10.5f, 18.2f, 13.5f, 18.2f)
            line(12f, 5.2f, 12f, 6.6f)
        }
        SongLineIconKind.ReminderOff -> {
            drawSongLineIcon(SongLineIconKind.Reminder, tint, strokeWidth)
            line(5.8f, 5.8f, 18.2f, 18.2f)
        }
        SongLineIconKind.Palette -> {
            path {
                moveTo(12f * unit, 5f * unit)
                cubicTo(7.5f * unit, 5f * unit, 4.8f * unit, 8.2f * unit, 4.8f * unit, 12.2f * unit)
                cubicTo(4.8f * unit, 16.8f * unit, 8.2f * unit, 19f * unit, 12.8f * unit, 18.4f * unit)
                cubicTo(13.8f * unit, 18.2f * unit, 13.4f * unit, 16.2f * unit, 15.4f * unit, 16.1f * unit)
                cubicTo(17.8f * unit, 16f * unit, 19.2f * unit, 14.4f * unit, 19.2f * unit, 12f * unit)
                cubicTo(19.2f * unit, 8f * unit, 16.2f * unit, 5f * unit, 12f * unit, 5f * unit)
            }
            circle(9f, 10f, 0.6f, Stroke(width = strokeWidth))
            circle(12f, 8.8f, 0.6f, Stroke(width = strokeWidth))
            circle(15f, 10.2f, 0.6f, Stroke(width = strokeWidth))
        }
        SongLineIconKind.Close -> {
            path {
                moveTo(7.2f * unit, 7.4f * unit)
                cubicTo(10.2f * unit, 9.2f * unit, 13.8f * unit, 14.8f * unit, 16.8f * unit, 16.6f * unit)
            }
            path {
                moveTo(16.8f * unit, 7.4f * unit)
                cubicTo(13.8f * unit, 9.2f * unit, 10.2f * unit, 14.8f * unit, 7.2f * unit, 16.6f * unit)
            }
        }
        SongLineIconKind.Seal -> {
            path {
                moveTo(8f * unit, 5.5f * unit)
                lineTo(16f * unit, 5.5f * unit)
                lineTo(16.8f * unit, 16.2f * unit)
                lineTo(12f * unit, 19f * unit)
                lineTo(7.2f * unit, 16.2f * unit)
                close()
            }
            line(10f, 10f, 14f, 10f)
            line(10f, 13f, 14f, 13f)
        }
        SongLineIconKind.Scroll -> {
            line(7f, 6f, 17f, 6f)
            line(7f, 10f, 15f, 10f)
            line(7f, 14f, 17f, 14f)
            path {
                moveTo(7f * unit, 5f * unit)
                cubicTo(5.8f * unit, 6.5f * unit, 5.8f * unit, 17.5f * unit, 7f * unit, 19f * unit)
                lineTo(17f * unit, 19f * unit)
                cubicTo(18.2f * unit, 17.5f * unit, 18.2f * unit, 6.5f * unit, 17f * unit, 5f * unit)
            }
        }
        SongLineIconKind.Cloud -> {
            path {
                moveTo(6f * unit, 14.4f * unit)
                cubicTo(6.4f * unit, 11.6f * unit, 8.6f * unit, 10.4f * unit, 10.6f * unit, 11.2f * unit)
                cubicTo(11.7f * unit, 8.6f * unit, 15.1f * unit, 8f * unit, 16.9f * unit, 10.4f * unit)
                cubicTo(19.2f * unit, 10.8f * unit, 20.3f * unit, 12.6f * unit, 19.8f * unit, 14.7f * unit)
                cubicTo(19.4f * unit, 16.4f * unit, 17.8f * unit, 17.4f * unit, 15.8f * unit, 17.4f * unit)
                lineTo(8.4f * unit, 17.4f * unit)
                cubicTo(6.6f * unit, 17.4f * unit, 5.6f * unit, 16.2f * unit, 6f * unit, 14.4f * unit)
            }
        }
        SongLineIconKind.Info -> {
            circle(12f, 12f, 7f)
            line(12f, 10.8f, 12f, 16f)
            circle(12f, 8.1f, 0.5f, Stroke(width = strokeWidth))
        }
        SongLineIconKind.Fan -> {
            path {
                moveTo(6f * unit, 13.5f * unit)
                cubicTo(7f * unit, 7.4f * unit, 11.8f * unit, 4.8f * unit, 18f * unit, 7.4f * unit)
                cubicTo(17.2f * unit, 13f * unit, 12.8f * unit, 16.8f * unit, 6f * unit, 13.5f * unit)
            }
            line(11.6f, 15.8f, 10f, 20f)
            line(8.4f, 12.7f, 16.8f, 8.4f)
            line(10.6f, 14.1f, 14.8f, 6.9f)
        }
        SongLineIconKind.Ruyi -> {
            path {
                moveTo(6.2f * unit, 12.8f * unit)
                cubicTo(7.4f * unit, 9.4f * unit, 10.8f * unit, 10.1f * unit, 12f * unit, 12.1f * unit)
                cubicTo(13.2f * unit, 10.1f * unit, 16.6f * unit, 9.4f * unit, 17.8f * unit, 12.8f * unit)
                cubicTo(16.6f * unit, 15.8f * unit, 13.8f * unit, 15.8f * unit, 12f * unit, 13.8f * unit)
                cubicTo(10.2f * unit, 15.8f * unit, 7.4f * unit, 15.8f * unit, 6.2f * unit, 12.8f * unit)
            }
            line(12f, 13.8f, 12f, 19f)
            line(9.6f, 19f, 14.4f, 19f)
        }
        SongLineIconKind.Plum -> {
            circle(12f, 12f, 1.05f)
            circle(12f, 7.8f, 2.1f)
            circle(15.7f, 10.6f, 2.1f)
            circle(14.3f, 15f, 2.1f)
            circle(9.7f, 15f, 2.1f)
            circle(8.3f, 10.6f, 2.1f)
        }
        SongLineIconKind.Book -> {
            path {
                moveTo(6f * unit, 6.8f * unit)
                cubicTo(8.2f * unit, 5.8f * unit, 10.4f * unit, 5.8f * unit, 12f * unit, 7.2f * unit)
                cubicTo(13.6f * unit, 5.8f * unit, 15.8f * unit, 5.8f * unit, 18f * unit, 6.8f * unit)
                lineTo(18f * unit, 18f * unit)
                cubicTo(15.8f * unit, 17f * unit, 13.6f * unit, 17f * unit, 12f * unit, 18.4f * unit)
                cubicTo(10.4f * unit, 17f * unit, 8.2f * unit, 17f * unit, 6f * unit, 18f * unit)
                close()
            }
            line(12f, 7.2f, 12f, 18.4f)
        }
    }
}
