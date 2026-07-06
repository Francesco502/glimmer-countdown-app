package com.example.timeapk.ui.event

import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventTemplateModelsTest {

    @Test
    fun eventEntryTemplates_useExpectedDefaults() {
        val birthday = eventEntryTemplates.single { it.type == EventEntryTemplateType.Birthday }
        val anniversary = eventEntryTemplates.single { it.type == EventEntryTemplateType.Anniversary }
        val countdown = eventEntryTemplates.single { it.type == EventEntryTemplateType.Countdown }

        assertEquals(CATEGORY_BIRTHDAY, birthday.category)
        assertEquals(REPEAT_YEARLY, birthday.repeatType)
        assertTrue(birthday.allowLunar)

        assertEquals(CATEGORY_ANNIVERSARY, anniversary.category)
        assertEquals(REPEAT_YEARLY, anniversary.repeatType)
        assertTrue(anniversary.allowLunar)

        assertEquals(CATEGORY_OTHER, countdown.category)
        assertEquals(REPEAT_NONE, countdown.repeatType)
        assertFalse(countdown.allowLunar)
    }

    @Test
    fun songNamedColors_includeNineNamedPresetColors() {
        assertEquals(9, songNamedColors.size)
        assertEquals(
            listOf(
                "ink",
                "dailan",
                "pine_green",
                "celadon",
                "cinnabar",
                "ochre",
                "old_gold",
                "tea_brown",
                "lotus_mauve"
            ),
            songNamedColors.map { it.nameKey }
        )
        songNamedColors.forEach { color ->
            assertTrue(color.hex.matches(Regex("^#[0-9A-Fa-f]{6}$")))
        }
    }

    @Test
    fun defaultTemplateForCategory_mapsExistingCategories() {
        assertEquals(EventEntryTemplateType.Birthday, defaultTemplateForCategory(CATEGORY_BIRTHDAY).type)
        assertEquals(EventEntryTemplateType.Anniversary, defaultTemplateForCategory(CATEGORY_ANNIVERSARY).type)
        assertEquals(EventEntryTemplateType.Countdown, defaultTemplateForCategory(CATEGORY_OTHER).type)
        assertEquals(EventEntryTemplateType.Countdown, defaultTemplateForCategory("unknown").type)
    }

    @Test
    fun applyTemplateForCategory_updatesCategoryRepeatAndLunarRules() {
        val base = EventDetails(
            title = "keep title",
            note = "keep note",
            category = CATEGORY_OTHER,
            repeatType = REPEAT_DAILY,
            isLunar = true
        )

        val birthday = applyTemplateForCategory(base, CATEGORY_BIRTHDAY)
        assertEquals(CATEGORY_BIRTHDAY, birthday.category)
        assertEquals(REPEAT_YEARLY, birthday.repeatType)
        assertTrue(birthday.isLunar)
        assertEquals(base.title, birthday.title)
        assertEquals(base.note, birthday.note)

        val countdown = applyTemplateForCategory(
            base.copy(category = CATEGORY_BIRTHDAY, repeatType = REPEAT_MONTHLY, isLunar = true),
            CATEGORY_OTHER
        )
        assertEquals(CATEGORY_OTHER, countdown.category)
        assertEquals(REPEAT_NONE, countdown.repeatType)
        assertFalse(countdown.isLunar)
    }
}
