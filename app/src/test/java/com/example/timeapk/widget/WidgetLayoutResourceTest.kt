package com.example.timeapk.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt

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

    @Test
    fun translucentWidgetBackgrounds_keepOpaqueFillsForRuntimeAlphaIncludingHighlights() {
        val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        listOf("drawable", "drawable-night").forEach { directory ->
            listOf("", "_borderless").forEach { border ->
                listOf(25, 50, 75).forEach { opacity ->
                    val suffix = if (opacity == 75) "" else "_$opacity"
                    val relative = "$directory/widget_background_translucent$suffix$border.xml"
                    val document = parser.parse(readLayoutText(relative).byteInputStream())
                    val shapes = document.getElementsByTagName("shape")
                    // Runtime image alpha controls opacity; fills must not attenuate it again.
                    listOf(0.0, 0.5, 1.0).forEach { position ->
                        var alpha = 0.0
                        for (index in 0 until shapes.length) {
                            val shape = shapes.item(index) as org.w3c.dom.Element
                            val gradient = shape.getElementsByTagName("gradient").item(0) as? org.w3c.dom.Element
                            val solid = shape.getElementsByTagName("solid").item(0) as? org.w3c.dom.Element
                            fun colorAlpha(color: String) = color.substring(1, 3).toInt(16) / 255.0
                            val fillAlpha = if (gradient != null) {
                                val start = colorAlpha(gradient.getAttribute("android:startColor"))
                                val end = colorAlpha(gradient.getAttribute("android:endColor"))
                                start + (end - start) * position
                            } else {
                                colorAlpha(requireNotNull(solid).getAttribute("android:color"))
                            }
                            alpha = fillAlpha + alpha * (1.0 - fillAlpha)
                        }
                        assertEquals(
                            "$relative at $position", 255,
                            (alpha * 255).roundToInt()
                        )
                    }
                }
            }
        }
    }

    @Test
    fun widgetChrome_usesNativeRoundedGlassMetrics() {
        val dimens = readLayoutText("values/dimens.xml")
        val v31Dimens = readLayoutText("values-v31/dimens.xml")

        assertTrue(dimens.contains("<dimen name=\"widget_background_radius\">22dp</dimen>"))
        assertTrue(dimens.contains("<dimen name=\"widget_stroke_width\">0.5dp</dimen>"))
        assertTrue(v31Dimens.contains("@android:dimen/system_app_widget_background_radius"))
    }

    @Test
    fun placedWidgets_areReconfigurableOnAndroid12Launchers() {
        val providerInfo = readLayoutText("xml-v31/countdown_widget_info.xml")

        assertTrue(providerInfo.contains("android:configure=\"com.example.timeapk.widget.WidgetConfigActivity\""))
        assertTrue(providerInfo.contains("android:widgetFeatures=\"reconfigurable\""))
    }

    @Test
    fun shadowWidgetItemLayouts_keepRequiredIdsAndTextProtection() {
        val darkShadowLayout = readLayoutText("layout/widget_countdown_item_shadow_dark.xml")
        val lightShadowLayout = readLayoutText("layout/widget_countdown_item_shadow_light.xml")

        listOf(darkShadowLayout, lightShadowLayout).forEach { layout ->
            assertTrue(layout.contains("android:id=\"@+id/widget_item_root\""))
            assertTrue(layout.contains("android:id=\"@+id/widget_item_title\""))
            assertTrue(layout.contains("android:id=\"@+id/widget_item_value\""))
            assertTrue(layout.contains("android:shadowRadius=\""))
        }
        assertTrue(darkShadowLayout.contains("android:shadowColor=\"#99000000\""))
        assertTrue(lightShadowLayout.contains("android:shadowColor=\"#CCFFFFFF\""))
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
