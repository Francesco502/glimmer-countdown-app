package com.example.timeapk.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.FrameLayout
import com.example.timeapk.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CountdownWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CountdownRemoteViewsFactory(applicationContext, intent)
    }
}

internal class CountdownRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val widgetSizeBucket: Int = intent.getIntExtra(
        WidgetSizeBucket.EXTRA_SIZE_BUCKET,
        WidgetSizeBucket.MEDIUM
    )
    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    @Volatile
    private var snapshot: WidgetContentSnapshot? = null

    override fun onCreate() = Unit

    override fun onDestroy() {
        snapshot = null
    }

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            runBlocking(Dispatchers.IO) {
                // Android can synchronously call this while updateAppWidget holds the provider
                // refresh lock. Never acquire that lock here; publish the collection atomically.
                snapshot = WidgetContentResolver.load(
                    context = context,
                    appWidgetId = appWidgetId.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID },
                    sizeBucket = widgetSizeBucket
                )
            }
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun getCount(): Int = snapshot?.items?.size ?: 0

    override fun getViewAt(position: Int): RemoteViews? {
        val current = snapshot ?: return null
        val item = current.items.getOrNull(position) ?: return null
        return buildWidgetItemRemoteViews(context, item, current.textStyle, current.renderStyle, current.widthDp)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long =
        snapshot?.items?.getOrNull(position)?.eventId?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}

internal fun buildWidgetItemRemoteViews(
    context: Context,
    item: WidgetRenderedItem,
    textStyle: WidgetTextStyle,
    renderStyle: WidgetRenderStyle,
    widthDp: Int = 110
): RemoteViews {
    fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    val row = LayoutInflater.from(context).inflate(renderStyle.itemLayoutResId, FrameLayout(context), false)
    val valueView = row.findViewById<TextView>(R.id.widget_item_value)
    valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textStyle.valueSp)
    val available = (dp(widthDp) - 2 * context.resources.getDimensionPixelSize(R.dimen.widget_content_padding) -
        2 * dp(textStyle.paddingHorizontalDp)).coerceAtLeast(1)
    val candidates = listOf(item.fullValue, item.compactValue, item.shortValue).distinct()
    val fitted = WidgetValueFormatter.fitMeasuredValue(candidates, available.toFloat(), dp(4).toFloat()) {
        valueView.paint.measureText(it)
    }
    val layout = if (!fitted.stacked) renderStyle.itemLayoutResId else when (renderStyle.itemLayoutResId) {
        R.layout.widget_countdown_item_shadow_dark -> R.layout.widget_countdown_item_stacked_shadow_dark
        R.layout.widget_countdown_item_shadow_light -> R.layout.widget_countdown_item_stacked_shadow_light
        else -> R.layout.widget_countdown_item_stacked
    }
    return RemoteViews(context.packageName, layout).apply {
        setTextViewText(R.id.widget_item_title, item.title)
        setTextViewText(R.id.widget_item_value, fitted.text)
        setContentDescription(R.id.widget_item_root, item.contentDescription)
        setWidgetTextColors(
            context, renderStyle,
            primaryIds = intArrayOf(R.id.widget_item_title),
            accentIds = intArrayOf(R.id.widget_item_value)
        )
        setTextViewTextSize(R.id.widget_item_title, TypedValue.COMPLEX_UNIT_SP, textStyle.titleSp)
        setTextViewTextSize(R.id.widget_item_value, TypedValue.COMPLEX_UNIT_SP, textStyle.valueSp)
        setViewPadding(
            R.id.widget_item_root,
            dp(textStyle.paddingHorizontalDp),
            dp(textStyle.paddingVerticalDp),
            dp(textStyle.paddingHorizontalDp),
            dp(textStyle.paddingVerticalDp)
        )
        setOnClickFillInIntent(
            R.id.widget_item_root,
            Intent().apply { putExtra("open_event_id", item.eventId) }
        )
    }
}
