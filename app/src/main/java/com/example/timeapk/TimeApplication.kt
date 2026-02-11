package com.example.timeapk

import android.app.Application
import com.example.timeapk.data.AppDatabase
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.UserPreferencesRepository

class TimeApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EventRepository(database.eventDao()) }
    val userPrefs by lazy { UserPreferencesRepository(this) }
    var initialCategoryForAdd: String? = null
}
