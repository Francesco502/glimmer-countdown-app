package com.example.timeapk.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSortMigrationTest {

    @Test
    fun resolveHomeSortPreference_defaultsToCustomWhenNoStoredValue() {
        assertEquals(
            HOME_SORT_CUSTOM,
            resolveHomeSortPreference(
                storedSortType = null,
                hasMigratedToCustomSort = false
            )
        )
    }

    @Test
    fun resolveHomeSortPreference_resetsLegacyUserChoiceToCustomDuring36Migration() {
        assertEquals(
            HOME_SORT_CUSTOM,
            resolveHomeSortPreference(
                storedSortType = HOME_SORT_BY_DAYS,
                hasMigratedToCustomSort = false
            )
        )
        assertEquals(
            HOME_SORT_CUSTOM,
            resolveHomeSortPreference(
                storedSortType = HOME_SORT_BY_DATE,
                hasMigratedToCustomSort = false
            )
        )
    }

    @Test
    fun resolveHomeSortPreference_preservesStoredChoiceAfterMigrationCompleted() {
        assertEquals(
            HOME_SORT_BY_DATE,
            resolveHomeSortPreference(
                storedSortType = HOME_SORT_BY_DATE,
                hasMigratedToCustomSort = true
            )
        )
    }

    @Test
    fun resolveHomeSortSelectionUpdate_marksMigrationCompleteWhenUserChoosesSort() {
        val update = resolveHomeSortSelectionUpdate(HOME_SORT_BY_DATE)

        assertEquals(HOME_SORT_BY_DATE, update.sortType)
        assertEquals(true, update.hasMigratedToCustomSort)
    }
}
