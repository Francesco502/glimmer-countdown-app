package com.example.timeapk.widget

import android.content.Context
import android.content.res.Configuration
import com.example.timeapk.LocalePreferenceMirror
import com.example.timeapk.data.LANG_EN
import java.util.Locale

internal fun widgetLanguageContext(context: Context): Context {
    val locale = if (LocalePreferenceMirror.read(context) == LANG_EN) {
        Locale.ENGLISH
    } else {
        Locale.SIMPLIFIED_CHINESE
    }
    // Do not mutate the process default locale from a background widget refresh.
    return context.createConfigurationContext(Configuration(context.resources.configuration).apply {
        setLocale(locale)
    })
}
