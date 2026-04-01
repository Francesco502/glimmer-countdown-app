package com.example.timeapk.widget

import android.content.Context
import android.content.res.Configuration
import com.example.timeapk.data.THEME_DARK
import com.example.timeapk.data.THEME_FOLLOW_SYSTEM
import com.example.timeapk.data.THEME_LIGHT
import com.example.timeapk.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal data class WidgetThemeSnapshot(
    val isDark: Boolean
)

internal object WidgetThemeResolver {
    fun resolve(context: Context): WidgetThemeSnapshot {
        val themeMode = resolveThemeModePreference(context)
        val systemNightMode = normalizeNightMode(context.resources.configuration.uiMode)
        val isDark = resolveIsDark(themeMode, systemNightMode)
        return WidgetThemeSnapshot(isDark = isDark)
    }

    internal fun resolveIsDark(themeMode: Int, systemNightMode: Int): Boolean {
        return when (themeMode) {
            THEME_LIGHT -> false
            THEME_DARK -> true
            else -> normalizeNightMode(systemNightMode) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    internal fun normalizeNightMode(uiMode: Int): Int {
        return uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    private fun resolveThemeModePreference(context: Context): Int {
        val prefs = UserPreferencesRepository(context.applicationContext)
        return runBlocking {
            runCatching { prefs.themeModeFlow.first() }
                .getOrDefault(THEME_FOLLOW_SYSTEM)
        }
    }
}
