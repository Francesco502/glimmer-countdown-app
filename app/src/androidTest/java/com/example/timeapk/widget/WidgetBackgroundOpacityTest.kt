package com.example.timeapk.widget

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import com.example.timeapk.ui.settings.buildWidgetPreviewView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class WidgetBackgroundOpacityTest {
    @Test
    fun listAndSingleEventBackgroundPixelsMatchOpacityInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            listOf(false, true).forEach { isDark ->
                val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                val context = instrumentation.targetContext.createConfigurationContext(configuration)
                val theme = WidgetThemeResolver.resolve(context)
                listOf(APPEARANCE_SYSTEM, APPEARANCE_TRANSLUCENT).forEach { appearance ->
                    listOf(BORDER_AUTO, BORDER_ON, BORDER_OFF).forEach { border ->
                        val previousAlpha = IntArray(3) { -1 }
                        (0..100).forEach { opacity ->
                            val config = WidgetConfig.default().copy(
                                appearancePreset = appearance, backgroundOpacityPercent = opacity, borderMode = border
                            )
                            val roots = listOf(
                                CountdownAppWidgetProvider.buildWidgetRemoteViews(
                                    context, 62001, WidgetSizeBucket.STANDARD_SQUARE, config, theme,
                                    attachRemoteAdapter = false
                                ).apply(context, FrameLayout(context)),
                                SingleEventWidgetProvider.buildViews(context, 62002, null, config, theme, 220, 220)
                                    .apply(context, FrameLayout(context)),
                                buildWidgetPreviewView(
                                    FrameLayout(context),
                                    WidgetContentSnapshot(
                                        emptyList(), WidgetStylePolicy.resolve(WidgetSizeBucket.STANDARD_SQUARE, 1f),
                                        WidgetRenderPolicy.resolve(config, theme)
                                    )
                                )
                            )
                            roots.forEachIndexed { index, root ->
                                val background = root.findViewById<ImageView>(R.id.widget_background_image)
                                assertEquals(
                                    if (opacity in 1..99) (opacity * 255.0 / 100).roundToInt() else 255,
                                    background.imageAlpha
                                )
                                assertEquals(if (opacity in 1..99) View.VISIBLE else View.GONE, background.visibility)
                                val content = root.findViewById<View>(R.id.widget_root)
                                assertEquals(1f, content.alpha, 0f)
                                content.visibility = View.INVISIBLE
                                val size = View.MeasureSpec.makeMeasureSpec(160, View.MeasureSpec.EXACTLY)
                                root.measure(size, size)
                                root.layout(0, 0, 160, 160)
                                val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
                                try {
                                    root.draw(Canvas(bitmap))
                                    listOf(40, 80, 120).forEach { y ->
                                        assertPixelAlpha(
                                            "dark=$isDark appearance=$appearance border=$border opacity=$opacity y=$y",
                                            (opacity * 255.0 / 100).roundToInt(),
                                            Color.alpha(bitmap.getPixel(80, y))
                                        )
                                    }
                                    val centerAlpha = Color.alpha(bitmap.getPixel(80, 80))
                                    assertTrue("Each 1% increment must change the rendered background", centerAlpha > previousAlpha[index])
                                    previousAlpha[index] = centerAlpha
                                    assertEquals(1f, root.alpha, 0f)
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun reusedWidgetSwitchesBetweenOpaqueAndImageBackgroundWithoutLeavingALayerBehind() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val theme = WidgetThemeResolver.resolve(context)
            fun views(opacity: Int) = CountdownAppWidgetProvider.buildWidgetRemoteViews(
                context, 62003, WidgetSizeBucket.STANDARD_SQUARE,
                WidgetConfig.default().copy(backgroundOpacityPercent = opacity), theme,
                attachRemoteAdapter = false
            )
            val root = views(100).apply(context, FrameLayout(context))
            root.findViewById<View>(R.id.widget_root).visibility = View.INVISIBLE
            val size = View.MeasureSpec.makeMeasureSpec(160, View.MeasureSpec.EXACTLY)
            listOf(37, 68, 100, 1, 99).forEach { opacity ->
                views(opacity).reapply(context, root)
                root.measure(size, size)
                root.layout(0, 0, 160, 160)
                val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
                try {
                    root.draw(Canvas(bitmap))
                    assertEquals(
                        (opacity * 255.0 / 100).roundToInt(),
                        root.findViewById<ImageView>(R.id.widget_background_image).imageAlpha
                    )
                    assertPixelAlpha(
                        "Reapplied opacity=$opacity",
                        (opacity * 255.0 / 100).roundToInt(),
                        Color.alpha(bitmap.getPixel(80, 80))
                    )
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    private fun assertPixelAlpha(message: String, expected: Int, actual: Int) {
        // GradientDrawable and software rasterization round alpha on older Android versions.
        // Allow at most two 8-bit levels (0.8 percentage points), keeping both endpoints exact.
        val tolerance = if (expected == 0 || expected == 255) 0.0 else 2.0
        assertEquals(message, expected.toDouble(), actual.toDouble(), tolerance)
    }
}
