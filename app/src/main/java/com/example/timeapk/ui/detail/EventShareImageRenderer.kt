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
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withSave
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
        val palette = when (data.paper) {
            SharePaper.Xuan -> Palette(0xFFF5F3ED, 0xFFFFFBF5, 0xFF1F1F1F, 0xFF62584C, 0xFF913824)
            SharePaper.Ink -> Palette(0xFF1D2626, 0xFF283232, 0xFFF5EEE0, 0xFFD5CDBF, 0xFFF0AD92)
            SharePaper.Celadon -> Palette(0xFFCEDBD2, 0xFFE5EDE3, 0xFF1D322F, 0xFF415B52, 0xFF30564F)
        }
        val (surround, paper, ink, mutedInk, seal) = palette
        val selectedAccent = withAlpha(data.accentColor.toArgb(), 255)
        val accent = if (ColorUtils.calculateContrast(selectedAccent, paper) >= 4.5) selectedAccent else seal
        val paperWash = withAlpha(accent, 18)
        val typeface = data.typeface ?: Typeface.DEFAULT

        canvas.drawColor(surround)
        val bounds = RectF(54f, 54f, 1026f, 1296f)
        val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paper
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(bounds, 20f, 20f, surfacePaint)
        surfacePaint.color = paperWash
        canvas.drawRoundRect(bounds, 20f, 20f, surfacePaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(accent, 92)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        data.backgroundImage?.takeIf { !it.isRecycled }?.let { image ->
            drawCardImage(canvas, image, bounds, if (data.paper == SharePaper.Ink) 22 else 42)
        }
        drawPaperTexture(canvas, ink)
        canvas.drawRoundRect(bounds, 20f, 20f, borderPaint)

        val categoryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 43f
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        val category = TextUtils.ellipsize(data.categoryLabel, categoryPaint, 730f, TextUtils.TruncateAt.END).toString()
        val sealHalfWidth = categoryPaint.measureText(category) / 2f + 22f
        canvas.drawRoundRect(540f - sealHalfWidth, 103f, 540f + sealHalfWidth, 180f, 8f, 8f, borderPaint)
        canvas.drawText(category, 540f, 154f, categoryPaint)

        val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = 42f
            this.typeface = typeface
        }
        drawCenteredText(canvas, data.dateText, datePaint, 790, 540f, 207f, maxLines = 1)
        data.lunarText?.let { lunar ->
            datePaint.textSize = 36f
            drawCenteredText(canvas, lunar, datePaint, 790, 540f, 265f, maxLines = 1)
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(ink, 42)
            strokeWidth = 2f
        }
        canvas.drawLine(240f, 335f, 840f, 335f, dividerPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(ink, 235)
            textSize = if (data.title.length > 12) 62f else 82f
            this.typeface = typeface
        }
        drawCenteredText(canvas, data.title, titlePaint, 800, 540f, 399f, maxLines = 3)

        canvas.drawLine(280f, 686f, 800f, 686f, dividerPaint)

        val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = if (data.timeText.length > 8) 82f else 121f
            this.typeface = typeface
        }
        drawCenteredText(canvas, data.timeText, timePaint, 820, 540f, 726f, maxLines = 2)

        if (data.timeLabel.isNotBlank()) {
            val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedInk
                textSize = 44f
                this.typeface = typeface
            }
            drawCenteredText(canvas, data.timeLabel, labelPaint, 820, 540f, 948f, maxLines = 1)
        }

        data.note?.let { note ->
            datePaint.textSize = 36f
            drawCenteredText(canvas, note, datePaint, 760, 540f, 1032f, maxLines = 2)
        }
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = 38f
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.brandText, 540f, 1220f, brandPaint)
    }

    private data class Palette(val surround: Int, val paper: Int, val ink: Int, val mutedInk: Int, val seal: Int) {
        constructor(surround: Long, paper: Long, ink: Long, mutedInk: Long, seal: Long) :
            this(surround.toInt(), paper.toInt(), ink.toInt(), mutedInk.toInt(), seal.toInt())
    }

    private fun drawPaperTexture(canvas: Canvas, ink: Int) {
        val random = java.util.Random(434L)
        val fibers = FloatArray(16_000) { index ->
            if (index % 2 == 0) 76f + random.nextFloat() * 928f
            else 76f + random.nextFloat() * 1198f
        }
        canvas.drawPoints(fibers, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(ink, 14)
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        })
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

    /** 配图按居中裁切铺满卡片，低透明度保证文字对比度。 */
    private fun drawCardImage(canvas: Canvas, image: Bitmap, bounds: RectF, opacity: Int) {
        val scale = maxOf(bounds.width() / image.width, bounds.height() / image.height)
        val drawWidth = image.width * scale
        val drawHeight = image.height * scale
        val left = bounds.centerX() - drawWidth / 2f
        val top = bounds.centerY() - drawHeight / 2f
        val clip = android.graphics.Path().apply {
            addRoundRect(bounds, 20f, 20f, android.graphics.Path.Direction.CW)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = opacity }
        canvas.withSave {
            clipPath(clip)
            drawBitmap(image, null, RectF(left, top, left + drawWidth, top + drawHeight), paint)
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
    }
}
