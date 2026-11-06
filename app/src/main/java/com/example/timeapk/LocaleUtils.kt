package com.example.timeapk

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.example.timeapk.data.LANG_EN
import com.example.timeapk.data.LANG_ZH
import java.util.Locale

object LocaleUtils {
    /**
     * Resolves the locale for a context. Android 13+ per-app language (set from system settings
     * or through [applyPlatformLocale]) wins; otherwise the in-app preference decides.
     */
    fun wrapContext(base: Context, languageMode: Int): Context {
        val locale = resolveLocale(base, languageMode)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    fun resolveLocale(base: Context, languageMode: Int): Locale {
        platformLocaleMode(base)?.let { return localeFor(it) }
        return localeFor(languageMode)
    }

    private fun localeFor(languageMode: Int): Locale =
        if (languageMode == LANG_EN) Locale.ENGLISH else Locale.SIMPLIFIED_CHINESE

    /** Pushes the in-app choice into the platform per-app locale. Returns true when it took effect. */
    fun applyPlatformLocale(context: Context, languageMode: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val manager = context.getSystemService(LocaleManager::class.java) ?: return false
        val target = LocaleList.forLanguageTags(if (languageMode == LANG_EN) "en" else "zh-CN")
        if (manager.applicationLocales == target) return false
        manager.setApplicationLocales(target)
        return true
    }

    /** Reads the platform per-app locale as an app language mode, or null when unset. */
    fun platformLocaleMode(context: Context): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val manager = context.getSystemService(LocaleManager::class.java) ?: return null
        val locales = manager.applicationLocales
        if (locales.isEmpty) return null
        return if (locales[0]?.language == Locale.ENGLISH.language) LANG_EN else LANG_ZH
    }

    /** Keeps the synchronous SharedPreferences mirror aligned with a system-side language change. */
    fun syncMirrorFromSystem(context: Context) {
        val mode = platformLocaleMode(context) ?: return
        if (LocalePreferenceMirror.read(context) != mode) {
            LocalePreferenceMirror.write(context, mode)
        }
    }
}
