package com.example.timeapk

import android.content.Context
import com.example.timeapk.data.LANG_ZH

object LocalePreferenceMirror {
    private const val FILE = "locale_mirror"
    private const val KEY = "language_mode"

    fun read(context: Context): Int? = context
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)
        .takeIf { it.contains(KEY) }
        ?.getInt(KEY, LANG_ZH)

    fun write(context: Context, mode: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY, mode)
            .apply()
    }
}
