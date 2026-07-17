package com.example.timeapk

import com.example.timeapk.data.LANG_EN
import com.example.timeapk.data.LANG_ZH
import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleMirrorMigrationDecisionTest {

    @Test
    fun noMirrorWithChineseStoredLocale_writesWithoutRecreating() {
        assertEquals(
            LocaleMirrorMigrationDecision(writeMirror = true, recreateActivity = false),
            resolveLocaleMirrorMigration(storedMode = LANG_ZH, mirroredMode = null)
        )
    }

    @Test
    fun noMirrorWithEnglishStoredLocale_writesAndRecreates() {
        assertEquals(
            LocaleMirrorMigrationDecision(writeMirror = true, recreateActivity = true),
            resolveLocaleMirrorMigration(storedMode = LANG_EN, mirroredMode = null)
        )
    }

    @Test
    fun mismatchedMirror_writesAndRecreates() {
        assertEquals(
            LocaleMirrorMigrationDecision(writeMirror = true, recreateActivity = true),
            resolveLocaleMirrorMigration(storedMode = LANG_EN, mirroredMode = LANG_ZH)
        )
    }

    @Test
    fun equalMirror_doesNeither() {
        assertEquals(
            LocaleMirrorMigrationDecision(writeMirror = false, recreateActivity = false),
            resolveLocaleMirrorMigration(storedMode = LANG_ZH, mirroredMode = LANG_ZH)
        )
    }
}
