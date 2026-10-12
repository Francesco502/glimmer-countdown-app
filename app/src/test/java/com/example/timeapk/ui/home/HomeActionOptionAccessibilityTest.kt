package com.example.timeapk.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeActionOptionAccessibilityTest {
    @Test
    fun sortAndFilterChoicesUseRadioSemanticsAndAccessibleTouchTargets() {
        val source = com.example.timeapk.HomeSourceFiles.combined()
        val grid = source.substringAfter("fun SongActionOptionGrid(")
            .substringBefore("@Composable\nprivate fun SongActionOptionTile(")
        val tile = source.substringAfter("fun SongActionOptionTile(")
            .substringBefore("@Composable\ninternal fun InlineActionIconButton(")

        assertTrue(source.contains("private val HomeOverflowActionItemHeight = 48.dp"))
        assertTrue(grid.contains("Modifier.selectableGroup()"))
        assertTrue(tile.contains(".heightIn(min = HomeOverflowActionItemHeight)"))
        assertTrue(tile.contains(".selectable("))
        assertTrue(tile.contains("selected = spec.selected"))
        assertTrue(tile.contains("role = Role.RadioButton"))
        assertFalse(tile.contains(".clickable("))
    }
}
