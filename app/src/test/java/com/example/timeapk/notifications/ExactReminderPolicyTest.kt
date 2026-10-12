package com.example.timeapk.notifications

import com.example.timeapk.ui.settings.ExactReminderStatus
import com.example.timeapk.ui.settings.exactReminderStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExactReminderPolicyTest {

    @Test
    fun exactPathRequiresChannelOptInAndSystemPermission() {
        assertEquals(
            ReminderDeliveryPath.EXACT_ALARM,
            ExactReminderPolicy.selectPath(channelSupportsExact = true, userEnabled = true, canScheduleExactAlarms = true)
        )
        listOf(
            Triple(false, true, true),
            Triple(true, false, true),
            Triple(true, true, false),
            Triple(false, false, false)
        ).forEach { (channel, user, system) ->
            assertEquals(
                "channel=$channel user=$user system=$system",
                ReminderDeliveryPath.WORK_MANAGER,
                ExactReminderPolicy.selectPath(channel, user, system)
            )
        }
    }

    @Test
    fun exactAlarmsAreUngatedBeforeApi31() {
        assertTrue(ExactReminderPolicy.canScheduleExact(30) { false })
        assertFalse(ExactReminderPolicy.canScheduleExact(33) { false })
        assertTrue(ExactReminderPolicy.canScheduleExact(33) { true })
    }

    @Test
    fun requestCodesAreUniqueAcrossKindsAndEvents() {
        val ids = (0..5_000) + listOf(65_535, 1_000_000, ExactReminderPolicy.EVENT_ID_MASK)
        val codes = ExactAlarmKind.entries.flatMap { kind ->
            ids.map { id -> ExactReminderPolicy.requestCode(kind, id) }
        }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(codes.all { it > 0 })
        assertEquals(
            ExactAlarmKind.entries.size,
            ExactAlarmKind.entries.map { ExactReminderPolicy.dataUri(it, 1) }.toSet().size
        )
        assertEquals(ExactAlarmKind.entries.size, ExactAlarmKind.entries.map { it.action }.toSet().size)
    }

    @Test
    fun settingsStatusReflectsPreferenceAndPermission() {
        assertEquals(ExactReminderStatus.OFF, exactReminderStatus(userEnabled = false, canScheduleExact = true))
        assertEquals(ExactReminderStatus.ACTIVE, exactReminderStatus(userEnabled = true, canScheduleExact = true))
        assertEquals(
            ExactReminderStatus.PERMISSION_NEEDED,
            exactReminderStatus(userEnabled = true, canScheduleExact = false)
        )
    }

    @Test
    fun cancellationAlwaysClearsBothPaths() {
        val reminder = source("java/com/example/timeapk/notifications/ReminderScheduler.kt")
        val milestone = source("java/com/example/timeapk/notifications/MilestoneReminderScheduler.kt")
        val cancelReminder = reminder.substringAfter("fun cancelReminder(")
        assertTrue(cancelReminder.contains("cancelUniqueWork("))
        assertTrue(cancelReminder.contains("ExactReminderAlarms.cancel(context, ExactAlarmKind.EVENT_REMINDER, eventId)"))
        val cancelMilestone = milestone.substringAfter("fun cancelMilestoneReminders(").substringBefore("internal fun")
        assertTrue(cancelMilestone.contains("ExactReminderAlarms.cancel(context, ExactAlarmKind.MILESTONE_REMINDER, eventId)"))
        assertTrue(cancelMilestone.contains("ExactReminderAlarms.cancelAllMilestones(context)"))
    }

    @Test
    fun manifestsDeclareExactAlarmForDirectAndRemoveItForPlay() {
        val main = source("AndroidManifest.xml")
        val play = listOf(File("src/play/AndroidManifest.xml"), File("app/src/play/AndroidManifest.xml"))
            .first(File::exists).readText()
        assertTrue(main.contains("android.permission.SCHEDULE_EXACT_ALARM"))
        assertTrue(main.contains(".notifications.ExactReminderAlarmReceiver"))
        assertTrue(main.contains("android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"))
        assertTrue(
            play.substringAfter("android:name=\"android.permission.SCHEDULE_EXACT_ALARM\"").trimStart().startsWith("tools:node=\"remove\"")
        )
    }

    private fun source(relative: String): String = listOf(
        File("src/main/$relative"),
        File("app/src/main/$relative")
    ).first(File::exists).readText(Charsets.UTF_8)
}
