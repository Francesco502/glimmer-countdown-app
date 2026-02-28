package com.example.timeapk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

private val THEME_MODE = intPreferencesKey("theme_mode")
private val FILTER_TYPE = intPreferencesKey("filter_type")
private val SORT_TYPE = intPreferencesKey("sort_type")
private val SHOW_HOURS = booleanPreferencesKey("show_hours")
private val SHOW_MILESTONE = booleanPreferencesKey("show_milestone")
private val HOME_DISPLAY_MODE = intPreferencesKey("home_display_mode")
/** 0=简洁 1=详细 */
private val HOME_DENSITY_MODE = intPreferencesKey("home_density_mode")
private val FONT_PRESET = intPreferencesKey("font_preset")
private val CUSTOM_BACKGROUND_HEX = stringPreferencesKey("custom_background_hex")
private val CUSTOM_SURFACE_HEX = stringPreferencesKey("custom_surface_hex")
private val CUSTOM_PRIMARY_HEX = stringPreferencesKey("custom_primary_hex")
private val CUSTOM_ON_BACKGROUND_HEX = stringPreferencesKey("custom_on_background_hex")
private val LANGUAGE_MODE = intPreferencesKey("language_mode")
/** 0=yyyy.MM.dd 1=yyyy-MM-dd */
private val DATE_FORMAT_MODE = intPreferencesKey("date_format_mode")
/** 0=天数 1=年/月/天 */
private val DATE_DELTA_DISPLAY_MODE = intPreferencesKey("date_delta_display_mode")
/** 按事件覆盖：JSON 对象 "{\"eventId\":0|1}"，未覆盖的用 DATE_DELTA_DISPLAY_MODE */
private val PER_EVENT_DATE_DELTA_MODES = stringPreferencesKey("per_event_date_delta_modes")
private val CUSTOM_MILESTONES_JSON = stringPreferencesKey("custom_milestones_json")
private val HAS_SEEN_SWIPE_HINT = booleanPreferencesKey("has_seen_swipe_hint")
private val MILESTONE_REMIND_ENABLED = booleanPreferencesKey("milestone_remind_enabled")
private val MILESTONE_REMIND_DAYS_AHEAD = intPreferencesKey("milestone_remind_days_ahead")

/** 0=跟随系统 1=浅色 2=深色 */
const val THEME_FOLLOW_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

/** 0=中文 1=English */
const val LANG_ZH = 0
const val LANG_EN = 1

/** 默认重大节点天数：100～1000 以 100 递增，后续以 500 递增（1500、2000…） */
val DEFAULT_MILESTONE_DAYS = listOf(100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 1000L)

class UserPreferencesRepository(private val context: Context) {
    val themeModeFlow: Flow<Int> = context.dataStore.data.map { it[THEME_MODE] ?: THEME_FOLLOW_SYSTEM }
    val filterTypeFlow: Flow<Int> = context.dataStore.data.map { it[FILTER_TYPE] ?: 0 }
    val sortTypeFlow: Flow<Int> = context.dataStore.data.map { it[SORT_TYPE] ?: 0 }
    val showHoursFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_HOURS] ?: true }
    val showMilestoneFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_MILESTONE] ?: true }
    val homeDisplayModeFlow: Flow<Int> = context.dataStore.data.map { it[HOME_DISPLAY_MODE] ?: 0 }
    /** 0=简洁 1=详细 */
    val homeDensityModeFlow: Flow<Int> = context.dataStore.data.map { it[HOME_DENSITY_MODE] ?: 1 }
    /** 字体预设：默认改为 4=瘦金体模拟 */
    val fontPresetFlow: Flow<Int> = context.dataStore.data.map { it[FONT_PRESET] ?: 4 }
    val customBackgroundHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_BACKGROUND_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val customSurfaceHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_SURFACE_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val customPrimaryHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_PRIMARY_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val customOnBackgroundHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_ON_BACKGROUND_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val languageModeFlow: Flow<Int> = context.dataStore.data.map { it[LANGUAGE_MODE] ?: LANG_ZH }
    /** 0=yyyy.MM.dd 1=yyyy-MM-dd */
    val dateFormatModeFlow: Flow<Int> = context.dataStore.data.map { it[DATE_FORMAT_MODE] ?: 0 }
    /** 0=天数 1=年/月/天 */
    val dateDeltaDisplayModeFlow: Flow<Int> = context.dataStore.data.map { it[DATE_DELTA_DISPLAY_MODE] ?: 0 }
    /** 按事件 ID 覆盖的显示模式，未出现的用全局 dateDeltaDisplayMode */
    val perEventDateDeltaDisplayModesFlow: Flow<Map<Int, Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[PER_EVENT_DATE_DELTA_MODES] ?: return@map emptyMap()
        parsePerEventDateDeltaModes(raw)
    }
    val hasSeenSwipeHintFlow: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_SWIPE_HINT] ?: false }

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
            val obj = org.json.JSONObject(raw)
            obj.keys().asSequence().mapNotNull { key ->
                key.toIntOrNull()?.let { id -> id to obj.optInt(key).coerceIn(0, 1) }
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    val customMilestonesFlow: Flow<List<Long>> = context.dataStore.data.map { prefs ->
        parseMilestonesJson(prefs[CUSTOM_MILESTONES_JSON])
    }
    /** 节点临近提醒开关，默认关 */
    val milestoneRemindEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[MILESTONE_REMIND_ENABLED] ?: false }
    /** 节点提醒提前天数：1/3/7/14，默认 7 */
    val milestoneRemindDaysAheadFlow: Flow<Int> = context.dataStore.data.map { it[MILESTONE_REMIND_DAYS_AHEAD] ?: 7 }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setFilterType(type: Int) {
        context.dataStore.edit { it[FILTER_TYPE] = type }
    }

    suspend fun setSortType(type: Int) {
        context.dataStore.edit { it[SORT_TYPE] = type }
    }

    suspend fun setShowHours(show: Boolean) {
        context.dataStore.edit { it[SHOW_HOURS] = show }
    }

    suspend fun setShowMilestone(show: Boolean) {
        context.dataStore.edit { it[SHOW_MILESTONE] = show }
    }

    suspend fun setHomeDisplayMode(mode: Int) {
        context.dataStore.edit { it[HOME_DISPLAY_MODE] = mode }
    }

    suspend fun setHomeDensityMode(mode: Int) {
        context.dataStore.edit { it[HOME_DENSITY_MODE] = mode }
    }

    suspend fun setFontPreset(preset: Int) {
        context.dataStore.edit { it[FONT_PRESET] = preset }
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
    }

    suspend fun setDateFormatMode(mode: Int) {
        context.dataStore.edit { it[DATE_FORMAT_MODE] = mode }
    }

    suspend fun setDateDeltaDisplayMode(mode: Int) {
        context.dataStore.edit { it[DATE_DELTA_DISPLAY_MODE] = mode.coerceIn(0, 1) }
    }

    /** 仅修改指定事件的日期显示模式，其它事件不受影响 */
    suspend fun setDateDeltaDisplayModeForEvent(eventId: Int, mode: Int) {
        val m = mode.coerceIn(0, 1)
        context.dataStore.edit { prefs ->
            val current = parsePerEventDateDeltaModes(prefs[PER_EVENT_DATE_DELTA_MODES] ?: "")
            val next = current + (eventId to m)
            prefs[PER_EVENT_DATE_DELTA_MODES] = org.json.JSONObject(next.mapKeys { it.key.toString() }).toString()
        }
    }

    suspend fun setHasSeenSwipeHint(seen: Boolean) {
        context.dataStore.edit { it[HAS_SEEN_SWIPE_HINT] = seen }
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
        context.dataStore.edit { it[MILESTONE_REMIND_DAYS_AHEAD] = days.coerceIn(1, 14) }
    }
}
