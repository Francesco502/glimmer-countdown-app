package com.example.timeapk.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal val Context.widgetConfigDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_preferences"
)

private val WIDGET_DEFAULT_CONFIG_JSON = stringPreferencesKey("widget_default_config_json")
private val WIDGET_INSTANCE_CONFIGS_JSON = stringPreferencesKey("widget_instance_configs_json")
private val SINGLE_EVENT_SELECTIONS_JSON = stringPreferencesKey("single_event_widget_selections_json")

internal fun decodeSingleEventSelections(raw: String?): Map<Int, Int> {
    if (raw.isNullOrBlank()) return emptyMap()
    return try {
        val json = org.json.JSONObject(raw)
        json.keys().asSequence().mapNotNull { key ->
            val widgetId = key.toIntOrNull() ?: return@mapNotNull null
            widgetId to json.optInt(key, SINGLE_EVENT_FOLLOW_NEAREST)
        }.toMap()
    } catch (_: Exception) {
        emptyMap()
    }
}

internal fun encodeSingleEventSelections(selections: Map<Int, Int>): String =
    org.json.JSONObject().apply { selections.forEach { (widgetId, eventId) -> put(widgetId.toString(), eventId) } }.toString()

class WidgetConfigRepository(private val context: Context) {
    val defaultConfigFlow: Flow<WidgetConfig> = context.widgetConfigDataStore.data.map { prefs ->
        WidgetConfig.fromJson(prefs[WIDGET_DEFAULT_CONFIG_JSON])
    }

    val instanceConfigsFlow: Flow<Map<Int, WidgetConfig>> = context.widgetConfigDataStore.data.map { prefs ->
        decodeWidgetInstanceConfigs(prefs[WIDGET_INSTANCE_CONFIGS_JSON])
    }

    suspend fun getDefaultConfig(): WidgetConfig {
        return defaultConfigFlow.first()
    }

    suspend fun setDefaultConfig(config: WidgetConfig) {
        context.widgetConfigDataStore.edit { prefs ->
            prefs[WIDGET_DEFAULT_CONFIG_JSON] = config.sanitize().toJson()
        }
    }

    suspend fun getConfigForWidget(appWidgetId: Int): WidgetConfig {
        val prefs = context.widgetConfigDataStore.data.first()
        val instances = decodeWidgetInstanceConfigs(prefs[WIDGET_INSTANCE_CONFIGS_JSON])
        return instances[appWidgetId] ?: WidgetConfig.fromJson(prefs[WIDGET_DEFAULT_CONFIG_JSON])
    }

    suspend fun setConfigForWidget(appWidgetId: Int, config: WidgetConfig) {
        context.widgetConfigDataStore.edit { prefs ->
            val current = decodeWidgetInstanceConfigs(prefs[WIDGET_INSTANCE_CONFIGS_JSON])
            prefs[WIDGET_INSTANCE_CONFIGS_JSON] = encodeWidgetInstanceConfigs(
                current + (appWidgetId to config.sanitize())
            )
        }
    }

    suspend fun getInstanceConfigs(): Map<Int, WidgetConfig> {
        val prefs = context.widgetConfigDataStore.data.first()
        return decodeWidgetInstanceConfigs(prefs[WIDGET_INSTANCE_CONFIGS_JSON])
    }

    suspend fun setAllInstanceConfigs(configs: Map<Int, WidgetConfig>) {
        context.widgetConfigDataStore.edit { prefs ->
            prefs[WIDGET_INSTANCE_CONFIGS_JSON] = encodeWidgetInstanceConfigs(configs)
        }
    }

    /** 单事件小组件选中的事件 id；未配置时跟随最近事件。 */
    suspend fun getSingleEventSelection(appWidgetId: Int): Int {
        val prefs = context.widgetConfigDataStore.data.first()
        return decodeSingleEventSelections(prefs[SINGLE_EVENT_SELECTIONS_JSON])[appWidgetId]
            ?: SINGLE_EVENT_FOLLOW_NEAREST
    }

    suspend fun setSingleEventSelection(appWidgetId: Int, eventId: Int) {
        context.widgetConfigDataStore.edit { prefs ->
            val current = decodeSingleEventSelections(prefs[SINGLE_EVENT_SELECTIONS_JSON])
            prefs[SINGLE_EVENT_SELECTIONS_JSON] = encodeSingleEventSelections(current + (appWidgetId to eventId))
        }
    }

    suspend fun removeSingleEventSelection(appWidgetId: Int) {
        context.widgetConfigDataStore.edit { prefs ->
            val current = decodeSingleEventSelections(prefs[SINGLE_EVENT_SELECTIONS_JSON])
            prefs[SINGLE_EVENT_SELECTIONS_JSON] = encodeSingleEventSelections(current - appWidgetId)
        }
    }

    suspend fun removeConfigForWidget(appWidgetId: Int) {
        context.widgetConfigDataStore.edit { prefs ->
            prefs[WIDGET_INSTANCE_CONFIGS_JSON] = removeWidgetInstanceConfig(
                prefs[WIDGET_INSTANCE_CONFIGS_JSON],
                appWidgetId
            )
        }
    }
}
