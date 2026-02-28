package com.example.timeapk.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.home.HomeViewModel
import com.example.timeapk.ui.event.EventEntryViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(
                timeApplication(),
                timeApplication().repository,
                timeApplication().userPrefs
            )
        }
        initializer {
            EventEntryViewModel(
                timeApplication(),
                timeApplication().repository
            )
        }
    }
}

fun CreationExtras.timeApplication(): TimeApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TimeApplication)
