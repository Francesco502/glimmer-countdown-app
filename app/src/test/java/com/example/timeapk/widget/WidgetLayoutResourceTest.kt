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

    @Test
    fun translucentWidgetBackgrounds_useLayeredGlassTreatment() {
        listOf(
            "drawable/widget_background_translucent.xml",
            "drawable-night/widget_background_translucent.xml",
            "drawable/widget_background_translucent_25.xml",
            "drawable-night/widget_background_translucent_25.xml",
            "drawable/widget_background_translucent_50.xml",
            "drawable-night/widget_background_translucent_50.xml"
        ).forEach { relative ->
            val background = readLayoutText(relative)

            assertTrue("$relative should be a layered drawable", background.contains("<layer-list"))
            assertTrue("$relative should include a glass highlight gradient", background.contains("<gradient"))
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
    fun twentyFivePercentGlass_usesMilkyScrimAndSoftHairline() {
        val lightGlass = readLayoutText("drawable/widget_background_translucent_25.xml")
        val darkGlass = readLayoutText("drawable-night/widget_background_translucent_25.xml")

        listOf(lightGlass, darkGlass).forEach { glass ->
            assertTrue(glass.contains("#B3F1F3F0"))
            assertTrue(glass.contains("#1A202124"))
            assertFalse(glass.contains("#42F7F3EA"))
            assertFalse(glass.contains("#59141618"))
            assertFalse(glass.contains("#331F1F1F"))
            assertFalse(glass.contains("#3DEDE8DD"))
        }
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
