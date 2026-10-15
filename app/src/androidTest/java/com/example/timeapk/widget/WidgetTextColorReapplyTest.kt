package com.example.timeapk.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetTextColorReapplyTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun listEmptyTextReturnsToAutomaticColorsOnReusedView() {
        instrumentation.runOnMainSync {
            listOf(true, false).forEach { isDark ->
                val context = hostContext(isDark)
                val theme = WidgetThemeResolver.resolve(context)
                val config = WidgetConfig.default().copy(backgroundOpacityPercent = 100)
                fun views(contrast: Int) = CountdownAppWidgetProvider.buildWidgetRemoteViews(
                    context, 101, WidgetSizeBucket.MEDIUM, config.copy(contrastMode = contrast),
                    theme, attachRemoteAdapter = false
                )
                val initial = views(if (isDark) CONTRAST_DARK_TEXT else CONTRAST_LIGHT_TEXT)
                val root = initial.apply(context, FrameLayout(context))

                views(CONTRAST_AUTO).reapply(context, root)

                assertEquals(
                    context.getColor(R.color.widget_text_secondary_color),
                    root.findViewById<TextView>(R.id.widget_empty).currentTextColor
                )
            }
        }
    }

    @Test
    fun singleEventTextReturnsToAutomaticColorsOnReusedView() {
        instrumentation.runOnMainSync {
            listOf(true, false).forEach { isDark ->
                val context = hostContext(isDark)
                val configs = listOf(APPEARANCE_SYSTEM, APPEARANCE_SOLID, APPEARANCE_CELADON, APPEARANCE_TRANSPARENT)
                    .map { WidgetConfig.default().copy(appearancePreset = it, backgroundOpacityPercent = 100) } +
                    listOf(25, 50, 75).map { WidgetConfig.default().copy(backgroundOpacityPercent = it) } +
                    WidgetConfig.default().copy(appearancePreset = APPEARANCE_TRANSLUCENT)
                configs.forEach { config ->
                    val forced = config.copy(
                        contrastMode = if (isDark) CONTRAST_DARK_TEXT else CONTRAST_LIGHT_TEXT
                    )
                    val root = singleViews(context, forced).apply(context, FrameLayout(context))

                    singleViews(context, config).reapply(context, root)

                    assertSingleTextColors(root, context, config)
                }
            }
        }
    }

    @Test
    fun automaticGlassColorsFollowHostModeAtApplyTime() {
        // Android 12+ can resolve both colors when the launcher applies/reapplies RemoteViews.
        org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        instrumentation.runOnMainSync {
            val config = WidgetConfig.default()
            listOf(true, false).forEach { hostIsDark ->
                val sourceContext = hostContext(!hostIsDark)
                val context = hostContext(hostIsDark)
                val views = singleViews(sourceContext, config)
                val root = views.apply(context, FrameLayout(context))
                val expected = WidgetRenderPolicy.resolve(config, WidgetThemeResolver.resolve(context))

                assertEquals(
                    expected.primaryTextColor,
                    root.findViewById<TextView>(R.id.single_title).currentTextColor
                )
                assertEquals(
                    expected.accentTextColor,
                    root.findViewById<TextView>(R.id.single_number).currentTextColor
                )
            }
        }
    }

    @Test
    fun manualTextColorsStayFixedAcrossHostNightModes() {
        instrumentation.runOnMainSync {
            listOf(CONTRAST_LIGHT_TEXT, CONTRAST_DARK_TEXT).forEach { contrast ->
                listOf(true, false).forEach { isDark ->
                    val sourceContext = hostContext(!isDark)
                    val context = hostContext(isDark)
                    val config = WidgetConfig.default().copy(contrastMode = contrast)
                    val root = singleViews(sourceContext, config).apply(context, FrameLayout(context))
                    val expected = WidgetRenderPolicy.resolve(config, WidgetThemeResolver.resolve(context))

                    assertEquals(expected.primaryTextColor, root.findViewById<TextView>(R.id.single_title).currentTextColor)
                    assertEquals(expected.accentTextColor, root.findViewById<TextView>(R.id.single_number).currentTextColor)
                }
            }
        }
    }

    private fun singleViews(context: Context, config: WidgetConfig) =
        SingleEventWidgetProvider.buildViews(
            context, 102,
            SingleEventWidgetText(1, "Theme regression", "还有", "3", "天", "10月18日", false),
            config, WidgetThemeResolver.resolve(context), 220, 220
        )

    private fun hostContext(isDark: Boolean): Context {
        val context = instrumentation.targetContext
        val configuration = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        return context.createConfigurationContext(configuration)
    }

    private fun assertSingleTextColors(root: android.view.View, context: Context, config: WidgetConfig) {
        val style = WidgetRenderPolicy.resolve(config, WidgetThemeResolver.resolve(context))
        val primary = if (style.useThemeTextColors) context.getColor(R.color.widget_text_primary_color) else style.primaryTextColor
        val secondary = if (style.useThemeTextColors) context.getColor(R.color.widget_text_secondary_color) else style.secondaryTextColor
        val accent = if (style.useThemeTextColors) context.getColor(R.color.widget_text_accent_color) else style.accentTextColor
        listOf(R.id.single_title, R.id.single_unit).forEach { id ->
            assertEquals(primary, root.findViewById<TextView>(id).currentTextColor)
        }
        listOf(R.id.single_label, R.id.single_date).forEach { id ->
            assertEquals(secondary, root.findViewById<TextView>(id).currentTextColor)
        }
        assertEquals(
            accent,
            root.findViewById<TextView>(R.id.single_number).currentTextColor
        )
    }
}
