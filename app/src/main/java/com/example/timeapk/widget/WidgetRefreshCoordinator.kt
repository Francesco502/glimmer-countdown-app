package com.example.timeapk.widget

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes every widget refresh in this process so an older refresh cannot finish after a newer one.
 * The refresh body reads the current theme, widget options, and persisted widget configuration only
 * after it acquires this lock. Consequently, launch scheduling may vary without applying a stale
 * snapshot after a more recent one.
 */
internal object WidgetRefreshCoordinator {
    private val refreshMutex = Mutex()

    suspend fun <T> runLatestSnapshot(block: suspend () -> T): T =
        refreshMutex.withLock { block() }
}
