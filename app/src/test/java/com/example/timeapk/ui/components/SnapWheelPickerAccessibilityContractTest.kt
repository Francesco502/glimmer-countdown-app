package com.example.timeapk.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SnapWheelPickerAccessibilityContractTest {
    @Test
    fun wheelHas48DpRowsAndAdjustableCurrentValueSemantics() {
        val source = readProjectFile("app/src/main/java/com/example/timeapk/ui/components/SnapWheelPicker.kt")

        assertTrue(source.contains("accessibilityLabel: String"))
        assertTrue(source.contains("itemHeight: Dp = 48.dp"))
        assertTrue(source.contains("itemHeight.coerceAtLeast(48.dp)"))
        assertTrue(source.contains("contentDescription = accessibilityLabel"))
        assertTrue(source.contains("stateDescription = selectedItemLabel"))
        assertTrue(source.contains("progressBarRangeInfo = ProgressBarRangeInfo("))
        assertTrue(source.contains("setProgress { targetValue ->"))
        assertTrue(source.contains("wheelTargetIndex("))
        assertTrue(source.contains("if (items.isNotEmpty())"))
        assertFalse(source.contains("itemHeight: Dp = 36.dp"))
    }

    @Test
    fun everyProductionWheelSuppliesLocalizedAccessibilityLabel() {
        val eventEntry = readProjectFile("app/src/main/java/com/example/timeapk/ui/event/EventEntryScreen.kt")
        val settings = readProjectFile("app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt")
        val datePicker = readProjectFile("app/src/main/java/com/example/timeapk/ui/components/BottomSheetDatePicker.kt")

        assertEquals(4, eventEntry.countOccurrences("SnapWheelPicker("))
        assertEquals(4, eventEntry.countOccurrences("accessibilityLabel = stringResource("))
        assertEquals(2, settings.countOccurrences("SnapWheelPicker("))
        assertEquals(2, settings.countOccurrences("accessibilityLabel = stringResource("))
        assertEquals(3, datePicker.countOccurrences("SnapWheelPicker("))
        assertEquals(3, datePicker.countOccurrences("accessibilityLabel = stringResource("))
    }

    private fun String.countOccurrences(needle: String): Int =
        Regex(Regex.escape(needle)).findAll(this).count()

    private fun readProjectFile(path: String): String {
        val direct = File(path)
        if (direct.exists()) return direct.readText(Charsets.UTF_8)
        val fromApp = File("../$path")
        require(fromApp.exists()) { "Missing project file: $path" }
        return fromApp.readText(Charsets.UTF_8)
    }
}
