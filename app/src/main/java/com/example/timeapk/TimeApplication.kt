package com.example.timeapk

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import com.example.timeapk.data.AppDatabase
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.notifications.RescheduleAllWorker
import com.example.timeapk.update.GitHubReleaseUpdateChecker
import com.example.timeapk.update.UpdateChecker
import com.example.timeapk.R
import com.example.timeapk.widget.CountdownAppWidgetProvider

class TimeApplication : Application() {

    private var lastUiModeNight: Int = -1

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EventRepository(database.eventDao()) }
    val userPrefs by lazy { UserPreferencesRepository(this) }
    val updateChecker: UpdateChecker by lazy { GitHubReleaseUpdateChecker() }
    var initialCategoryForAdd: String? = null

    override fun onCreate() {
        super.onCreate()
        lastUiModeNight = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        RescheduleAllWorker.enqueue(this, "cold_start")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val current = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (lastUiModeNight != current && lastUiModeNight != -1) {
            refreshWidgetsOnThemeChange()
        }
        lastUiModeNight = current
    }

    /** 系统深浅模式切换时刷新小组件，使桌面小组件跟随系统主题 */
    private fun refreshWidgetsOnThemeChange() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, CountdownAppWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(provider)
        if (ids.isEmpty()) return
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
    }
}
