package com.example.timeapk.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class SongUiSourceConsistencyTest {
    @Test
    fun homeScreen_usesSongWrappedControlsAndAnimationSpecs() {
        val source = readSource("ui/home/HomeScreen.kt")

        assertFalse(source.contains("SingleChoiceSegmentedButtonRow"))
        assertFalse(source.contains("SegmentedButton("))
        assertFalse(Regex("(?m)^\\s*FilterChip\\(").containsMatchIn(source))
        assertFalse(source.contains("spring(dampingRatio = 0.7f"))
        assertFalse(source.contains("tween(durationMillis = 350"))
    }

    private fun readSource(relative: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relative")
        if (direct.exists()) {
            return direct.readText(Charsets.UTF_8)
        }
        val fromRoot = File("app/src/main/java/com/example/timeapk/$relative")
        require(fromRoot.exists()) { "Missing source file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }
}
