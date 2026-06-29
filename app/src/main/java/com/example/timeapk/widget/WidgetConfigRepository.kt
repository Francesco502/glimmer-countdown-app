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

private val Context.widgetConfigDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_preferences"
)

private val WIDGET_DEFAULT_CONFIG_JSON = stringPreferencesKey("widget_default_config_json")
private val WIDGET_INSTANCE_CONFIGS_JSON = stringPreferencesKey("widget_instance_configs_json")

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

    suspend fun removeConfigForWidget(appWidgetId: Int) {
        context.widgetConfigDataStore.edit { prefs ->
            prefs[WIDGET_INSTANCE_CONFIGS_JSON] = removeWidgetInstanceConfig(
                prefs[WIDGET_INSTANCE_CONFIGS_JSON],
                appWidgetId
            )
        }
    }
}
