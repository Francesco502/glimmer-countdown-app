package com.example.timeapk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.timeapk.LocalePreferenceMirror
import com.example.timeapk.ui.theme.FONT_PRESET_NOTO_SERIF_SC
import com.example.timeapk.ui.theme.sanitizeFontPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

private val THEME_MODE = intPreferencesKey("theme_mode")
private val FILTER_TYPE = intPreferencesKey("filter_type")
private val SORT_TYPE = intPreferencesKey("sort_type")
private val HAS_MIGRATED_HOME_SORT_TO_CUSTOM = booleanPreferencesKey("has_migrated_home_sort_to_custom")
private val SHOW_HOURS = booleanPreferencesKey("show_hours")
private val SHOW_MILESTONE = booleanPreferencesKey("show_milestone")
private val HOME_DISPLAY_MODE = intPreferencesKey("home_display_mode")
private val HOME_DENSITY_MODE = intPreferencesKey("home_density_mode")
private val FONT_PRESET = intPreferencesKey("font_preset")
private val APP_BASE_FONT_SCALE = floatPreferencesKey("app_base_font_scale")
private val WIDGET_FONT_SCALE = floatPreferencesKey("widget_font_scale")
private val CUSTOM_BACKGROUND_HEX = stringPreferencesKey("custom_background_hex")
private val CUSTOM_SURFACE_HEX = stringPreferencesKey("custom_surface_hex")
private val CUSTOM_PRIMARY_HEX = stringPreferencesKey("custom_primary_hex")
private val CUSTOM_ON_BACKGROUND_HEX = stringPreferencesKey("custom_on_background_hex")
private val LANGUAGE_MODE = intPreferencesKey("language_mode")
private val DATE_FORMAT_MODE = intPreferencesKey("date_format_mode")
private val DATE_DELTA_DISPLAY_MODE = intPreferencesKey("date_delta_display_mode")
private val PER_EVENT_DATE_DELTA_MODES = stringPreferencesKey("per_event_date_delta_modes")
private val CUSTOM_MILESTONES_JSON = stringPreferencesKey("custom_milestones_json")
private val CUSTOM_EVENT_ORDER_JSON = stringPreferencesKey("custom_event_order_json")
private val PINNED_EVENT_IDS_JSON = stringPreferencesKey("pinned_event_ids_json")
private val MILESTONE_REMIND_ENABLED = booleanPreferencesKey("milestone_remind_enabled")
private val MILESTONE_REMIND_DAYS_AHEAD = intPreferencesKey("milestone_remind_days_ahead")
private val MILESTONE_REMIND_TIME_MINUTES_OF_DAY = intPreferencesKey("milestone_remind_time_minutes_of_day")
private val DEFAULT_EVENT_REMIND_ENABLED = booleanPreferencesKey("default_event_remind_enabled")
private val DEFAULT_EVENT_REMIND_DAYS_BEFORE = intPreferencesKey("default_event_remind_days_before")
private val DEFAULT_EVENT_REMIND_TIME_MINUTES_OF_DAY = intPreferencesKey("default_event_remind_time_minutes_of_day")
private val SMART_MILESTONES_ENABLED = booleanPreferencesKey("smart_milestones_enabled")
private val REDUCE_MOTION_ENABLED = booleanPreferencesKey("reduce_motion_enabled")
private val SONG_SOUND_ENABLED = booleanPreferencesKey("song_sound_enabled")
private val SCHEDULE_TARGET_CALENDAR_ID = longPreferencesKey("schedule_target_calendar_id")
private val SCHEDULE_USE_RRULE_SYNC = booleanPreferencesKey("schedule_use_rrule_sync")

private const val APP_BASE_FONT_SCALE_MIN = 0.85f
private const val APP_BASE_FONT_SCALE_MAX = 1.30f
private const val WIDGET_FONT_SCALE_MIN = 0.85f
private const val WIDGET_FONT_SCALE_MAX = 1.60f

const val THEME_FOLLOW_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

const val LANG_ZH = 0
const val LANG_EN = 1

const val DEFAULT_NEW_EVENT_REMIND_ENABLED = true
const val DEFAULT_NEW_EVENT_REMIND_DAYS_BEFORE = 7
const val DEFAULT_NEW_EVENT_REMIND_TIME_MINUTES_OF_DAY = 10 * 60

internal const val HOME_SORT_BY_DAYS = 0
internal const val HOME_SORT_BY_DATE = 1
internal const val HOME_SORT_CUSTOM = 2

val DEFAULT_MILESTONE_DAYS = listOf(100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 1000L)

internal fun resolveHomeSortPreference(storedSortType: Int?, hasMigratedToCustomSort: Boolean): Int {
    if (!hasMigratedToCustomSort) return HOME_SORT_CUSTOM
    return when (storedSortType) {
        HOME_SORT_BY_DAYS,
        HOME_SORT_BY_DATE,
        HOME_SORT_CUSTOM -> storedSortType
        else -> HOME_SORT_CUSTOM
    }
}

internal data class HomeSortSelectionUpdate(
    val sortType: Int,
    val hasMigratedToCustomSort: Boolean
)

data class DefaultEventReminderSettings(
    val enabled: Boolean = DEFAULT_NEW_EVENT_REMIND_ENABLED,
    val daysBefore: Int = DEFAULT_NEW_EVENT_REMIND_DAYS_BEFORE,
    val timeMinutesOfDay: Int = DEFAULT_NEW_EVENT_REMIND_TIME_MINUTES_OF_DAY
)

internal fun resolveHomeSortSelectionUpdate(selectedSortType: Int): HomeSortSelectionUpdate {
    return HomeSortSelectionUpdate(
        sortType = resolveHomeSortPreference(
            storedSortType = selectedSortType,
            hasMigratedToCustomSort = true
        ),
        hasMigratedToCustomSort = true
    )
}

class UserPreferencesRepository(private val context: Context) {
    val themeModeFlow: Flow<Int> = context.dataStore.data.map { it[THEME_MODE] ?: THEME_FOLLOW_SYSTEM }
    val filterTypeFlow: Flow<Int> = context.dataStore.data.map { it[FILTER_TYPE] ?: 0 }
    val sortTypeFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        resolveHomeSortPreference(
            storedSortType = prefs[SORT_TYPE],
            hasMigratedToCustomSort = prefs[HAS_MIGRATED_HOME_SORT_TO_CUSTOM] ?: false
        )
    }
    val showHoursFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_HOURS] ?: true }
    val showMilestoneFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_MILESTONE] ?: true }
    val homeDisplayModeFlow: Flow<Int> = context.dataStore.data.map { (it[HOME_DISPLAY_MODE] ?: 0).coerceIn(0, 2) }
    val homeDensityModeFlow: Flow<Int> = context.dataStore.data.map { it[HOME_DENSITY_MODE] ?: 1 }
    val fontPresetFlow: Flow<Int> = context.dataStore.data.map {
        sanitizeFontPreset(it[FONT_PRESET] ?: FONT_PRESET_NOTO_SERIF_SC)
    }
    val appBaseFontScaleFlow: Flow<Float> = context.dataStore.data.map {
        sanitizeAppBaseFontScale(it[APP_BASE_FONT_SCALE] ?: 1f)
    }
    val widgetFontScaleFlow: Flow<Float> = context.dataStore.data.map {
        sanitizeWidgetFontScale(it[WIDGET_FONT_SCALE] ?: 1f)
    }
    val customBackgroundHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_BACKGROUND_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val customSurfaceHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_SURFACE_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val customPrimaryHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_PRIMARY_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val customOnBackgroundHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_ON_BACKGROUND_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val languageModeFlow: Flow<Int> = context.dataStore.data.map { it[LANGUAGE_MODE] ?: LANG_ZH }
    val dateFormatModeFlow: Flow<Int> = context.dataStore.data.map { it[DATE_FORMAT_MODE] ?: 0 }
    val dateDeltaDisplayModeFlow: Flow<Int> = context.dataStore.data.map { (it[DATE_DELTA_DISPLAY_MODE] ?: 0).coerceIn(0, 4) }
    val perEventDateDeltaDisplayModesFlow: Flow<Map<Int, Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[PER_EVENT_DATE_DELTA_MODES] ?: return@map emptyMap()
        parsePerEventDateDeltaModes(raw)
    }
    val customEventOrderFlow: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        parseCustomEventOrder(prefs[CUSTOM_EVENT_ORDER_JSON] ?: "")
    }
    val pinnedEventIdsFlow: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        parseCustomEventOrder(prefs[PINNED_EVENT_IDS_JSON] ?: "")
    }
    val customMilestonesFlow: Flow<List<Long>> = context.dataStore.data.map { prefs ->
        parseMilestonesJson(prefs[CUSTOM_MILESTONES_JSON])
    }
    val milestoneRemindEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[MILESTONE_REMIND_ENABLED] ?: false }
    val milestoneRemindDaysAheadFlow: Flow<Int> = context.dataStore.data.map {
        sanitizeRemindDaysBefore(it[MILESTONE_REMIND_DAYS_AHEAD] ?: 7)
    }
    val milestoneRemindTimeMinutesOfDayFlow: Flow<Int> = context.dataStore.data.map {
        sanitizeReminderTimeMinutesOfDay(it[MILESTONE_REMIND_TIME_MINUTES_OF_DAY] ?: 480)
    }
    val defaultEventRemindEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[DEFAULT_EVENT_REMIND_ENABLED] ?: DEFAULT_NEW_EVENT_REMIND_ENABLED
    }
    val defaultEventRemindDaysBeforeFlow: Flow<Int> = context.dataStore.data.map {
        sanitizeRemindDaysBefore(it[DEFAULT_EVENT_REMIND_DAYS_BEFORE] ?: DEFAULT_NEW_EVENT_REMIND_DAYS_BEFORE)
    }
    val defaultEventRemindTimeMinutesOfDayFlow: Flow<Int> = context.dataStore.data.map {
        sanitizeReminderTimeMinutesOfDay(
            it[DEFAULT_EVENT_REMIND_TIME_MINUTES_OF_DAY] ?: DEFAULT_NEW_EVENT_REMIND_TIME_MINUTES_OF_DAY
        )
    }
    val smartMilestonesEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[SMART_MILESTONES_ENABLED] ?: true
    }
    val reduceMotionEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[REDUCE_MOTION_ENABLED] ?: false
    }
    val songSoundEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[SONG_SOUND_ENABLED] ?: false
    }
    val scheduleTargetCalendarIdFlow: Flow<Long?> = context.dataStore.data.map {
        it[SCHEDULE_TARGET_CALENDAR_ID]
    }
    val scheduleUseRRuleSyncFlow: Flow<Boolean> = context.dataStore.data.map {
        it[SCHEDULE_USE_RRULE_SYNC] ?: true
    }

    private fun parseCustomEventOrder(raw: String): List<Int> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i -> arr.optInt(i) }.filter { it != 0 }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseMilestonesJson(json: String?): List<Long> {
        if (json.isNullOrBlank()) return DEFAULT_MILESTONE_DAYS
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i -> arr.optLong(i) }.filter { it > 0 }.distinct().sorted()
        } catch (_: Exception) {
            DEFAULT_MILESTONE_DAYS
        }
    }

    private fun parsePerEventDateDeltaModes(raw: String): Map<Int, Int> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val obj = JSONObject(raw)
            obj.keys().asSequence().mapNotNull { key ->
                key.toIntOrNull()?.let { id -> id to obj.optInt(key).coerceIn(0, 4) }
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setFilterType(type: Int) {
        context.dataStore.edit { it[FILTER_TYPE] = type }
    }

    suspend fun setSortType(type: Int) {
        context.dataStore.edit { prefs ->
            val update = resolveHomeSortSelectionUpdate(type)
            prefs[SORT_TYPE] = update.sortType
            prefs[HAS_MIGRATED_HOME_SORT_TO_CUSTOM] = update.hasMigratedToCustomSort
        }
    }

    suspend fun migrateHomeSortToCustomIfNeeded() {
        context.dataStore.edit { prefs ->
            val hasMigrated = prefs[HAS_MIGRATED_HOME_SORT_TO_CUSTOM] ?: false
            if (!hasMigrated) {
                prefs[SORT_TYPE] = HOME_SORT_CUSTOM
                prefs[HAS_MIGRATED_HOME_SORT_TO_CUSTOM] = true
            }
        }
    }

    suspend fun setShowHours(show: Boolean) {
        context.dataStore.edit { it[SHOW_HOURS] = show }
    }

    suspend fun setShowMilestone(show: Boolean) {
        context.dataStore.edit { it[SHOW_MILESTONE] = show }
    }

    suspend fun setHomeDisplayMode(mode: Int) {
        context.dataStore.edit { it[HOME_DISPLAY_MODE] = mode.coerceIn(0, 2) }
    }

    suspend fun setHomeDensityMode(mode: Int) {
        context.dataStore.edit { it[HOME_DENSITY_MODE] = mode }
    }

    suspend fun setFontPreset(preset: Int) {
        context.dataStore.edit { it[FONT_PRESET] = sanitizeFontPreset(preset) }
    }

    suspend fun setAppBaseFontScale(scale: Float) {
        context.dataStore.edit { it[APP_BASE_FONT_SCALE] = sanitizeAppBaseFontScale(scale) }
    }

    suspend fun setWidgetFontScale(scale: Float) {
        context.dataStore.edit { it[WIDGET_FONT_SCALE] = sanitizeWidgetFontScale(scale) }
    }

    suspend fun setCustomBackgroundHex(hex: String?) {
        context.dataStore.edit { it[CUSTOM_BACKGROUND_HEX] = hex ?: "" }
    }

    suspend fun setCustomSurfaceHex(hex: String?) {
        context.dataStore.edit { it[CUSTOM_SURFACE_HEX] = hex ?: "" }
    }

    suspend fun setCustomPrimaryHex(hex: String?) {
        context.dataStore.edit { it[CUSTOM_PRIMARY_HEX] = hex ?: "" }
    }

    suspend fun setCustomOnBackgroundHex(hex: String?) {
        context.dataStore.edit { it[CUSTOM_ON_BACKGROUND_HEX] = hex ?: "" }
    }

    suspend fun setLanguageMode(mode: Int) {
        context.dataStore.edit { it[LANGUAGE_MODE] = mode }
        LocalePreferenceMirror.write(context, mode)
    }

    suspend fun setDateFormatMode(mode: Int) {
        context.dataStore.edit { it[DATE_FORMAT_MODE] = mode }
    }

    suspend fun setDateDeltaDisplayMode(mode: Int) {
        context.dataStore.edit { it[DATE_DELTA_DISPLAY_MODE] = mode.coerceIn(0, 4) }
    }

    suspend fun setDateDeltaDisplayModeForEvent(eventId: Int, mode: Int) {
        val m = mode.coerceIn(0, 4)
        context.dataStore.edit { prefs ->
            val current = parsePerEventDateDeltaModes(prefs[PER_EVENT_DATE_DELTA_MODES] ?: "")
            val next = current + (eventId to m)
            prefs[PER_EVENT_DATE_DELTA_MODES] = JSONObject(next.mapKeys { it.key.toString() }).toString()
        }
    }

    suspend fun setCustomEventOrder(orderedIds: List<Int>) {
        val json = JSONArray(orderedIds).toString()
        context.dataStore.edit { it[CUSTOM_EVENT_ORDER_JSON] = json }
    }

    suspend fun togglePinnedEventId(eventId: Int) {
        context.dataStore.edit { prefs ->
            val current = parseCustomEventOrder(prefs[PINNED_EVENT_IDS_JSON] ?: "")
            val next = if (eventId in current) current.filter { it != eventId } else listOf(eventId) + current
            prefs[PINNED_EVENT_IDS_JSON] = JSONArray(next).toString()
        }
    }

    suspend fun setCustomMilestones(days: List<Long>) {
        val list = days.filter { it > 0 }.distinct().sorted()
        val json = JSONArray(list).toString()
        context.dataStore.edit { it[CUSTOM_MILESTONES_JSON] = json }
    }

    suspend fun setMilestoneRemindEnabled(enabled: Boolean) {
        context.dataStore.edit { it[MILESTONE_REMIND_ENABLED] = enabled }
    }

    suspend fun setMilestoneRemindDaysAhead(days: Int) {
        context.dataStore.edit { it[MILESTONE_REMIND_DAYS_AHEAD] = sanitizeRemindDaysBefore(days) }
    }

    suspend fun setMilestoneRemindTimeMinutesOfDay(minutesOfDay: Int) {
        context.dataStore.edit {
            it[MILESTONE_REMIND_TIME_MINUTES_OF_DAY] = sanitizeReminderTimeMinutesOfDay(minutesOfDay)
        }
    }

    suspend fun getDefaultEventReminderSettings(): DefaultEventReminderSettings {
        val prefs = context.dataStore.data.first()
        return DefaultEventReminderSettings(
            enabled = prefs[DEFAULT_EVENT_REMIND_ENABLED] ?: DEFAULT_NEW_EVENT_REMIND_ENABLED,
            daysBefore = sanitizeRemindDaysBefore(
                prefs[DEFAULT_EVENT_REMIND_DAYS_BEFORE] ?: DEFAULT_NEW_EVENT_REMIND_DAYS_BEFORE
            ),
            timeMinutesOfDay = sanitizeReminderTimeMinutesOfDay(
                prefs[DEFAULT_EVENT_REMIND_TIME_MINUTES_OF_DAY] ?: DEFAULT_NEW_EVENT_REMIND_TIME_MINUTES_OF_DAY
            )
        )
    }

    suspend fun setDefaultEventRemindEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DEFAULT_EVENT_REMIND_ENABLED] = enabled }
    }

    suspend fun setDefaultEventRemindDaysBefore(days: Int) {
        context.dataStore.edit {
            it[DEFAULT_EVENT_REMIND_DAYS_BEFORE] = sanitizeRemindDaysBefore(days)
        }
    }

    suspend fun setDefaultEventRemindTimeMinutesOfDay(minutesOfDay: Int) {
        context.dataStore.edit {
            it[DEFAULT_EVENT_REMIND_TIME_MINUTES_OF_DAY] = sanitizeReminderTimeMinutesOfDay(minutesOfDay)
        }
    }

    suspend fun setSmartMilestonesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SMART_MILESTONES_ENABLED] = enabled }
    }

    suspend fun setReduceMotionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[REDUCE_MOTION_ENABLED] = enabled }
    }

    suspend fun setSongSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SONG_SOUND_ENABLED] = enabled }
    }

    suspend fun setScheduleTargetCalendarId(calendarId: Long?) {
        context.dataStore.edit { prefs ->
            if (calendarId == null) {
                prefs.remove(SCHEDULE_TARGET_CALENDAR_ID)
            } else {
                prefs[SCHEDULE_TARGET_CALENDAR_ID] = calendarId
            }
        }
    }

    suspend fun setScheduleUseRRuleSync(enabled: Boolean) {
        context.dataStore.edit { it[SCHEDULE_USE_RRULE_SYNC] = enabled }
    }

    private fun sanitizeAppBaseFontScale(scale: Float): Float {
        return scale.coerceIn(APP_BASE_FONT_SCALE_MIN, APP_BASE_FONT_SCALE_MAX)
    }

    private fun sanitizeWidgetFontScale(scale: Float): Float {
        return scale.coerceIn(WIDGET_FONT_SCALE_MIN, WIDGET_FONT_SCALE_MAX)
    }
}
