package com.example.timeapk.widget

import android.view.View
import android.widget.RemoteViews
import com.example.timeapk.R

internal fun RemoteViews.setWidgetBackground(style: WidgetRenderStyle) {
    val translucent = style.backgroundAlpha < 255
    // Keep the launcher background at the root; only the image layer fades, never the text.
    setInt(
        android.R.id.background, "setBackgroundResource",
        if (translucent) R.drawable.widget_background_transparent else style.backgroundResId
    )
    setImageViewResource(R.id.widget_background_image, style.backgroundResId)
    setInt(R.id.widget_background_image, "setImageAlpha", style.backgroundAlpha)
    setViewVisibility(R.id.widget_background_image, if (translucent) View.VISIBLE else View.GONE)
}
