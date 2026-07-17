package com.example.timeapk

import android.content.Context
import androidx.core.content.edit
import com.example.timeapk.data.LANG_ZH

internal data class LocaleMirrorMigrationDecision(
    val writeMirror: Boolean,
    val recreateActivity: Boolean
)

internal fun resolveLocaleMirrorMigration(
    storedMode: Int,
    mirroredMode: Int?
): LocaleMirrorMigrationDecision {
    val writeMirror = mirroredMode == null || mirroredMode != storedMode
    return LocaleMirrorMigrationDecision(
        writeMirror = writeMirror,
        recreateActivity = writeMirror && (mirroredMode != null || storedMode != LANG_ZH)
    )
}

object LocalePreferenceMirror {
    private const val FILE = "locale_mirror"
    private const val KEY = "language_mode"

    fun read(context: Context): Int? = context
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)
        .takeIf { it.contains(KEY) }
        ?.getInt(KEY, LANG_ZH)

    fun write(context: Context, mode: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit(commit = false) {
                putInt(KEY, mode)
            }
    }
}
