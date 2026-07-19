package com.example.timeapk.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeActionOptionAccessibilityTest {
    @Test
    fun sortAndFilterChoicesUseRadioSemanticsAndAccessibleTouchTargets() {
        val source = listOf(
            File("src/main/java/com/example/timeapk/ui/home/HomeScreen.kt"),
            File("app/src/main/java/com/example/timeapk/ui/home/HomeScreen.kt")
        ).firstOrNull(File::exists)?.readText(Charsets.UTF_8)
            ?: error("Missing HomeScreen.kt")
        val grid = source.substringAfter("fun SongActionOptionGrid(")
            .substringBefore("@Composable\nprivate fun SongActionOptionTile(")
        val tile = source.substringAfter("fun SongActionOptionTile(")
            .substringBefore("@Composable\nprivate fun SongActionSlipItem(")

        assertTrue(source.contains("private val HomeOverflowActionItemHeight = 48.dp"))
        assertTrue(grid.contains("Modifier.selectableGroup()"))
        assertTrue(tile.contains(".heightIn(min = HomeOverflowActionItemHeight)"))
        assertTrue(tile.contains(".selectable("))
        assertTrue(tile.contains("selected = spec.selected"))
        assertTrue(tile.contains("role = Role.RadioButton"))
        assertFalse(tile.contains(".clickable("))
    }
}
