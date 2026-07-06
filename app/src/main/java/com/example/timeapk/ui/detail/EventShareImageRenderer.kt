package com.example.timeapk.ui.detail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import androidx.compose.ui.graphics.toArgb

const val SHARE_IMAGE_WIDTH_PX = 1080
const val SHARE_IMAGE_HEIGHT_PX = 1350

class EventShareImageRenderer {
    fun render(data: EventShareCardData): Bitmap {
        return createBitmap(SHARE_IMAGE_WIDTH_PX, SHARE_IMAGE_HEIGHT_PX).also { bitmap ->
            val canvas = Canvas(bitmap)
            drawShareCard(canvas, data)
        }
    }

    private fun drawShareCard(canvas: Canvas, data: EventShareCardData) {
        val accent = data.accentColor.toArgb()
        val ink = 0xFF1F1F1F.toInt()
        val mutedInk = 0xFF6A6256.toInt()
        val paper = 0xFFFFFBF5.toInt()
        val paperWash = withAlpha(accent, 18)
        val serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL)

        canvas.drawColor(0xFFF5F3ED.toInt())
        val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paper
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(54f, 54f, 1026f, 1296f), 20f, 20f, surfacePaint)
        surfacePaint.color = paperWash
        canvas.drawRoundRect(RectF(54f, 54f, 1026f, 1296f), 20f, 20f, surfacePaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(accent, 92)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(54f, 54f, 1026f, 1296f), 20f, 20f, borderPaint)

        val categoryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 54f
            typeface = serif
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.categoryLabel, 540f, 184f, categoryPaint)

        val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = 42f
            typeface = serif
        }
        drawCenteredText(canvas, data.dateText, datePaint, 760, 540f, 238f, maxLines = 1)

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(ink, 42)
            strokeWidth = 2f
        }
        canvas.drawLine(240f, 318f, 840f, 318f, dividerPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(ink, 235)
            textSize = if (data.title.length > 12) 70f else 82f
            typeface = serif
        }
        drawCenteredText(canvas, data.title, titlePaint, 800, 540f, 416f, maxLines = 3)

        canvas.drawLine(280f, 686f, 800f, 686f, dividerPaint)

        val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = if (data.timeText.length > 8) 92f else 130f
            typeface = serif
        }
        drawCenteredText(canvas, data.timeText, timePaint, 820, 540f, 764f, maxLines = 2)

        if (data.timeLabel.isNotBlank()) {
            val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedInk
                textSize = 48f
                typeface = serif
            }
            drawCenteredText(canvas, data.timeLabel, labelPaint, 820, 540f, 960f, maxLines = 2)
        }

        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(ink, 116)
            textSize = 38f
            typeface = serif
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.brandText, 540f, 1184f, brandPaint)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        width: Int,
        centerX: Float,
        topY: Float,
        maxLines: Int
    ) {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(maxLines)
            .setLineSpacing(0f, 1f)
            .build()
        canvas.withTranslation(centerX - width / 2f, topY) {
            layout.draw(this)
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
    }
}
