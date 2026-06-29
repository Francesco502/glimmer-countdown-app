package com.example.timeapk

import android.app.Application
import android.content.res.Configuration
import com.example.timeapk.data.AppDatabase
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.notifications.RescheduleAllWorker
import com.example.timeapk.update.UpdateChecker
import com.example.timeapk.update.UpdateCheckerFactory
import com.example.timeapk.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TimeApplication : Application() {
    private var lastUiModeNight: Int = -1
    private var lastThemeMode: Int? = null
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EventRepository(database.eventDao()) }
    val userPrefs by lazy { UserPreferencesRepository(this) }
    val updateChecker: UpdateChecker by lazy { UpdateCheckerFactory.create() }
    var initialCategoryForAdd: String? = null

    fun launchAppTask(block: suspend CoroutineScope.() -> Unit) {
        appScope.launch(block = block)
    }

    override fun onCreate() {
        super.onCreate()
        lastUiModeNight = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        RescheduleAllWorker.enqueue(this, "cold_start")
        observeThemeModeChanges()
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
        WidgetUpdater.refreshCountdownWidgets(this)
    }

    private fun observeThemeModeChanges() {
        appScope.launch {
            userPrefs.themeModeFlow.collectLatest { themeMode ->
                val previous = lastThemeMode
                lastThemeMode = themeMode
                if (previous != null && previous != themeMode) {
                    refreshWidgetsOnThemeChange()
                }
            }
        }
    }
}
