package com.example.timeapk

import android.content.Context
import android.content.res.Configuration
import com.example.timeapk.data.LANG_EN
import com.example.timeapk.data.LANG_ZH
import java.util.Locale

object LocaleUtils {
    fun wrapContext(base: Context, languageMode: Int): Context {
        val locale = when (languageMode) {
            LANG_EN -> Locale.ENGLISH
            // 默认中文
            else -> Locale.SIMPLIFIED_CHINESE
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}

