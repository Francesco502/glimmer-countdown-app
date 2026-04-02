package com.example.timeapk.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build

internal data class WidgetThemeSnapshot(
    val isDark: Boolean,
    val usesSystemPalette: Boolean
) {
    val remoteCollectionKey: String
        get() = "${if (isDark) "dark" else "light"}-${if (usesSystemPalette) "system" else "fallback"}"
}

internal object WidgetThemeResolver {
    fun resolve(context: Context): WidgetThemeSnapshot {
        val systemNightMode = normalizeNightMode(context.resources.configuration.uiMode)
        return WidgetThemeSnapshot(
            isDark = resolveIsDark(systemNightMode),
            usesSystemPalette = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        )
    }

    internal fun resolveIsDark(systemNightMode: Int): Boolean {
        return normalizeNightMode(systemNightMode) == Configuration.UI_MODE_NIGHT_YES
    }

    internal fun normalizeNightMode(uiMode: Int): Int {
        return uiMode and Configuration.UI_MODE_NIGHT_MASK
    }
}
