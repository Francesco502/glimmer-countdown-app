package com.example.timeapk.widget

import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import com.example.timeapk.R

internal fun RemoteViews.setWidgetTextColors(
    context: Context,
    style: WidgetRenderStyle,
    primaryIds: IntArray = intArrayOf(),
    secondaryIds: IntArray = intArrayOf(),
    accentIds: IntArray = intArrayOf()
) {
    // Reapply keeps old TextView properties. Always send colors, resolving night mode in the host.
    fun applyColors(ids: IntArray, colorResource: Int, light: Int, dark: Int) {
        ids.forEach { id ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && style.useThemeTextColors ->
                    setColor(id, "setTextColor", colorResource)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    setColorInt(id, "setTextColor", light, dark)
                else -> setTextColor(
                    id,
                    if (style.useThemeTextColors) context.getColor(colorResource)
                    else if (style.isDark) dark else light
                )
            }
        }
    }
    applyColors(primaryIds, R.color.widget_text_primary_color, style.lightTextColors.primary, style.darkTextColors.primary)
    applyColors(secondaryIds, R.color.widget_text_secondary_color, style.lightTextColors.secondary, style.darkTextColors.secondary)
    applyColors(accentIds, R.color.widget_text_accent_color, style.lightTextColors.accent, style.darkTextColors.accent)
}
