package com.example.timeapk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timeapk.data.LANG_EN
import com.example.timeapk.data.LANG_ZH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalePreferenceMirrorInstrumentationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun mirrorClearReadAndWriteAreImmediatelyVisibleInMemory() {
        val preferences = context.getSharedPreferences("locale_mirror", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()

        assertNull(LocalePreferenceMirror.read(context))

        LocalePreferenceMirror.write(context, LANG_EN)
        assertEquals(LANG_EN, LocalePreferenceMirror.read(context))

        preferences.edit().clear().commit()
        assertNull(LocalePreferenceMirror.read(context))

        LocalePreferenceMirror.write(context, LANG_ZH)
        assertEquals(LANG_ZH, LocalePreferenceMirror.read(context))
    }
}
