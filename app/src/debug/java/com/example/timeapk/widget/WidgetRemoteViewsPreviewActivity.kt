package com.example.timeapk.widget

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import com.example.timeapk.R

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
            backgroundOpacityPercent = intent.getIntExtra(EXTRA_OPACITY, 75),
            borderMode = intent.getIntExtra(EXTRA_BORDER, BORDER_ON),
            densityMode = intent.getIntExtra(EXTRA_DENSITY, DENSITY_STANDARD),
            fontScale = intent.getFloatExtra(EXTRA_FONT_SCALE, 1.0f)
        ).sanitize()
        val sizeBucket = intent.getIntExtra(EXTRA_SIZE_BUCKET, WidgetSizeBucket.STANDARD_SQUARE)
        val themeSnapshot = WidgetThemeResolver.resolve(this)
        val renderStyle = WidgetRenderPolicy.resolve(config, themeSnapshot)
        val textStyle = WidgetStylePolicy.resolve(sizeBucket, config.fontScale, config.densityMode)
        val widgetView = CountdownAppWidgetProvider.buildWidgetRemoteViews(
            context = this,
            appWidgetId = PREVIEW_WIDGET_ID,
            sizeBucket = sizeBucket,
            config = config,
            themeSnapshot = themeSnapshot,
            attachRemoteAdapter = false
        ).apply(this, root)
        if (intent.getBooleanExtra(EXTRA_SAMPLE_ITEMS, false)) {
            widgetView.findViewById<ListView>(R.id.widget_list)?.apply {
                dividerHeight = textStyle.dividerHeightDp.dp
                adapter = PreviewCountdownAdapter(
                    inflater = LayoutInflater.from(context),
                    renderStyle = renderStyle,
                    textStyle = textStyle
                )
            }
        }

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
        const val EXTRA_OPACITY = "opacity"
        const val EXTRA_BORDER = "border"
        const val EXTRA_DENSITY = "density"
        const val EXTRA_FONT_SCALE = "fontScale"
        const val EXTRA_SIZE_BUCKET = "sizeBucket"
        const val EXTRA_SAMPLE_ITEMS = "sampleItems"
    }
}

private class PreviewCountdownAdapter(
    private val inflater: LayoutInflater,
    private val renderStyle: WidgetRenderStyle,
    private val textStyle: WidgetTextStyle
) : BaseAdapter() {
    private val items = listOf(
        PreviewItem("结婚", "已经9个月5天"),
        PreviewItem("在一起", "2124"),
        PreviewItem("塞恩生日", "还有272天"),
        PreviewItem("EYY 生日", "还有269天"),
        PreviewItem("深圳领证", "已经298天"),
        PreviewItem("老婆生日", "还有201天"),
        PreviewItem("特别的特", "还有298天")
    )

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): PreviewItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(renderStyle.itemLayoutResId, parent, false)
        val item = getItem(position)
        view.findViewById<TextView>(R.id.widget_item_title).apply {
            text = item.title
            setTextColor(renderStyle.primaryTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textStyle.titleSp)
        }
        view.findViewById<TextView>(R.id.widget_item_value).apply {
            text = item.value
            setTextColor(renderStyle.accentTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textStyle.valueSp)
            maxEms = textStyle.valueMaxEms
        }
        view.setPadding(
            parent.context.dp(textStyle.paddingHorizontalDp),
            parent.context.dp(textStyle.paddingVerticalDp),
            parent.context.dp(textStyle.paddingHorizontalDp),
            parent.context.dp(textStyle.paddingVerticalDp)
        )
        return view
    }

    private fun android.content.Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

private data class PreviewItem(
    val title: String,
    val value: String
)
