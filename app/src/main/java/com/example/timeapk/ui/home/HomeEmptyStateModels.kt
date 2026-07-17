package com.example.timeapk.ui.home

internal enum class HomeEmptyStateKind {
    FirstEvent,
    NoMatches
}

internal fun resolveHomeEmptyStateKind(isCalendarEmpty: Boolean): HomeEmptyStateKind =
    if (isCalendarEmpty) HomeEmptyStateKind.FirstEvent else HomeEmptyStateKind.NoMatches
