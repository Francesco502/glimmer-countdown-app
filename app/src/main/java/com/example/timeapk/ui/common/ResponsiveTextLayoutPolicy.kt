package com.example.timeapk.ui.common

private const val LargeTextLayoutBreakpoint = 1.3f

internal fun useLargeTextLayout(fontScale: Float): Boolean =
    fontScale >= LargeTextLayoutBreakpoint

internal fun homeCardTitleMaxLines(fontScale: Float): Int =
    if (useLargeTextLayout(fontScale)) 1 else 2
