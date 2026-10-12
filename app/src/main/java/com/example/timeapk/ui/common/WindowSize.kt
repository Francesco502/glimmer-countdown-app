package com.example.timeapk.ui.common

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 窗口宽度分级，与 Material 窗口尺寸类断点一致：600dp / 840dp。 */
enum class WindowWidthClass { Compact, Medium, Expanded }

fun windowWidthClassFor(width: Dp): WindowWidthClass = when {
    width < 600.dp -> WindowWidthClass.Compact
    width < 840.dp -> WindowWidthClass.Medium
    else -> WindowWidthClass.Expanded
}
