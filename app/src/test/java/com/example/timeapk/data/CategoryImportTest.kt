package com.example.timeapk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryImportTest {
    private fun event(category: String, title: String = "t") = Event(
        id = 0,
        title = title,
        date = 1_700_000_000_000L,
        category = category
    )

    @Test
    fun customCategoryIds_areRecognizedAndBuiltInsStayBuiltIn() {
        assertTrue(isBuiltInCategory(CATEGORY_BIRTHDAY))
        assertTrue(isCustomCategoryId("custom_abc"))
        assertFalse(isCustomCategoryId("custom_"))
        assertFalse(isCustomCategoryId("birthday"))
        assertTrue(newCustomCategoryId().startsWith(CUSTOM_CATEGORY_ID_PREFIX))
    }

    @Test
    fun resolveCategoryId_fallsBackToOtherForDeletedCategories() {
        assertEquals(CATEGORY_ANNIVERSARY, resolveCategoryId(CATEGORY_ANNIVERSARY, emptySet()))
        assertEquals("custom_a", resolveCategoryId("custom_a", setOf("custom_a")))
        assertEquals(CATEGORY_OTHER, resolveCategoryId("custom_gone", setOf("custom_a")))
    }

    @Test
    fun normalizeCategoryName_trimsCollapsesAndLimitsLength() {
        assertEquals("旅行 计划", normalizeCategoryName("  旅行   计划 "))
        assertEquals(CATEGORY_NAME_MAX_LENGTH, normalizeCategoryName("一".repeat(30)).length)
    }

    @Test
    fun jsonRoundTrip_carriesCustomCategoryNameAndColor() {
        val travel = Category(id = "custom_travel", name = "旅行", colorHex = "#457080")
        val json = listOf(event("custom_travel"), event(CATEGORY_BIRTHDAY)).toJsonString(listOf(travel))
        val parsed = parseEventsFromJson(json)

        assertEquals(0, parsed.errorCount)
        assertEquals(2, parsed.events.size)
        assertEquals(ImportedCategoryDraft("旅行", "#457080"), parsed.categoryDrafts["custom_travel"])
    }

    @Test
    fun unknownCategoryWithoutDeclaredName_isRejected() {
        val json = """[{"title":"x","date":1700000000000,"category":"custom_lost"}]"""
        val parsed = parseEventsFromJson(json)
        assertEquals(1, parsed.errorCount)
        assertTrue(parsed.events.isEmpty())
    }

    @Test
    fun resolveImportedCategories_mergesByNameAndCreatesMissingOnes() {
        val existing = listOf(Category(id = "custom_local", name = "Travel", sortOrder = 3))
        val drafts = mapOf(
            "custom_remote_travel" to ImportedCategoryDraft("travel", null),
            "custom_remote_work" to ImportedCategoryDraft("工作", "#AF4E31")
        )
        val events = listOf(event("custom_remote_travel"), event("custom_remote_work"), event(CATEGORY_OTHER))

        val result = resolveImportedCategories(
            events = events,
            drafts = drafts,
            existing = existing,
            newId = { "custom_new" },
            nowMillis = 1L
        )

        assertEquals("custom_local", result.events[0].category)
        assertEquals("custom_new", result.events[1].category)
        assertEquals(CATEGORY_OTHER, result.events[2].category)
        assertEquals(1, result.newCategories.size)
        val created = result.newCategories.single()
        assertEquals("工作", created.name)
        assertEquals(4, created.sortOrder)
    }

    @Test
    fun resolveImportedCategories_dropsUndeclaredCustomIdsToOther() {
        val result = resolveImportedCategories(
            events = listOf(event("custom_orphan")),
            drafts = emptyMap(),
            existing = emptyList()
        )
        assertEquals(CATEGORY_OTHER, result.events.single().category)
        assertTrue(result.newCategories.isEmpty())
    }

    @Test
    fun legacyIntegerFilter_mapsToCategoryIds() {
        assertNull(resolveFilterCategoryId(null, null))
        assertNull(resolveFilterCategoryId(null, 0))
        assertEquals(CATEGORY_BIRTHDAY, resolveFilterCategoryId(null, 1))
        assertEquals(CATEGORY_ANNIVERSARY, resolveFilterCategoryId(null, 2))
        assertEquals(CATEGORY_OTHER, resolveFilterCategoryId(null, 3))
        assertEquals("custom_a", resolveFilterCategoryId("custom_a", 1))
        assertNull(resolveFilterCategoryId("", 1))
    }
}
