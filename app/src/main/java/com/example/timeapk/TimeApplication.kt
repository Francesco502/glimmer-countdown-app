package com.example.timeapk

import android.app.Application
import com.example.timeapk.data.AppDatabase
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.update.GitHubReleaseUpdateChecker
import com.example.timeapk.update.UpdateChecker

class TimeApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EventRepository(database.eventDao()) }
    val userPrefs by lazy { UserPreferencesRepository(this) }
    val updateChecker: UpdateChecker by lazy { GitHubReleaseUpdateChecker() }
    var initialCategoryForAdd: String? = null
}
