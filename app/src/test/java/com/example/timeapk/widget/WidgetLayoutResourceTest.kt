package com.example.timeapk.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WidgetLayoutResourceTest {
    @Test
    fun widgetEmptyState_isCenteredWithinWidgetCanvas() {
        val defaultLayout = readLayoutText("layout/widget_countdown.xml")
        val v31Layout = readLayoutText("layout-v31/widget_countdown.xml")

        listOf(defaultLayout, v31Layout).forEach { layout ->
            val emptyBlock = layout.substringAfter("android:id=\"@+id/widget_empty\"")
                .substringBefore("</TextView>")

            assertTrue(emptyBlock.contains("android:layout_height=\"match_parent\""))
            assertTrue(emptyBlock.contains("android:gravity=\"center\""))
            assertFalse(emptyBlock.contains("android:layout_marginTop=\"8dp\""))
        }
    }

    private fun readLayoutText(relative: String): String {
        val direct = File("src/main/res/$relative")
        if (direct.exists()) {
            return direct.readText(Charsets.UTF_8)
        }
        val fromRoot = File("app/src/main/res/$relative")
        require(fromRoot.exists()) { "Missing layout file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }
}
