package com.example.timeapk.widget

import androidx.lifecycle.SavedStateHandle
import com.example.timeapk.data.Event
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleEventWidgetConfigViewModelTest {
    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun cancelTasks() = scopes.forEach { it.cancel() }

    private fun model(
        handle: SavedStateHandle = SavedStateHandle(),
        load: suspend () -> Int = { 9 },
        events: Flow<List<Event>> = MutableStateFlow(emptyList()),
        persist: suspend (Int) -> Unit = {},
        refresh: suspend () -> Unit = {}
    ): SingleEventWidgetConfigViewModel {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        scopes += scope
        return SingleEventWidgetConfigViewModel(handle, load, events, persist, refresh, scope)
    }

    @Test
    fun loadingSelectionPreventsDefaultOverwriteAndEarlyInteraction() = runBlocking {
        val stored = CompletableDeferred<Int>()
        var writes = 0
        val model = model(load = { stored.await() }, persist = { writes++ })

        model.select(3)
        model.save()
        assertFalse(model.ready)
        assertFalse(model.canSave)
        assertEquals(0, writes)

        stored.complete(12)
        assertTrue(model.canSave)
        assertEquals(12, model.selection)
    }

    @Test
    fun eventLoadingAndReadFailuresRequireSuccessfulRetry() = runBlocking {
        val events = CompletableDeferred<List<Event>>()
        var reads = 0
        val model = model(
            load = {
                reads++
                if (reads == 1) error("read failed")
                12
            },
            events = flow { emit(events.await()) }
        )

        assertTrue(model.loadFailed)
        assertFalse(model.canSave)
        model.retryLoad()
        assertFalse(model.loadFailed)
        assertFalse(model.canSave)
        events.complete(emptyList())
        assertTrue(model.canSave)
        assertEquals(12, model.selection)
        assertEquals(2, reads)
    }

    @Test
    fun eventReadFailureCanRetryWithoutDiscardingTheSelectedDraft() {
        var attempts = 0
        val model = model(events = flow {
            attempts++
            emit(emptyList())
            if (attempts == 1) error("event read failed")
        })
        assertTrue(model.loadFailed)
        assertFalse(model.canSave)
        model.retryLoad()
        model.select(3)
        model.retryLoad()
        assertTrue(model.canSave)
        assertEquals(3, model.selection)
    }

    @Test
    fun writeFailureRetainsDraftAndDoesNotRefreshUntilPersisted() {
        var writes = 0
        var refreshes = 0
        val model = model(
            persist = { writes++; if (writes == 1) error("write failed") },
            refresh = { refreshes++ }
        )
        model.select(3)
        model.save()

        assertEquals(SingleEventWidgetSaveFailure.PERSIST, model.saveFailure)
        assertEquals(3, model.selection)
        assertEquals(0, refreshes)
        assertTrue(model.canSave)
        model.save()
        assertEquals(2, writes)
        assertEquals(1, refreshes)
        assertTrue(model.saved)
    }

    @Test
    fun refreshFailureRetryNeverWritesTheAlreadySavedSelectionAgain() {
        var writes = 0
        var refreshes = 0
        val model = model(
            persist = { writes++ },
            refresh = { refreshes++; if (refreshes == 1) error("refresh failed") }
        )
        model.select(3)
        model.save()

        assertEquals(SingleEventWidgetSaveFailure.REFRESH, model.saveFailure)
        assertEquals(1, writes)
        model.save()
        assertEquals(1, writes)
        assertEquals(2, refreshes)
        assertTrue(model.saved)
    }

    @Test
    fun reselectionAfterRefreshFailurePersistsTheNewChoice() {
        val written = mutableListOf<Int>()
        var refreshes = 0
        val model = model(
            persist = { written += it },
            refresh = { refreshes++; if (refreshes == 1) error("refresh failed") }
        )
        model.select(3)
        model.save()
        model.select(7)
        assertNull(model.saveFailure)
        model.save()

        assertEquals(listOf(3, 7), written)
        assertTrue(model.saved)
    }

    @Test
    fun duplicateSaveAndSelectionChangesAreBlockedDuringPersist() = runBlocking {
        val persisted = CompletableDeferred<Unit>()
        var writes = 0
        val model = model(persist = { writes++; persisted.await() })
        model.select(3)
        model.save()
        model.save()
        model.select(7)

        assertTrue(model.saving)
        assertEquals(1, writes)
        assertEquals(3, model.selection)
        persisted.complete(Unit)
        assertFalse(model.saving)
        assertTrue(model.saved)
        model.save()
        assertEquals(1, writes)
    }

    @Test
    fun restoredDraftKeepsItsSelectionAndPersistedRefreshMarker() {
        val handle = SavedStateHandle()
        var reads = 0
        var writes = 0
        val original = model(
            handle = handle,
            load = { reads++; 9 },
            persist = { writes++ },
            refresh = { error("refresh failed") }
        )
        original.select(3)
        original.save()
        val restored = model(
            handle = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }),
            load = { reads++; 9 },
            persist = { writes++ }
        )
        assertEquals(3, restored.selection)
        restored.save()

        assertEquals(1, reads)
        assertEquals(1, writes)
        assertTrue(restored.saved)
    }

    @Test
    fun cancellationIsNotReportedAsAPersistFailure() {
        val model = model(persist = { CompletableDeferred<Unit>().await() })
        model.select(3)
        model.save()
        assertTrue(model.saving)
        scopes.last().cancel()

        assertFalse(model.saving)
        assertNull(model.saveFailure)
        assertFalse(model.saved)
    }
}
