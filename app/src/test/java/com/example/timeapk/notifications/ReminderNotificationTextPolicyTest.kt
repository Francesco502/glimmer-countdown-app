package com.example.timeapk.notifications

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import java.io.File
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderNotificationTextPolicyTest {

    private val zh = ReminderNotificationTemplates(
        today = "今天",
        joinFormat = "%1\$s · %2\$s",
        datePattern = "M月d日 EEE",
        locale = Locale.SIMPLIFIED_CHINESE,
        daysLeft = { "还有 $it 天" },
        milestoneElapsed = { "已经 $it 天" },
        milestoneMark = { "满 $it 天" }
    )

    private val en = ReminderNotificationTemplates(
        today = "Today",
        joinFormat = "%1\$s · %2\$s",
        datePattern = "EEE, MMM d",
        locale = Locale.ENGLISH,
        daysLeft = { if (it == 1) "1 day left" else "$it days left" },
        milestoneElapsed = { "$it days so far" },
        milestoneMark = { "$it-day mark" }
    )

    // 2026-10-15 is a Thursday.
    private val oct15 = LocalDate.of(2026, 10, 15)

    @Test
    fun zhCountdownUsesDateAndWeekdayWithoutAsciiComma() {
        val text = ReminderNotificationTextPolicy.countdownText(zh, daysLeft = 3, occurrenceDate = oct15)

        assertEquals("还有 3 天 · 10月15日 周四", text)
        assertFalse(text.contains(","))
    }

    @Test
    fun enCountdownUsesEnglishDate() {
        assertEquals(
            "3 days left · Thu, Oct 15",
            ReminderNotificationTextPolicy.countdownText(en, daysLeft = 3, occurrenceDate = oct15)
        )
    }

    @Test
    fun dayOfEventReadsToday() {
        assertEquals("今天", ReminderNotificationTextPolicy.countdownText(zh, 0, oct15))
        assertEquals("Today", ReminderNotificationTextPolicy.countdownText(en, 0, oct15))
    }

    @Test
    fun unknownDateFallsBackToRelativePhrase() {
        assertEquals("还有 2 天", ReminderNotificationTextPolicy.countdownText(zh, 2, null))
    }

    @Test
    fun milestoneDayCountReadsElapsedOnTheDay() {
        assertEquals(
            "已经 1000 天",
            ReminderNotificationTextPolicy.milestoneText(zh, 0, milestoneDays = 1000, milestoneYears = null, label = "千日")
        )
        assertEquals(
            "还有 3 天 · 满 1000 天",
            ReminderNotificationTextPolicy.milestoneText(zh, 3, milestoneDays = 1000, milestoneYears = null, label = "千日")
        )
        assertEquals(
            "3 days left · 1000-day mark",
            ReminderNotificationTextPolicy.milestoneText(en, 3, milestoneDays = 1000, milestoneYears = null, label = "x")
        )
    }

    @Test
    fun anniversaryAndLegacyMilestonesUseLabel() {
        assertEquals(
            "今天 · 3周年",
            ReminderNotificationTextPolicy.milestoneText(zh, 0, milestoneDays = 1096, milestoneYears = 3, label = "3周年")
        )
        assertEquals(
            "还有 7 天 · 千日",
            ReminderNotificationTextPolicy.milestoneText(zh, 7, milestoneDays = null, milestoneYears = null, label = "千日")
        )
    }

    @Test
    fun noteFirstLineExpandsBigTextOnlyWhenPresent() {
        val withNote = ReminderNotificationTextPolicy.content("妈妈生日", "今天", "\n  记得订蛋糕  \n第二行")
        assertEquals("今天\n记得订蛋糕", withNote.bigText)

        val withoutNote = ReminderNotificationTextPolicy.content("妈妈生日", "今天", "   ")
        assertNull(withoutNote.bigText)
    }

    @Test
    fun renamedEventUsesLatestTitle() {
        val latest = event(title = "结婚纪念日", remindEnabled = true)

        val decision = ReminderNotificationTextPolicy.decide(
            latest = latest,
            lookupFailed = false,
            fallbackTitle = "旧标题",
            reminderStillEnabled = { it.remindEnabled }
        )

        assertTrue(decision is ReminderDeliveryDecision.Deliver)
        assertEquals("结婚纪念日", (decision as ReminderDeliveryDecision.Deliver).title)
    }

    @Test
    fun deletedOrDisabledEventIsNotNotified() {
        assertEquals(
            ReminderDeliveryDecision.Skip,
            ReminderNotificationTextPolicy.decide(null, lookupFailed = false, fallbackTitle = "t") { true }
        )
        assertEquals(
            ReminderDeliveryDecision.Skip,
            ReminderNotificationTextPolicy.decide(
                event(title = "考研", remindEnabled = false),
                lookupFailed = false,
                fallbackTitle = "考研"
            ) { it.remindEnabled }
        )
    }

    @Test
    fun lookupFailureFallsBackToScheduledTitle() {
        val decision = ReminderNotificationTextPolicy.decide(null, lookupFailed = true, fallbackTitle = "考研") { true }

        assertEquals("考研", (decision as ReminderDeliveryDecision.Deliver).title)
    }

    @Test
    fun groupSummaryOnlyWhenMoreThanOneReminderIsShowing() {
        assertFalse(ReminderNotificationTextPolicy.shouldPostGroupSummary(1))
        assertTrue(ReminderNotificationTextPolicy.shouldPostGroupSummary(2))
    }

    @Test
    fun reminderStringResourcesUseFullWidthChineseAndNoHanInEnglish() {
        val zhRes = resource("values/strings.xml")
        val enRes = resource("values-en/strings.xml")
        val zhBlock = zhRes.substringAfter("<!-- 4.2 reminders -->")
        val enBlock = enRes.substringAfter("<!-- 4.2 reminders -->")

        assertTrue(zhBlock.contains("<string name=\"notification_action_view\">查看</string>"))
        assertTrue(zhBlock.contains("<string name=\"notification_date_pattern\">M月d日 EEE</string>"))
        assertFalse(
            "Chinese reminder strings must not use ASCII commas",
            Regex(">[^<]*,[^<]*<").containsMatchIn(zhBlock)
        )
        assertFalse(Regex("\\p{IsHan}").containsMatchIn(enBlock))
    }

    @Test
    fun workersDelegateToSharedDeliveryAndNoLongerJoinWithAsciiComma() {
        val reminder = source("notifications/ReminderWorker.kt")
        val milestone = source("notifications/MilestoneReminderWorker.kt")
        val delivery = source("notifications/ReminderNotificationDelivery.kt")

        assertTrue(reminder.contains("ReminderNotificationDelivery.deliverEventReminder("))
        assertTrue(milestone.contains("ReminderNotificationDelivery.deliverMilestoneReminder("))
        assertFalse(reminder.contains("\"\$title, \$content\""))
        assertFalse(milestone.contains("\"\$title, \$content\""))
        assertTrue(delivery.contains("NotificationCompat.CATEGORY_REMINDER"))
        assertTrue(delivery.contains("NotificationCompat.BigTextStyle()"))
        assertTrue(delivery.contains("repository?.getEvent(eventId)"))
        assertTrue(delivery.contains(".setGroup(GROUP_KEY)"))
    }

    private fun event(title: String, remindEnabled: Boolean) = Event(
        id = 7,
        title = title,
        date = 0L,
        category = CATEGORY_OTHER,
        remindEnabled = remindEnabled
    )

    private fun source(relative: String): String = listOf(
        File("src/main/java/com/example/timeapk/$relative"),
        File("app/src/main/java/com/example/timeapk/$relative")
    ).firstOrNull(File::exists)?.readText(Charsets.UTF_8) ?: error("Missing source: $relative")

    private fun resource(relative: String): String = listOf(
        File("src/main/res/$relative"),
        File("app/src/main/res/$relative")
    ).firstOrNull(File::exists)?.readText(Charsets.UTF_8) ?: error("Missing resource: $relative")
}
