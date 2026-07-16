package com.example.timeapk.widget

import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.ui.home.SortType
import com.example.timeapk.ui.home.applyHomeSort
import com.example.timeapk.ui.home.toEventUiState
import com.example.timeapk.ui.utils.DisplayModes
import org.junit.Assert.assertEquals
import org.json.JSONArray
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class WidgetContentResolverTest {

    @Test
    fun resolveDisplayMode_futureEventFallsBackToUntilMode() {
        val futureEvent = Event(
            title = "future",
            date = epochMillisOf(LocalDate.now().plusDays(10)),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )

        val resolved = WidgetContentResolver.resolveDisplayMode(
            state = futureEvent.toEventUiState(),
            preferredMode = DisplayModes.PAST_DAYS,
            showMilestone = true
        )

        assertEquals(DisplayModes.UNTIL_DAYS, resolved)
    }

    @Test
    fun resolveDisplayMode_dropsMilestoneWhenMilestoneIsHidden() {
        val futureEvent = Event(
            title = "future",
            date = epochMillisOf(LocalDate.now().plusDays(10)),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )

        val resolved = WidgetContentResolver.resolveDisplayMode(
            state = futureEvent.toEventUiState(),
            preferredMode = DisplayModes.MILESTONE,
            showMilestone = false
        )

        assertEquals(DisplayModes.UNTIL_DAYS, resolved)
    }

    @Test
    fun filterAndSortStates_appliesContentScopes() {
        val birthday = Event(
            id = 1,
            title = "birthday",
            date = epochMillisOf(LocalDate.now().plusDays(3)),
            category = CATEGORY_BIRTHDAY,
            repeatType = REPEAT_NONE
        )
        val normal = Event(
            id = 2,
            title = "normal",
            date = epochMillisOf(LocalDate.now().plusDays(8)),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )
        val past = Event(
            id = 3,
            title = "past",
            date = epochMillisOf(LocalDate.now().minusDays(2)),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )
        val states = listOf(birthday, normal, past).map { it.toEventUiState() }

        assertEquals(
            listOf(1),
            WidgetContentResolver.filterAndSortStates(
                states = states,
                config = WidgetConfig.default().copy(contentScope = CONTENT_BIRTHDAY),
                pinnedEventIds = emptyList(),
                customEventOrder = emptyList(),
                homeSortType = SortType.Custom
            ).map { it.event.id }
        )
        assertEquals(
            listOf(1, 2),
            WidgetContentResolver.filterAndSortStates(
                states = states,
                config = WidgetConfig.default().copy(contentScope = CONTENT_FUTURE),
                pinnedEventIds = emptyList(),
                customEventOrder = emptyList(),
                homeSortType = SortType.Custom
            ).map { it.event.id }
        )
        assertEquals(
            listOf(2),
            WidgetContentResolver.filterAndSortStates(
                states = states,
                config = WidgetConfig.default().copy(contentScope = CONTENT_PINNED),
                pinnedEventIds = listOf(2),
                customEventOrder = emptyList(),
                homeSortType = SortType.Custom
            ).map { it.event.id }
        )
    }

    @Test
    fun filterAndSortStates_homeSortUsesPinnedThenCustomOrder() {
        val first = Event(
            id = 1,
            title = "first",
            date = epochMillisOf(LocalDate.now().plusDays(8)),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )
        val pinned = Event(
            id = 2,
            title = "pinned",
            date = epochMillisOf(LocalDate.now().plusDays(4)),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )
        val past = Event(
            id = 3,
            title = "past",
            date = epochMillisOf(LocalDate.now().minusDays(2)),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )
        val states = listOf(first, pinned, past).map { it.toEventUiState() }

        val orderedIds = WidgetContentResolver.filterAndSortStates(
            states = states,
            config = WidgetConfig.default().copy(sortMode = SORT_HOME),
            pinnedEventIds = listOf(2),
            customEventOrder = listOf(3, 1),
            homeSortType = SortType.Custom
        ).map { it.event.id }

        assertEquals(listOf(2, 3, 1), orderedIds)
    }

    @Test
    fun filterAndSortStates_followHomeByDays_matchesHomeForExportedEvents_andKeepsPinsFirst() {
        val states = exportedEvents().map { it.toEventUiState() }
        val pinnedIds = listOf(4, 6)
        val customOrder = states.map { it.event.id }.reversed()
        val expected = applyHomeSort(
            list = states,
            sortType = SortType.ByDays,
            customEventOrderIds = customOrder,
            pinnedEventIds = pinnedIds
        )

        val actual = WidgetContentResolver.filterAndSortStates(
            states = states,
            config = WidgetConfig.default().copy(sortMode = SORT_HOME),
            pinnedEventIds = pinnedIds,
            customEventOrder = customOrder,
            homeSortType = SortType.ByDays
        )

        assertEquals(22, actual.size)
        assertEquals(expected.map { it.event.id }, actual.map { it.event.id })
        assertEquals(pinnedIds, actual.take(pinnedIds.size).map { it.event.id })
        val unpinnedDays = actual.drop(pinnedIds.size).map { it.daysRemaining }
        assertEquals(unpinnedDays.sorted(), unpinnedDays)
    }

    @Test
    fun filterAndSortStates_explicitWidgetModes_keepPinnedFirstAndNearestFirstSemantics() {
        val states = listOf(
            Event(
                id = 1,
                title = "far-pinned",
                date = epochMillisOf(LocalDate.now().plusDays(8)),
                category = CATEGORY_OTHER,
                repeatType = REPEAT_NONE
            ),
            Event(
                id = 2,
                title = "near",
                date = epochMillisOf(LocalDate.now().plusDays(2)),
                category = CATEGORY_OTHER,
                repeatType = REPEAT_NONE
            ),
            Event(
                id = 3,
                title = "past",
                date = epochMillisOf(LocalDate.now().minusDays(1)),
                category = CATEGORY_OTHER,
                repeatType = REPEAT_NONE
            )
        ).map { it.toEventUiState() }

        val pinnedFirst = WidgetContentResolver.filterAndSortStates(
            states = states,
            config = WidgetConfig.default().copy(sortMode = SORT_PINNED_FIRST),
            pinnedEventIds = listOf(1),
            customEventOrder = listOf(3, 1, 2),
            homeSortType = SortType.ByDate
        )
        val nearestFirst = WidgetContentResolver.filterAndSortStates(
            states = states,
            config = WidgetConfig.default().copy(sortMode = SORT_NEAREST_FIRST),
            pinnedEventIds = listOf(1),
            customEventOrder = listOf(3, 1, 2),
            homeSortType = SortType.ByDate
        )

        assertEquals(listOf(1, 2, 3), pinnedFirst.map { it.event.id })
        assertEquals(listOf(2, 1, 3), nearestFirst.map { it.event.id })
    }

    private fun exportedEvents(): List<Event> {
        val raw = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("widget_sort_3_17_export_anonymized.json")
        ) { "Missing exported widget sort fixture" }
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val array = JSONArray(raw)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Event(
                id = item.getInt("id"),
                title = item.getString("title"),
                date = item.getLong("date"),
                category = item.getString("category"),
                repeatType = item.getString("repeatType"),
                createdAt = item.getLong("createdAt"),
                isLunar = item.getBoolean("isLunar")
            )
        }
    }

    private fun epochMillisOf(localDate: LocalDate): Long {
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
