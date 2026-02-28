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
private val CUSTOM_MILESTONES_JSON = stringPreferencesKey("custom_milestones_json")
private val HAS_SEEN_SWIPE_HINT = booleanPreferencesKey("has_seen_swipe_hint")

/** 0=跟随系统 1=浅色 2=深色 */
const val THEME_FOLLOW_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

/** 0=中文 1=English */
const val LANG_ZH = 0
const val LANG_EN = 1

/** 默认重大节点天数 */
val DEFAULT_MILESTONE_DAYS = listOf(7L, 30L, 100L, 365L, 520L, 1000L)

class UserPreferencesRepository(private val context: Context) {
    val themeModeFlow: Flow<Int> = context.dataStore.data.map { it[THEME_MODE] ?: THEME_FOLLOW_SYSTEM }
    val filterTypeFlow: Flow<Int> = context.dataStore.data.map { it[FILTER_TYPE] ?: 0 }
    val sortTypeFlow: Flow<Int> = context.dataStore.data.map { it[SORT_TYPE] ?: 0 }
    val showHoursFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_HOURS] ?: true }
    val showMilestoneFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_MILESTONE] ?: true }
    val homeDisplayModeFlow: Flow<Int> = context.dataStore.data.map { it[HOME_DISPLAY_MODE] ?: 0 }
    /** 0=简洁 1=详细 */
    val homeDensityModeFlow: Flow<Int> = context.dataStore.data.map { it[HOME_DENSITY_MODE] ?: 1 }
    val fontPresetFlow: Flow<Int> = context.dataStore.data.map { it[FONT_PRESET] ?: 0 }
    val customBackgroundHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_BACKGROUND_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val customSurfaceHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_SURFACE_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val customPrimaryHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_PRIMARY_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val customOnBackgroundHexFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_ON_BACKGROUND_HEX].takeIf { s -> !s.isNullOrBlank() } }
    val languageModeFlow: Flow<Int> = context.dataStore.data.map { it[LANGUAGE_MODE] ?: LANG_ZH }
    /** 0=yyyy.MM.dd 1=yyyy-MM-dd */
    val dateFormatModeFlow: Flow<Int> = context.dataStore.data.map { it[DATE_FORMAT_MODE] ?: 0 }
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

    val customMilestonesFlow: Flow<List<Long>> = context.dataStore.data.map { prefs ->
        parseMilestonesJson(prefs[CUSTOM_MILESTONES_JSON])
    }

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

    suspend fun setHasSeenSwipeHint(seen: Boolean) {
        context.dataStore.edit { it[HAS_SEEN_SWIPE_HINT] = seen }
    }

    suspend fun setCustomMilestones(days: List<Long>) {
        val list = days.filter { it > 0 }.distinct().sorted()
        val json = JSONArray(list).toString()
        context.dataStore.edit { it[CUSTOM_MILESTONES_JSON] = json }
    }
}
