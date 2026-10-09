package com.example.timeapk.ui.event

import android.app.Application
import android.os.Bundle
import android.util.AtomicFile
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.DefaultEventReminderSettings
import com.example.timeapk.data.Event
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.data.sanitizeRemindDaysBefore
import com.example.timeapk.data.sanitizeReminderTimeMinutesOfDay
import com.example.timeapk.data.sanitizedReminderConfig
import com.example.timeapk.notifications.ScheduleSyncManager
import com.example.timeapk.notifications.cancelMilestoneReminders
import com.example.timeapk.notifications.cancelReminder
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.notifications.syncMilestoneReminderForEvent
import com.example.timeapk.notifications.enqueueMilestoneScheduleRetry
import com.example.timeapk.notifications.eventAfterMilestoneScheduleSyncAttempt
import com.example.timeapk.notifications.requestMilestoneScheduleRetryOnFailure
import com.example.timeapk.notifications.clearPendingMilestoneCalendarOwnership
import com.example.timeapk.ui.home.calendarCleanupRequired
import com.example.timeapk.ui.home.eventAfterCleanupAttempt
import com.example.timeapk.notifications.eventAfterScheduleSyncAttempt
import com.example.timeapk.widget.WidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.Serializable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import java.security.MessageDigest

sealed class SaveEventResult {
    object Success : SaveEventResult()
    data class PartialSuccess(@param:StringRes val messageResId: Int) : SaveEventResult()
    data class Failure(@param:StringRes val messageResId: Int) : SaveEventResult()
}

private val MIN_SUPPORTED_EVENT_DATE_MILLIS: Long = LocalDate.of(1900, 1, 1)
    .atStartOfDay(ZoneOffset.UTC)
    .toInstant()
    .toEpochMilli()

private data class PreparedEventKey(val eventId: Int?)

// 事件卡片默认颜色（与编辑页预设颜色保持一致）
private val DEFAULT_EVENT_COLOR_HEX = listOf(
    "#4A4933",
    "#457080",
    "#5F856B",
    "#AF4E31",
    "#AC8F62",
    "#86351C",
    "#5B8E79",
    "#3A4550",
    "#785B64"
)
private const val TAG = "EventEntryViewModel"
private const val DRAFT_SNAPSHOT_KEY = "eventDraft"
private const val DRAFT_INLINE_LIMIT_BYTES = 64 * 1024
private const val DRAFT_FILE_LIMIT_BYTES = 32 * 1024 * 1024
private val DRAFT_FILE_ID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

internal fun isEventDateValid(dateMillis: Long): Boolean = dateMillis >= MIN_SUPPORTED_EVENT_DATE_MILLIS

internal fun sanitizeRepeatTypeForLunar(isLunar: Boolean, repeatType: String): String {
    if (!isLunar) return repeatType
    return when (repeatType) {
        REPEAT_NONE, REPEAT_YEARLY -> repeatType
        else -> REPEAT_NONE
    }
}

internal fun supportedRepeatTypes(isLunar: Boolean): List<String> {
    return if (isLunar) {
        listOf(REPEAT_NONE, REPEAT_YEARLY)
    } else {
        listOf(
            REPEAT_NONE,
            REPEAT_YEARLY,
            com.example.timeapk.data.REPEAT_HALF_YEARLY,
            com.example.timeapk.data.REPEAT_MONTHLY,
            com.example.timeapk.data.REPEAT_WEEKLY,
            com.example.timeapk.data.REPEAT_DAILY
        )
    }
}

internal fun calendarCleanupHandledExternallyForSave(syncToScheduleEnabled: Boolean): Boolean =
    !syncToScheduleEnabled

internal fun shouldClearMilestoneCalendarAfterSave(
    syncToScheduleEnabled: Boolean,
    repeatType: String
): Boolean = syncToScheduleEnabled && repeatType != REPEAT_YEARLY && repeatType != REPEAT_NONE

internal fun resolvePartialSaveMessageResId(
    hasGenericFailure: Boolean,
    scheduleSyncError: String?
): Int? {
    if (!hasGenericFailure && scheduleSyncError.isNullOrBlank()) {
        return null
    }
    if (!hasGenericFailure && ScheduleSyncManager.isNoWritableCalendarError(scheduleSyncError)) {
        return R.string.save_event_partial_warning_no_writable_calendar
    }
    return R.string.save_event_partial_warning
}

internal fun buildNewEventDetails(
    defaultReminderSettings: DefaultEventReminderSettings,
    initialCategory: String? = null,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): EventDetails {
    val resolvedCategory = initialCategory
        .takeIf { it in listOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER) }
        ?: CATEGORY_OTHER
    return EventDetails(
        date = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
        category = resolvedCategory,
        remindDaysBefore = sanitizeRemindDaysBefore(defaultReminderSettings.daysBefore),
        reminderTimeMinutesOfDay = sanitizeReminderTimeMinutesOfDay(defaultReminderSettings.timeMinutesOfDay),
        remindEnabled = defaultReminderSettings.enabled,
        createdAt = nowMillis
    )
}

class EventEntryViewModel(
    private val application: Application,
    private val repository: EventRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : AndroidViewModel(application) {
    private var draftFileId: String? = null
    private var hasRestoredDraft = false
    private val _draftRecoveryError = MutableStateFlow(false)
    val draftRecoveryError: StateFlow<Boolean> = _draftRecoveryError.asStateFlow()
    private val _eventUiState = MutableStateFlow(restoreDraftSnapshot() ?: EventEntryUiState())
    val eventUiState: StateFlow<EventEntryUiState> = _eventUiState.asStateFlow()
    private var preparedEventKey: PreparedEventKey? = if (hasRestoredDraft && savedStateHandle.contains("preparedEventId")) {
        PreparedEventKey(savedStateHandle.get<Int>("preparedEventId").takeUnless { it == 0 })
    } else null
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    private val _saveResult = MutableStateFlow<SaveEventResult?>(null)
    val saveResult: StateFlow<SaveEventResult?> = _saveResult.asStateFlow()

    init {
        savedStateHandle.setSavedStateProvider(DRAFT_SNAPSHOT_KEY) { saveDraftSnapshot() }
    }

    private fun draftFile(id: String): File {
        require(DRAFT_FILE_ID_PATTERN.matches(id)) { "Invalid editor draft identifier" }
        val root = application.filesDir.canonicalFile
        val directory = File(root, "editor-drafts").canonicalFile
        require(directory.parentFile == root && directory.name == "editor-drafts") { "Editor draft directory must remain private" }
        val file = File(directory, "$id.bin").canonicalFile
        require(file.parentFile == directory && file.name == "$id.bin") { "Editor draft must remain in private storage" }
        return file
    }

    private fun restoreDraftSnapshot(): EventEntryUiState? {
        return try {
            val snapshot = savedStateHandle.get<Bundle>(DRAFT_SNAPSHOT_KEY) ?: return null
            snapshot.getString("file")?.let { id ->
                draftFile(id)
                draftFileId = id
            }
            check(!snapshot.getBoolean("failed")) { "Previous editor state could not be saved" }
            val bytes = snapshot.getByteArray("inline") ?: run {
                val file = draftFile(requireNotNull(draftFileId))
                AtomicFile(file).openRead().use { stream ->
                    check(stream.channel.size() in 1L..DRAFT_FILE_LIMIT_BYTES.toLong()) { "Invalid editor draft size" }
                    stream.readBytes()
                }
            }
            check(bytes.size <= DRAFT_FILE_LIMIT_BYTES) { "Editor draft is too large" }
            val state = ObjectInputStream(ByteArrayInputStream(bytes)).use {
                it.readObject() as EventEntryUiState
            }
            hasRestoredDraft = true
            state
        } catch (error: Exception) {
            Log.w(TAG, "Unable to restore editor draft", error)
            _draftRecoveryError.value = true
            null
        }
    }

    private fun saveDraftSnapshot(): Bundle {
        return try {
            val bytes = object : ByteArrayOutputStream() {
                override fun write(value: Int) {
                    check(count < DRAFT_FILE_LIMIT_BYTES) { "Editor draft exceeds the recovery size limit" }
                    super.write(value)
                }

                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    check(length <= DRAFT_FILE_LIMIT_BYTES - count) { "Editor draft exceeds the recovery size limit" }
                    super.write(bytes, offset, length)
                }
            }.use { buffer ->
                ObjectOutputStream(buffer).use { it.writeObject(_eventUiState.value) }
                buffer.toByteArray()
            }
            check(bytes.size <= DRAFT_FILE_LIMIT_BYTES) { "Editor draft exceeds the recovery size limit" }
            Bundle().apply {
                if (bytes.size <= DRAFT_INLINE_LIMIT_BYTES) {
                    putByteArray("inline", bytes)
                    draftFileId?.let { putString("file", it) }
                } else {
                    val id = draftFileId ?: UUID.randomUUID().toString().also { draftFileId = it }
                    val file = draftFile(id)
                    check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
                    val atomicFile = AtomicFile(file)
                    val stream = atomicFile.startWrite()
                    try {
                        stream.write(bytes)
                        stream.fd.sync()
                        atomicFile.finishWrite(stream)
                    } catch (error: Exception) {
                        atomicFile.failWrite(stream)
                        throw error
                    }
                    check(file.length() == bytes.size.toLong()) { "Editor draft write did not complete" }
                    val digest = MessageDigest.getInstance("SHA-256")
                    atomicFile.openRead().use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var count = input.read(buffer)
                        while (count >= 0) {
                            digest.update(buffer, 0, count)
                            count = input.read(buffer)
                        }
                    }
                    check(digest.digest().contentEquals(MessageDigest.getInstance("SHA-256").digest(bytes))) {
                        "Editor draft write could not be verified"
                    }
                    putString("file", id)
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "Unable to save editor draft", error)
            _draftRecoveryError.value = true
            Bundle().apply {
                putBoolean("failed", true)
                draftFileId?.let { putString("file", it) }
            }
        }
    }

    fun consumeDraftRecoveryError() {
        _draftRecoveryError.value = false
    }

    override fun onCleared() {
        // Rotation retains this VM; process death does not call onCleared.
        // A cleared navigation entry has genuinely finished editing.
        draftFileId?.let { id ->
            runCatching { AtomicFile(draftFile(id)).delete() }
                .onFailure { Log.w(TAG, "Unable to remove finished editor draft", it) }
        }
        super.onCleared()
    }

    private fun markPrepared(key: PreparedEventKey) {
        preparedEventKey = key
        savedStateHandle["preparedEventId"] = key.eventId ?: 0
    }

    fun consumeSaveResult() {
        _saveResult.value = null
    }

    fun updateUiState(eventDetails: EventDetails) {
        if (_isSaving.value || _saveResult.value != null) return
        _eventUiState.update {
            it.copy(eventDetails = eventDetails, isEntryValid = validateInput(eventDetails))
        }
    }

    suspend fun prepareForEvent(eventId: Int?) {
        val requestedKey = PreparedEventKey(eventId.takeUnless { it == 0 })
        if (preparedEventKey == requestedKey) return

        if (eventId != null && eventId != 0) {
            loadExistingEvent(eventId)
            markPrepared(requestedKey)
            return
        }

        val app = application as? TimeApplication
        val initialCategory = app?.initialCategoryForAdd
        if (app != null) {
            app.initialCategoryForAdd = null
        }

        val defaultReminderSettings = try {
            userPreferencesRepository.getDefaultEventReminderSettings()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            DefaultEventReminderSettings()
        }
        val newDetails = buildNewEventDetails(
            defaultReminderSettings = defaultReminderSettings,
            initialCategory = initialCategory
        )
        _eventUiState.update {
            it.copy(
                eventDetails = newDetails,
                initialEventDetails = newDetails,
                isEntryValid = validateInput(newDetails),
                loadError = false
            )
        }
        markPrepared(requestedKey)
    }

    private suspend fun loadExistingEvent(id: Int) {
        val event = repository.getEvent(id)
        if (event != null) {
            val loadedDetails = event.toEventDetails()
            _eventUiState.update {
                it.copy(
                    eventDetails = loadedDetails,
                    initialEventDetails = loadedDetails,
                    isEntryValid = validateInput(loadedDetails),
                    loadError = false
                )
            }
        } else {
            _eventUiState.update { it.copy(loadError = true) }
        }
    }

    fun saveEvent() {
        if (_isSaving.value || _saveResult.value != null) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                _saveResult.value = persistEvent()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "Saved event but a follow-up action failed", error)
                enqueueMilestoneScheduleRetry(application)
                _saveResult.value = SaveEventResult.PartialSuccess(R.string.save_event_partial_warning)
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun persistEvent(): SaveEventResult {
        val rawDetails = _eventUiState.value.eventDetails
        val details = if (rawDetails.id == 0 && rawDetails.colorHex.isNullOrBlank()) {
            // 新建事件且未选择卡片颜色时，在默认颜色中随机选一个
            rawDetails.copy(colorHex = DEFAULT_EVENT_COLOR_HEX.random())
        } else {
            rawDetails
        }
        if (!validateInput(details)) {
            return SaveEventResult.Failure(R.string.save_event_failed)
        }

        val app = application as? TimeApplication
            ?: return SaveEventResult.Failure(R.string.save_event_failed)

        val persistedEvent = try {
            var event = details.toEvent().sanitizedReminderConfig()
            app.database.withTransaction {
                if (event.id != 0) {
                    repository.updateEvent(event)
                } else {
                    val generatedId = repository.insertEvent(event)
                    event = event.copy(id = generatedId.toInt(), scheduleEventId = null)
                }
            }
            event
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return SaveEventResult.Failure(R.string.save_event_failed)
        }

        // Publish the generated ID before external side effects can suspend.
        // Retrying a partial save must update the same database row.
        _eventUiState.update {
            it.copy(eventDetails = persistedEvent.toEventDetails())
        }

        var hasGenericSideEffectFailure = false
        var scheduleSyncError: String? = null

        var updatedEvent = persistedEvent

        try {
            cancelReminder(application, persistedEvent.id)
            if (persistedEvent.remindEnabled) {
                scheduleReminder(application, persistedEvent)
            }
        } catch (t: Exception) {
            Log.w(TAG, "Failed to schedule app reminder for eventId=${persistedEvent.id}", t)
            hasGenericSideEffectFailure = true
        }

        updatedEvent = if (persistedEvent.syncToScheduleEnabled) {
            try {
                val preferredCalendarId = app.userPrefs.scheduleTargetCalendarIdFlow.first()
                val useRRuleSync = app.userPrefs.scheduleUseRRuleSyncFlow.first()
                val syncResult = ScheduleSyncManager.syncReminderSeries(
                    context = application,
                    event = persistedEvent,
                    preferredCalendarId = preferredCalendarId,
                    useRRuleSync = useRRuleSync
                )
                if (syncResult.error != null) {
                    Log.w(
                        TAG,
                        "Calendar sync returned warning for eventId=${persistedEvent.id}: ${syncResult.error}"
                    )
                    scheduleSyncError = syncResult.error
                }
                eventAfterScheduleSyncAttempt(persistedEvent, syncResult)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Exception) {
                Log.w(TAG, "Calendar sync crashed for eventId=${persistedEvent.id}", t)
                scheduleSyncError = "Schedule sync failed"
                eventAfterScheduleSyncAttempt(
                    persistedEvent,
                    ScheduleSyncManager.ScheduleSyncResult(
                        primaryScheduleEventId = null,
                        targetCalendarId = null,
                        lastSyncAt = System.currentTimeMillis(),
                        error = scheduleSyncError
                    )
                )
            }
        } else {
            val cleanup = ScheduleSyncManager.removeManagedCalendarEntries(
                context = application,
                event = persistedEvent,
                repairReason = "manual_event_save_cleanup"
            )
            scheduleSyncError = cleanup.message
            if (calendarCleanupRequired(persistedEvent) || !cleanup.isSuccess) {
                eventAfterCleanupAttempt(
                    event = persistedEvent,
                    result = cleanup,
                    nowMillis = System.currentTimeMillis()
                )
            } else {
                // No provider attempt occurred, so preserve the previous sync timestamp.
                persistedEvent
            }
        }

        if (updatedEvent != persistedEvent) {
            try {
                repository.updateEvent(updatedEvent)
            } catch (t: Exception) {
                Log.w(TAG, "Failed to persist sync status for eventId=${updatedEvent.id}", t)
                hasGenericSideEffectFailure = true
            }
        }

        try {
            val milestoneResult = syncMilestoneReminderForEvent(
                application = application,
                event = updatedEvent,
                calendarCleanupHandledExternally = calendarCleanupHandledExternallyForSave(
                    updatedEvent.syncToScheduleEnabled
                )
            )
            val milestoneUpdatedEvent = eventAfterMilestoneScheduleSyncAttempt(
                updatedEvent,
                milestoneResult
            )
            if (milestoneUpdatedEvent != updatedEvent) {
                updatedEvent = milestoneUpdatedEvent
                try {
                    repository.updateEvent(updatedEvent)
                } catch (t: Exception) {
                    Log.w(TAG, "Failed to persist milestone schedule status for eventId=${updatedEvent.id}", t)
                    hasGenericSideEffectFailure = true
                }
            }
            scheduleSyncError = updatedEvent.lastScheduleSyncError
            requestMilestoneScheduleRetryOnFailure(milestoneResult?.error) {
                enqueueMilestoneScheduleRetry(application)
            }
            if (
                shouldClearMilestoneCalendarAfterSave(
                    updatedEvent.syncToScheduleEnabled,
                    updatedEvent.repeatType
                )
            ) {
                cancelMilestoneReminders(application, updatedEvent.id)
                val cleanup = clearPendingMilestoneCalendarOwnership(
                    application,
                    updatedEvent.id
                )
                if (!cleanup.isSuccess) {
                    val cleanupError = cleanup.message ?: "Calendar cleanup failed"
                    updatedEvent = eventAfterMilestoneScheduleSyncAttempt(
                        updatedEvent,
                        ScheduleSyncManager.MilestoneScheduleSyncResult(
                            scheduleEventId = null,
                            targetCalendarId = updatedEvent.targetCalendarId,
                            lastSyncAt = System.currentTimeMillis(),
                            error = cleanupError
                        )
                    )
                    scheduleSyncError = updatedEvent.lastScheduleSyncError
                    enqueueMilestoneScheduleRetry(application)
                    try {
                        repository.updateEvent(updatedEvent)
                    } catch (t: Exception) {
                        Log.w(TAG, "Failed to persist milestone cleanup status for eventId=${updatedEvent.id}", t)
                        hasGenericSideEffectFailure = true
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Exception) {
            Log.w(TAG, "Failed to sync milestone reminders for eventId=${updatedEvent.id}", t)
            hasGenericSideEffectFailure = true
            enqueueMilestoneScheduleRetry(application)
        }

        try {
            WidgetUpdater.refreshCountdownWidgets(application)
        } catch (t: Exception) {
            Log.w(TAG, "Failed to refresh widgets after saving eventId=${updatedEvent.id}", t)
            hasGenericSideEffectFailure = true
        }

        val savedDetails = updatedEvent.toEventDetails()
        _eventUiState.update {
            it.copy(
                eventDetails = savedDetails,
                initialEventDetails = savedDetails,
                isEntryValid = validateInput(savedDetails),
                loadError = false
            )
        }

        val partialMessageResId = resolvePartialSaveMessageResId(
            hasGenericFailure = hasGenericSideEffectFailure,
            scheduleSyncError = scheduleSyncError
        )

        return if (partialMessageResId != null) {
            Log.w(
                TAG,
                "Saved eventId=${updatedEvent.id} with partial side-effect failure. hasGenericFailure=$hasGenericSideEffectFailure, scheduleSyncError=$scheduleSyncError"
            )
            SaveEventResult.PartialSuccess(partialMessageResId)
        } else {
            SaveEventResult.Success
        }
    }

    private fun validateInput(uiState: EventDetails = _eventUiState.value.eventDetails): Boolean {
        return uiState.title.isNotBlank() && isEventDateValid(uiState.date)
    }
}

private fun EventDetails.hasSameEditableContent(other: EventDetails): Boolean {
    return title == other.title &&
        date == other.date &&
        category == other.category &&
        note == other.note &&
        colorHex == other.colorHex &&
        repeatType == other.repeatType &&
        remindDaysBefore == other.remindDaysBefore &&
        reminderTimeMinutesOfDay == other.reminderTimeMinutesOfDay &&
        remindEnabled == other.remindEnabled &&
        syncToScheduleEnabled == other.syncToScheduleEnabled &&
        isLunar == other.isLunar
}

data class EventEntryUiState(
    val eventDetails: EventDetails = EventDetails(),
    val isEntryValid: Boolean = false,
    val loadError: Boolean = false,
    val initialEventDetails: EventDetails = eventDetails
) : Serializable {
    fun hasUnsavedChanges(): Boolean = !eventDetails.hasSameEditableContent(initialEventDetails)
}

data class EventDetails(
    val id: Int = 0,
    val title: String = "",
    val date: Long = System.currentTimeMillis(),
    val category: String = CATEGORY_OTHER,
    val note: String = "",
    val colorHex: String? = null,
    val repeatType: String = REPEAT_NONE,
    val remindDaysBefore: Int = 0,
    val reminderTimeMinutesOfDay: Int = 480,
    val remindEnabled: Boolean = false,
    val syncToScheduleEnabled: Boolean = false,
    val scheduleEventId: Long? = null,
    val targetCalendarId: Long? = null,
    val lastScheduleSyncAt: Long? = null,
    val lastScheduleSyncError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isLunar: Boolean = false
) : Serializable

fun EventDetails.toEvent(): Event = Event(
    id = id,
    title = title,
    date = date,
    category = category.takeIf { it in listOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER) }
        ?: CATEGORY_OTHER,
    note = note,
    colorHex = colorHex,
    repeatType = sanitizeRepeatTypeForLunar(isLunar = isLunar, repeatType = repeatType),
    remindDaysBefore = remindDaysBefore,
    reminderTimeMinutesOfDay = reminderTimeMinutesOfDay,
    remindEnabled = remindEnabled,
    syncToScheduleEnabled = syncToScheduleEnabled,
    scheduleEventId = scheduleEventId,
    targetCalendarId = targetCalendarId,
    lastScheduleSyncAt = lastScheduleSyncAt,
    lastScheduleSyncError = lastScheduleSyncError,
    createdAt = createdAt,
    isLunar = isLunar
)

fun Event.toEventDetails(): EventDetails = EventDetails(
    id = id,
    title = title,
    date = date,
    category = category,
    note = note,
    colorHex = colorHex,
    repeatType = sanitizeRepeatTypeForLunar(isLunar = isLunar, repeatType = repeatType),
    remindDaysBefore = remindDaysBefore,
    reminderTimeMinutesOfDay = reminderTimeMinutesOfDay,
    remindEnabled = remindEnabled,
    syncToScheduleEnabled = syncToScheduleEnabled,
    scheduleEventId = scheduleEventId,
    targetCalendarId = targetCalendarId,
    lastScheduleSyncAt = lastScheduleSyncAt,
    lastScheduleSyncError = lastScheduleSyncError,
    createdAt = createdAt,
    isLunar = isLunar
)
