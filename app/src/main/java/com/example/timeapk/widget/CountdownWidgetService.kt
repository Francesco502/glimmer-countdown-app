package com.example.timeapk.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.timeapk.R

class CountdownWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CountdownRemoteViewsFactory(applicationContext, intent)
    }
}

private class CountdownRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val widgetSizeBucket: Int = intent.getIntExtra(
        WidgetSizeBucket.EXTRA_SIZE_BUCKET,
        WidgetSizeBucket.MEDIUM
    )

    private val items = mutableListOf<WidgetRenderedItem>()
    private var textStyle: WidgetTextStyle = WidgetStylePolicy.resolve(widgetSizeBucket, 1f)

    override fun onCreate() = Unit

    override fun onDestroy() {
        items.clear()
    }

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            val snapshot = WidgetContentResolver.load(context, widgetSizeBucket)
            items.clear()
            items.addAll(snapshot.items)
            textStyle = snapshot.textStyle
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        val item = items.getOrNull(position) ?: return null

        return RemoteViews(context.packageName, R.layout.widget_countdown_item).apply {
            setTextViewText(R.id.widget_item_title, item.title)
            setTextViewText(R.id.widget_item_value, item.value)
            setTextViewTextSize(R.id.widget_item_title, TypedValue.COMPLEX_UNIT_SP, textStyle.titleSp)
            setTextViewTextSize(R.id.widget_item_value, TypedValue.COMPLEX_UNIT_SP, textStyle.valueSp)
            setViewPadding(
                R.id.widget_item_root,
                dp(textStyle.paddingHorizontalDp),
                dp(textStyle.paddingVerticalDp),
                dp(textStyle.paddingHorizontalDp),
                dp(textStyle.paddingVerticalDp)
            )
            setInt(R.id.widget_item_value, "setMaxEms", textStyle.valueMaxEms)
            setOnClickFillInIntent(
                R.id.widget_item_root,
                Intent().apply { putExtra("open_event_id", item.eventId) }
            )
        }
    }

    private fun dp(value: Int): Int {
        val density = context.resources.displayMetrics.density
        return (value * density).toInt()
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        items.getOrNull(position)?.eventId?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
