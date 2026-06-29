package com.example.timeapk.widget

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout

class WidgetRemoteViewsPreviewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(246, 243, 236))
            clipToPadding = false
        }
        setContentView(root)

        val config = WidgetConfig(
            appearancePreset = intent.getIntExtra(EXTRA_APPEARANCE, APPEARANCE_CELADON),
            borderMode = intent.getIntExtra(EXTRA_BORDER, BORDER_ON),
            densityMode = intent.getIntExtra(EXTRA_DENSITY, DENSITY_STANDARD),
            fontScale = intent.getFloatExtra(EXTRA_FONT_SCALE, 1.0f)
        ).sanitize()
        val sizeBucket = intent.getIntExtra(EXTRA_SIZE_BUCKET, WidgetSizeBucket.STANDARD_SQUARE)
        val widgetView = CountdownAppWidgetProvider.buildWidgetRemoteViews(
            context = this,
            appWidgetId = PREVIEW_WIDGET_ID,
            sizeBucket = sizeBucket,
            config = config,
            themeSnapshot = WidgetThemeResolver.resolve(this),
            attachRemoteAdapter = false
        ).apply(this, root)

        root.addView(
            widgetView,
            FrameLayout.LayoutParams(resolvePreviewWidthDp(sizeBucket).dp, resolvePreviewHeightDp(sizeBucket).dp).apply {
                gravity = Gravity.CENTER
            }
        )
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun resolvePreviewWidthDp(sizeBucket: Int): Int {
        return when (sizeBucket) {
            WidgetSizeBucket.WIDE_SHORT -> 330
            WidgetSizeBucket.TALL -> 150
            WidgetSizeBucket.COMPACT_SQUARE -> 150
            else -> 220
        }
    }

    private fun resolvePreviewHeightDp(sizeBucket: Int): Int {
        return when (sizeBucket) {
            WidgetSizeBucket.WIDE_SHORT -> 150
            WidgetSizeBucket.TALL -> 330
            WidgetSizeBucket.COMPACT_SQUARE -> 150
            else -> 220
        }
    }

    companion object {
        private const val PREVIEW_WIDGET_ID = 13013

        const val EXTRA_APPEARANCE = "appearance"
        const val EXTRA_BORDER = "border"
        const val EXTRA_DENSITY = "density"
        const val EXTRA_FONT_SCALE = "fontScale"
        const val EXTRA_SIZE_BUCKET = "sizeBucket"
    }
}
