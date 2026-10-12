package com.example.timeapk.notifications

import com.example.timeapk.data.Event
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Localized building blocks for reminder notifications. Loaded from string resources at runtime;
 * tests construct them directly so the formatting rules stay JVM-testable.
 */
internal data class ReminderNotificationTemplates(
    /** "今天" / "Today" */
    val today: String,
    /** "%1$s · %2$s" — joins a relative phrase with a date or label. */
    val joinFormat: String,
    /** java.time pattern, e.g. "M月d日 EEE" / "EEE, MMM d". */
    val datePattern: String,
    val locale: Locale,
    /** Plural "还有 %d 天" / "%d days left". */
    val daysLeft: (Int) -> String,
    /** Plural "已经 %d 天" / "%d days so far". */
    val milestoneElapsed: (Long) -> String,
    /** "满 %d 天" / "%d-day mark" */
    val milestoneMark: (Long) -> String
)

internal data class ReminderNotificationContent(
    val title: String,
    val text: String,
    /** Expanded text (text + note first line); null when the event has no note. */
    val bigText: String?
)

/** Outcome of re-reading the event at fire time. */
internal sealed interface ReminderDeliveryDecision {
    data class Deliver(val title: String, val note: String, val event: Event?) : ReminderDeliveryDecision
    object Skip : ReminderDeliveryDecision
}

internal object ReminderNotificationTextPolicy {
    const val NOTE_PREVIEW_MAX_CHARS = 120

    /**
     * @param latest the event re-read by id; null when it no longer exists.
     * @param lookupFailed true when the database could not be read; only then is the work-data title used.
     * @param reminderStillEnabled whether the relevant reminder setting is still on for [latest].
     */
    fun decide(
        latest: Event?,
        lookupFailed: Boolean,
        fallbackTitle: String,
        reminderStillEnabled: (Event) -> Boolean
    ): ReminderDeliveryDecision {
        if (lookupFailed) {
            return ReminderDeliveryDecision.Deliver(fallbackTitle, note = "", event = null)
        }
        val event = latest ?: return ReminderDeliveryDecision.Skip
        if (!reminderStillEnabled(event)) return ReminderDeliveryDecision.Skip
        return ReminderDeliveryDecision.Deliver(
            title = event.title.trim().ifBlank { fallbackTitle },
            note = event.note,
            event = event
        )
    }

    fun formatDate(templates: ReminderNotificationTemplates, date: LocalDate): String =
        DateTimeFormatter.ofPattern(templates.datePattern, templates.locale).format(date)

    /** "还有 3 天 · 10月15日 周四" or "今天". Without a known date only the relative phrase is shown. */
    fun countdownText(
        templates: ReminderNotificationTemplates,
        daysLeft: Int,
        occurrenceDate: LocalDate?
    ): String {
        if (daysLeft <= 0) return templates.today
        val relative = templates.daysLeft(daysLeft)
        return occurrenceDate?.let { join(templates, relative, formatDate(templates, it)) } ?: relative
    }

    /**
     * Day-count milestones read "已经 1000 天" on the day and "还有 3 天 · 满 1000 天" before it.
     * Anniversary milestones (or legacy work without a value) use their label: "今天 · 3周年".
     */
    fun milestoneText(
        templates: ReminderNotificationTemplates,
        daysLeft: Int,
        milestoneDays: Long?,
        milestoneYears: Int?,
        label: String
    ): String {
        val isDayCount = milestoneDays != null && milestoneDays > 0 && (milestoneYears == null || milestoneYears <= 0)
        if (daysLeft <= 0) {
            return if (isDayCount) templates.milestoneElapsed(milestoneDays!!) else join(templates, templates.today, label)
        }
        val target = if (isDayCount) templates.milestoneMark(milestoneDays!!) else label
        return join(templates, templates.daysLeft(daysLeft), target)
    }

    fun noteFirstLine(note: String?): String? = note
        ?.lineSequence()
        ?.map(String::trim)
        ?.firstOrNull(String::isNotEmpty)
        ?.let { line -> if (line.length > NOTE_PREVIEW_MAX_CHARS) line.take(NOTE_PREVIEW_MAX_CHARS) + "…" else line }

    fun content(title: String, text: String, note: String?): ReminderNotificationContent {
        val noteLine = noteFirstLine(note)
        return ReminderNotificationContent(
            title = title,
            text = text,
            bigText = noteLine?.let { "$text\n$it" }
        )
    }

    /** A group summary is needed once more than one child notification is showing. */
    fun shouldPostGroupSummary(activeChildCount: Int): Boolean = activeChildCount > 1

    private fun join(templates: ReminderNotificationTemplates, first: String, second: String): String =
        String.format(templates.locale, templates.joinFormat, first, second)
}
