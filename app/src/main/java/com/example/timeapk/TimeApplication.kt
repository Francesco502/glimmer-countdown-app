package com.example.timeapk

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.example.timeapk.data.AppDatabase
import com.example.timeapk.data.CategoryRepository
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.notifications.ReminderNotificationDelivery
import com.example.timeapk.notifications.RescheduleAllWorker
import com.example.timeapk.update.UpdateChecker
import com.example.timeapk.update.UpdateCheckerFactory
import com.example.timeapk.update.UpdateDownloadState
import com.example.timeapk.update.UpdateInstallOutcome
import com.example.timeapk.update.UpdateInstaller
import com.example.timeapk.widget.WidgetUpdater
import com.example.timeapk.ui.common.EventImageStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimeApplication : Application() {
    private var lastUiModeNight: Int = -1
    private var lastFontScale: Float = 1f
    private var lastThemeMode: Int? = null
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EventRepository(database.eventDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val userPrefs by lazy { UserPreferencesRepository(this) }
    internal val backupImportInProgress = MutableStateFlow(false)
    val updateChecker: UpdateChecker by lazy { UpdateCheckerFactory.create() }

    private val _updateDownloadState = MutableStateFlow(UpdateDownloadState())
    val updateDownloadState: StateFlow<UpdateDownloadState> = _updateDownloadState.asStateFlow()
    @Volatile
    private var pendingUpdateInstall = false

    /** Starts (or joins) the single in-flight update download; survives leaving the About screen. */
    fun startUpdateDownload(downloadUrl: String, expectedSha256: String?) {
        if (_updateDownloadState.value.downloading) return
        _updateDownloadState.value = UpdateDownloadState(downloading = true)
        appScope.launch {
            val outcome = UpdateInstaller.downloadAndInstall(
                context = this@TimeApplication,
                downloadUrl = downloadUrl,
                expectedSha256 = expectedSha256
            ) { progress ->
                _updateDownloadState.value = _updateDownloadState.value.copy(progress = progress)
            }
            if (outcome == UpdateInstallOutcome.NEEDS_FOREGROUND) {
                pendingUpdateInstall = true
            }
            _updateDownloadState.value = UpdateDownloadState(outcome = outcome)
        }
    }

    /** Called from Activity.onResume: installs a verified APK that finished downloading in background. */
    fun installPendingUpdate(context: Context) {
        if (!pendingUpdateInstall) return
        if (UpdateInstaller.installDownloadedApk(context)) {
            pendingUpdateInstall = false
        }
    }

    fun clearUpdateOutcome() {
        if (_updateDownloadState.value.downloading) return
        _updateDownloadState.value = UpdateDownloadState()
    }
    var initialCategoryForAdd: String? = null
    /** 月历中选中的日期（UTC 零点毫秒），新建页读取一次后清空。 */
    var initialDateForAdd: Long? = null

    fun launchAppTask(block: suspend CoroutineScope.() -> Unit) {
        appScope.launch(block = block)
    }

    internal fun saveEventDisplayMode(eventId: Int, mode: Int, markHomeHintSeen: Boolean = false) {
        // Enter DataStore in click order while keeping the save alive after navigation.
        appScope.launch(Dispatchers.Main.immediate) {
            userPrefs.setDateDeltaDisplayModeForEvent(eventId, mode)
            WidgetUpdater.refreshCountdownWidgets(this@TimeApplication)
            if (markHomeHintSeen) userPrefs.markHomeTimeToggleHintSeen()
        }
    }

    override fun onCreate() {
        super.onCreate()
        lastUiModeNight = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        lastFontScale = resources.configuration.fontScale
        ReminderNotificationDelivery.ensureChannels(this)
        LocaleUtils.syncMirrorFromSystem(this)
        appScope.launch {
            val systemMode = LocaleUtils.platformLocaleMode(this@TimeApplication)
            if (systemMode != null && userPrefs.languageModeFlow.first() != systemMode) {
                userPrefs.setLanguageMode(systemMode)
            }
        }
        // Throttled: a reschedule already completed in the last 15 minutes is not repeated.
        RescheduleAllWorker.enqueueColdStartIfStale(this)
        observeThemeModeChanges()
        // Finish owners from the previous process before any Activity can register a new undo.
        EventImageStore.releasePreviousUndoReferences(this)
        appScope.launch(Dispatchers.IO) {
            // Give system backup restoration a chance to stabilize before running any file sweeps.
            kotlinx.coroutines.delay(5000)
            try {
                EventImageStore.cleanupRegisteredImages(this@TimeApplication) {
                    var retries = 3
                    var result: Set<String>? = null
                    while (retries > 0 && result == null) {
                        try {
                            result = repository.getAllEventsSnapshot().mapNotNullTo(mutableSetOf()) { it.imagePath }
                        } catch (e: Exception) {
                            retries--
                            if (retries == 0) throw e
                            kotlinx.coroutines.delay(1000)
                        }
                    }
                    result ?: emptySet()
                }
            } catch (e: Exception) {
                android.util.Log.e("TimeApplication", "Image cleanup failed safely", e)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val current = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if ((lastUiModeNight != current && lastUiModeNight != -1) || lastFontScale != newConfig.fontScale) {
            refreshWidgetsOnThemeChange()
        }
        lastUiModeNight = current
        lastFontScale = newConfig.fontScale
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
