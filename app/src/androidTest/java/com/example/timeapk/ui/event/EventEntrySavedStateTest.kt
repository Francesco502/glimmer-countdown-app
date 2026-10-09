package com.example.timeapk.ui.event

import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.Event
import com.example.timeapk.ui.AppViewModelProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile

@RunWith(AndroidJUnit4::class)
class EventEntrySavedStateTest {
    @Test
    fun repeatedSaveRequestsPersistOneRowAndRetryUsesGeneratedId() {
        val app = ApplicationProvider.getApplicationContext<TimeApplication>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var owner: EntryStateOwner? = null
        lateinit var viewModel: EventEntryViewModel
        val title = "E2E-Entry-Save-${System.nanoTime()}"
        try {
            instrumentation.runOnMainSync {
                val createdOwner = EntryStateOwner()
                owner = createdOwner
                viewModel = entryViewModel(createdOwner, app)
                runBlocking { viewModel.prepareForEvent(null) }
                viewModel.updateUiState(viewModel.eventUiState.value.eventDetails.copy(
                    title = title, remindEnabled = false, syncToScheduleEnabled = false
                ))
                viewModel.saveEvent()
                viewModel.saveEvent()
            }
            runBlocking { withTimeout(10_000) { viewModel.saveResult.filterNotNull().first() } }
            val id = viewModel.eventUiState.value.eventDetails.id
            assertTrue(id > 0)
            assertEquals(1, runBlocking { app.repository.getAllEventsSnapshot().count { it.title == title } })

            instrumentation.runOnMainSync {
                viewModel.consumeSaveResult()
                viewModel.saveEvent()
            }
            runBlocking { withTimeout(10_000) { viewModel.saveResult.filterNotNull().first() } }
            assertEquals(id, viewModel.eventUiState.value.eventDetails.id)
            assertEquals(1, runBlocking { app.repository.getAllEventsSnapshot().count { it.title == title } })
        } finally {
            instrumentation.runOnMainSync { owner?.viewModelStore?.clear() }
            runBlocking {
                app.repository.getAllEventsSnapshot().filter { it.title == title }.forEach { app.repository.deleteEvent(it) }
            }
        }
    }

    @Test
    fun newAndEditedDraftsRestoreIntoNewViewModelAfterSavedStateParcelRoundTrip() {
        val app = ApplicationProvider.getApplicationContext<TimeApplication>()
        val existingId = runBlocking {
            app.repository.insertEvent(Event(
                title = "E2E-Entry-Restore-${System.nanoTime()}", date = System.currentTimeMillis(),
                category = CATEGORY_ANNIVERSARY, syncToScheduleEnabled = false
            )).toInt()
        }
        try {
            listOf(null, existingId).forEach { eventId ->
                assertDraftRestoration(app, eventId)
            }
        } finally {
            runBlocking { app.repository.getEvent(existingId)?.let { app.repository.deleteEvent(it) } }
        }
    }

    @Test
    fun draftLargerThanBinderBudgetRestoresCompleteNotesFromPrivateSnapshot() {
        val app = ApplicationProvider.getApplicationContext<TimeApplication>()
        val existingId = runBlocking {
            app.repository.insertEvent(Event(
                title = "E2E-Entry-Large-Restore-${System.nanoTime()}", date = System.currentTimeMillis(),
                category = CATEGORY_ANNIVERSARY, note = "原备注🙂".repeat(100_000), syncToScheduleEnabled = false
            )).toInt()
        }
        try {
            listOf(null, existingId).forEach {
                assertDraftRestoration(app, it, "长备注🙂".repeat(100_000), expectFile = true)
            }
        } finally {
            runBlocking { app.repository.getEvent(existingId)?.let { app.repository.deleteEvent(it) } }
        }
    }

    @Test
    fun missingOrOversizedSnapshotReportsRecoveryFailure() {
        val app = ApplicationProvider.getApplicationContext<TimeApplication>()
        val note = "长备注🙂".repeat(100_000)
        assertDraftRestoration(app, null, note, expectFile = true) { assertTrue(it.delete()) }
        assertDraftRestoration(app, null, note, expectFile = true) { file ->
            RandomAccessFile(file, "rw").use { it.setLength(33L * 1024 * 1024) }
        }
    }

    @Test
    fun downsizedDraftKeepsItsFileOwnershipUntilRestoredEditorLeaves() {
        val app = ApplicationProvider.getApplicationContext<TimeApplication>()
        assertDraftRestoration(app, null, "长备注🙂".repeat(100_000), expectFile = true, shrinkBeforeRestore = true)
    }

    @Test
    fun draftSnapshotRejectsUncontrolledFilePaths() {
        val app = ApplicationProvider.getApplicationContext<TimeApplication>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val handle = SavedStateHandle(mapOf("eventDraft" to Bundle().apply { putString("file", "../outside") }))
            val viewModel = EventEntryViewModel(app, app.repository, app.userPrefs, handle)
            assertTrue(viewModel.draftRecoveryError.value)
            assertFalse(viewModel.eventUiState.value.isEntryValid)
        }
    }

    private fun assertDraftRestoration(
        app: TimeApplication, eventId: Int?, note: String = "Unsaved note", expectFile: Boolean = false,
        shrinkBeforeRestore: Boolean = false,
        damageFile: ((File) -> Unit)? = null
    ) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val firstOwner = EntryStateOwner()
            val first = entryViewModel(firstOwner, app)
            var restoredOwner: EntryStateOwner? = null
            val createdFiles = mutableListOf<File>()
            try {
                runBlocking { first.prepareForEvent(eventId) }
                val initial = first.eventUiState.value.initialEventDetails
                var draft = initial.copy(
                    title = "Recovered anniversary", note = note, category = CATEGORY_ANNIVERSARY,
                    colorHex = "#457080", repeatType = REPEAT_YEARLY, remindEnabled = true,
                    remindDaysBefore = 3, reminderTimeMinutesOfDay = 615, isLunar = true
                )
                first.updateUiState(draft)
                val directory = File(app.filesDir, "editor-drafts")
                val previousFiles = directory.listFiles().orEmpty().map { it.name }.toSet()

                var restoredBundle = parcelSnapshot(firstOwner)
                if (shrinkBeforeRestore) {
                    draft = draft.copy(note = "Shortened note")
                    first.updateUiState(draft)
                    restoredBundle = parcelSnapshot(firstOwner)
                }
                createdFiles += directory.listFiles().orEmpty().filter { it.name !in previousFiles }
                if (expectFile) {
                    assertEquals(1, createdFiles.size)
                    assertTrue(createdFiles.single().length() > 1024 * 1024)
                } else {
                    assertTrue(createdFiles.isEmpty())
                }
                damageFile?.invoke(createdFiles.single())

                // A killed process never clears its VM store. Create a separate
                // registry while keeping the snapshot until restoration is proven.
                val newOwner = EntryStateOwner(restoredBundle)
                restoredOwner = newOwner
                val restored = entryViewModel(newOwner, app)
                runBlocking { restored.prepareForEvent(eventId) }
                if (damageFile == null) {
                    assertEquals(draft, restored.eventUiState.value.eventDetails)
                    assertEquals(initial, restored.eventUiState.value.initialEventDetails)
                    assertTrue(restored.eventUiState.value.hasUnsavedChanges())
                    assertTrue(restored.eventUiState.value.isEntryValid)
                    assertFalse(restored.draftRecoveryError.value)
                } else {
                    assertTrue(restored.draftRecoveryError.value)
                    assertFalse(draft == restored.eventUiState.value.eventDetails)
                }
            } finally {
                restoredOwner?.viewModelStore?.clear()
                firstOwner.viewModelStore.clear()
            }
            assertTrue("Leaving editing removes this draft snapshot", createdFiles.none { it.exists() })
        }
    }

    private fun parcelSnapshot(owner: EntryStateOwner): Bundle {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(owner.save())
            assertTrue("Saved registry must remain below Binder budget", parcel.dataSize() < 128 * 1024)
            parcel.setDataPosition(0)
            parcel.readBundle(EventEntryUiState::class.java.classLoader)!!
        } finally {
            parcel.recycle()
        }
    }

    private fun entryViewModel(owner: EntryStateOwner, app: TimeApplication): EventEntryViewModel {
        val extras = MutableCreationExtras().apply {
            this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] = app
            this[SAVED_STATE_REGISTRY_OWNER_KEY] = owner
            this[VIEW_MODEL_STORE_OWNER_KEY] = owner
        }
        return ViewModelProvider(owner.viewModelStore, AppViewModelProvider.Factory, extras)[EventEntryViewModel::class.java]
    }

    private class EntryStateOwner(restored: Bundle? = null) : SavedStateRegistryOwner, ViewModelStoreOwner {
        override val lifecycle = LifecycleRegistry(this)
        override val viewModelStore = ViewModelStore()
        private val controller = SavedStateRegistryController.create(this)
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

        init {
            controller.performAttach()
            enableSavedStateHandles()
            controller.performRestore(restored)
            lifecycle.currentState = Lifecycle.State.CREATED
        }

        fun save(): Bundle = Bundle().also(controller::performSave)
    }
}
