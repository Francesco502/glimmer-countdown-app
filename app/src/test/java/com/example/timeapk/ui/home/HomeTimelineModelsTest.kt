package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class HomeTimelineModelsTest {

    private val today = LocalDate.of(2026, 7, 1)

    @Test
    fun homeTimelineDigest_countsTodaySevenDayMonthAndMilestoneBuckets() {
        val events = listOf(
            eventState(1, "today", today, nextMilestoneDays = null),
            eventState(2, "tomorrow", today.plusDays(1)),
            eventState(3, "next week", today.plusDays(7)),
            eventState(4, "later this month", today.plusDays(20)),
            eventState(5, "milestone", today.plusDays(40), nextMilestoneDays = 5)
        )

        val digest = buildHomeTimelineDigest(events = events, today = today)

        assertEquals(1, digest.today.count)
        assertEquals(2, digest.sevenDays.count)
        assertEquals(4, digest.month.count)
        assertEquals(1, digest.milestone.count)
        assertEquals(1, digest.today.topItem?.event?.id)
        assertEquals(2, digest.sevenDays.topItem?.event?.id)
        assertEquals(1, digest.month.topItem?.event?.id)
        assertEquals(5, digest.milestone.topItem?.event?.id)
    }

    @Test
    fun homeTimelineDigest_prioritizesBirthdayAndPinnedEventsForTopItems() {
        val sameDate = today.plusDays(2)
        val events = listOf(
            eventState(1, "ordinary", sameDate, category = CATEGORY_OTHER),
            eventState(2, "anniversary", sameDate, category = CATEGORY_ANNIVERSARY),
            eventState(3, "birthday", sameDate, category = CATEGORY_BIRTHDAY),
            eventState(4, "pinned birthday", sameDate, category = CATEGORY_BIRTHDAY)
        )

        val digest = buildHomeTimelineDigest(
            events = events,
            today = today,
            pinnedEventIds = listOf(4)
        )

        assertEquals(4, digest.sevenDays.topItem?.event?.id)
    }

    @Test
    fun homeTimelineDigest_excludesPastEventsFromFutureBuckets() {
        val events = listOf(
            eventState(1, "past", today.minusDays(1), isPast = true),
            eventState(2, "future", today.plusDays(1))
        )

        val digest = buildHomeTimelineDigest(events = events, today = today)

        assertEquals(0, digest.today.count)
        assertEquals(1, digest.sevenDays.count)
        assertEquals(1, digest.month.count)
        assertEquals(2, digest.sevenDays.topItem?.event?.id)
    }

    @Test
    fun homeTimelineDigest_respectsSearchAndFilterBeforeCounting() {
        val events = listOf(
            eventState(1, "阿宁生日", today, category = CATEGORY_BIRTHDAY, note = "family"),
            eventState(2, "项目上线", today.plusDays(2), category = CATEGORY_OTHER, note = "work"),
            eventState(3, "周年", today.plusDays(3), category = CATEGORY_ANNIVERSARY, note = "阿宁")
        )

        val digest = buildHomeTimelineDigest(
            events = events,
            today = today,
            query = "阿宁",
            filterType = FilterType.Birthday
        )

        assertEquals(1, digest.today.count)
        assertEquals(0, digest.sevenDays.count)
        assertEquals(1, digest.month.count)
        assertEquals(1, digest.today.topItem?.event?.id)
    }

    @Test
    fun homeTimelineDigest_usesNextOccurrenceForYearlyAndLunarEvents() {
        val events = listOf(
            eventState(
                id = 1,
                title = "yearly birthday",
                date = LocalDate.of(2020, 1, 1),
                nextOccurrenceDate = today.plusDays(4),
                category = CATEGORY_BIRTHDAY,
                repeatType = REPEAT_YEARLY
            ),
            eventState(
                id = 2,
                title = "lunar birthday",
                date = LocalDate.of(2020, 2, 1),
                nextOccurrenceDate = today.plusDays(9),
                category = CATEGORY_BIRTHDAY,
                repeatType = REPEAT_YEARLY,
                isLunar = true
            )
        )

        val digest = buildHomeTimelineDigest(events = events, today = today)

        assertEquals(1, digest.sevenDays.count)
        assertEquals(2, digest.month.count)
        assertEquals(1, digest.sevenDays.topItem?.event?.id)
        assertEquals(1, digest.month.topItem?.event?.id)
    }

    @Test
    fun homeTimelineDigest_returnsEmptyBucketsWhenNoUpcomingEvents() {
        val digest = buildHomeTimelineDigest(
            events = listOf(eventState(1, "past", today.minusDays(3), isPast = true)),
            today = today,
            month = YearMonth.from(today)
        )

        assertEquals(0, digest.today.count)
        assertEquals(0, digest.sevenDays.count)
        assertEquals(0, digest.month.count)
        assertEquals(0, digest.milestone.count)
        assertNull(digest.today.topItem)
        assertNull(digest.sevenDays.topItem)
        assertNull(digest.month.topItem)
        assertNull(digest.milestone.topItem)
    }

    @Test
    fun filterEventsForTimelineBucket_usesTheSameBucketRulesAsDigest() {
        val events = listOf(
            eventState(1, "today", today),
            eventState(2, "seven", today.plusDays(7)),
            eventState(3, "month", today.plusDays(20)),
            eventState(4, "milestone", today.plusMonths(2), nextMilestoneDays = 4),
            eventState(5, "past", today.minusDays(1), isPast = true)
        )

        assertEquals(
            listOf(1),
            filterEventsForTimelineBucket(events, today, TimelineBucketType.Today).map { it.event.id }
        )
        assertEquals(
            listOf(2),
            filterEventsForTimelineBucket(events, today, TimelineBucketType.SevenDays).map { it.event.id }
        )
        assertEquals(
            listOf(1, 2, 3),
            filterEventsForTimelineBucket(events, today, TimelineBucketType.Month).map { it.event.id }
        )
        assertEquals(
            listOf(4),
            filterEventsForTimelineBucket(events, today, TimelineBucketType.Milestone).map { it.event.id }
        )
    }

    private fun eventState(
        id: Int,
        title: String,
        nextOccurrenceDate: LocalDate,
        category: String = CATEGORY_OTHER,
        repeatType: String = REPEAT_NONE,
        isLunar: Boolean = false,
        date: LocalDate = nextOccurrenceDate,
        note: String = "",
        isPast: Boolean = false,
        nextMilestoneDays: Long? = null
    ): EventUiState {
        val event = Event(
            id = id,
            title = title,
            date = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            category = category,
            note = note,
            repeatType = repeatType,
            isLunar = isLunar,
            createdAt = id.toLong()
        )
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextOccurrenceDate)
        return EventUiState(
            event = event,
            daysRemaining = kotlin.math.abs(days),
            daysElapsed = if (isPast) kotlin.math.abs(days) else 0,
            daysLeft = if (isPast) 0 else kotlin.math.abs(days),
            daysPassed = 0,
            isPast = isPast,
            nextMilestoneDays = nextMilestoneDays,
            nextMilestoneValue = nextMilestoneDays?.let { 100L },
            nextOccurrenceDate = nextOccurrenceDate
        )
    }
}
