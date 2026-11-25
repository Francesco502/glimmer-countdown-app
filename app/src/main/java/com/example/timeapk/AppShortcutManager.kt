package com.example.timeapk

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

internal object AppShortcutManager {
    const val ADD_DATE = "com.example.timeapk.action.ADD_DATE"
    const val OPEN_CALENDAR = "com.example.timeapk.action.OPEN_CALENDAR"

    suspend fun observePinnedEvents(app: TimeApplication) = withContext(Dispatchers.IO) {
        val manager = app.getSystemService(ShortcutManager::class.java) ?: return@withContext
        val component = ComponentName(app, MainActivity::class.java)
        combine(app.repository.getAllEvents(), app.userPrefs.pinnedEventIdsFlow) { events, pinned ->
            val active = events.filterNot { it.isArchived }.associateBy { it.id }
            pinned.mapNotNull { id -> active[id]?.let { id to it.title } }.take(2)
        }.distinctUntilChanged().collect { pinned ->
            try {
                val limit = (manager.maxShortcutCountPerActivity - manager.manifestShortcuts.size).coerceIn(0, 2)
                val shortcuts = pinned.take(limit).mapIndexed { rank, (id, title) ->
                    ShortcutInfo.Builder(app, "pinned_event_$id")
                        .setActivity(component)
                        .setRank(rank)
                        .setShortLabel(title.trim().ifEmpty { app.getString(R.string.app_name) })
                        .setIcon(Icon.createWithResource(app, R.drawable.ic_launcher))
                        .setIntent(Intent(Intent.ACTION_VIEW).setComponent(component).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("open_event_id", id)
                        })
                        .build()
                }
                if (!manager.setDynamicShortcuts(shortcuts)) Log.w("AppShortcuts", "Shortcut updates are rate limited")
            } catch (locked: IllegalStateException) {
                Log.w("AppShortcuts", "Unable to update launcher shortcuts", locked)
            }
        }
    }
}
