package com.example.timeapk.ui.event

import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY

data class EventEntryTemplate(
    val type: EventEntryTemplateType,
    val category: String,
    val repeatType: String,
    val allowLunar: Boolean
)

enum class EventEntryTemplateType {
    Birthday,
    Anniversary,
    Countdown
}

data class SongNamedColor(
    val nameKey: String,
    val hex: String
)

val eventEntryTemplates: List<EventEntryTemplate> = listOf(
    EventEntryTemplate(
        type = EventEntryTemplateType.Birthday,
        category = CATEGORY_BIRTHDAY,
        repeatType = REPEAT_YEARLY,
        allowLunar = true
    ),
    EventEntryTemplate(
        type = EventEntryTemplateType.Anniversary,
        category = CATEGORY_ANNIVERSARY,
        repeatType = REPEAT_YEARLY,
        allowLunar = true
    ),
    EventEntryTemplate(
        type = EventEntryTemplateType.Countdown,
        category = CATEGORY_OTHER,
        repeatType = REPEAT_NONE,
        allowLunar = false
    )
)

val songNamedColors: List<SongNamedColor> = listOf(
    SongNamedColor("ink", "#4A4933"),
    SongNamedColor("dailan", "#3A4550"),
    SongNamedColor("pine_green", "#5F856B"),
    SongNamedColor("celadon", "#83ACA2"),
    SongNamedColor("cinnabar", "#AF4E31"),
    SongNamedColor("ochre", "#AC8F62"),
    SongNamedColor("old_gold", "#9A7A3D"),
    SongNamedColor("tea_brown", "#86351C"),
    SongNamedColor("lotus_mauve", "#785B64")
)

fun defaultTemplateForCategory(category: String): EventEntryTemplate {
    return when (category) {
        CATEGORY_BIRTHDAY -> eventEntryTemplates.first { it.type == EventEntryTemplateType.Birthday }
        CATEGORY_ANNIVERSARY -> eventEntryTemplates.first { it.type == EventEntryTemplateType.Anniversary }
        else -> eventEntryTemplates.first { it.type == EventEntryTemplateType.Countdown }
    }
}

fun applyTemplateForCategory(details: EventDetails, category: String): EventDetails {
    val template = defaultTemplateForCategory(category)
    return details.copy(
        category = template.category,
        repeatType = template.repeatType,
        isLunar = if (template.allowLunar) details.isLunar else false
    )
}
