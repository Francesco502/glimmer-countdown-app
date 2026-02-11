package com.example.timeapk.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

private val THEME_MODE = intPreferencesKey("theme_mode")
private val FILTER_TYPE = intPreferencesKey("filter_type")
private val SORT_TYPE = intPreferencesKey("sort_type")
private val SHOW_HOURS = booleanPreferencesKey("show_hours")
private val LANGUAGE_MODE = intPreferencesKey("language_mode")

/** 0=跟随系统 1=浅色 2=深色 */
const val THEME_FOLLOW_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

/** 0=中文 1=English */
const val LANG_ZH = 0
const val LANG_EN = 1

class UserPreferencesRepository(private val context: Context) {
    val themeModeFlow: Flow<Int> = context.dataStore.data.map { it[THEME_MODE] ?: THEME_FOLLOW_SYSTEM }
    val filterTypeFlow: Flow<Int> = context.dataStore.data.map { it[FILTER_TYPE] ?: 0 }
    val sortTypeFlow: Flow<Int> = context.dataStore.data.map { it[SORT_TYPE] ?: 0 }
    val showHoursFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_HOURS] ?: true }
    val languageModeFlow: Flow<Int> = context.dataStore.data.map { it[LANGUAGE_MODE] ?: LANG_ZH }

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

    suspend fun setLanguageMode(mode: Int) {
        context.dataStore.edit { it[LANGUAGE_MODE] = mode }
    }
}
