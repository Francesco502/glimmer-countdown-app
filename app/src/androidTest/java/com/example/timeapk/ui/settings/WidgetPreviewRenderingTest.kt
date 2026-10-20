package com.example.timeapk.ui.settings

import android.view.View
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import com.example.timeapk.widget.APPEARANCE_SEAL
import com.example.timeapk.widget.DENSITY_COMFORTABLE
import com.example.timeapk.widget.WidgetConfig
import com.example.timeapk.widget.WidgetContentSnapshot
import com.example.timeapk.widget.WidgetRenderPolicy
import com.example.timeapk.widget.WidgetRenderedItem
import com.example.timeapk.widget.WidgetSizeBucket
import com.example.timeapk.widget.WidgetStylePolicy
import com.example.timeapk.widget.WidgetThemeResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class WidgetPreviewRenderingTest {
    @Test
    fun nativePreviewUsesResolvedRowsFontColorsSpacingAndAccessibility() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val config = WidgetConfig(appearancePreset = APPEARANCE_SEAL, densityMode = DENSITY_COMFORTABLE, fontScale = 1.4f)
            val style = WidgetStylePolicy.resolve(WidgetSizeBucket.STANDARD_SQUARE, config.fontScale, config.densityMode)
            val renderStyle = WidgetRenderPolicy.resolve(config, WidgetThemeResolver.resolve(context))
            val item = WidgetRenderedItem(7, "Birthday", "In 12d", "12 days until next birthday")
            val snapshot = WidgetContentSnapshot(listOf(item), style, renderStyle)
            val root = buildWidgetPreviewView(FrameLayout(context), snapshot)
            val list = root.findViewById<ListView>(R.id.widget_list)
            val row = list.adapter.getView(0, null, list)
            val title = row.findViewById<TextView>(R.id.widget_item_title)
            val value = row.findViewById<TextView>(R.id.widget_item_value)

            assertEquals(1, list.adapter.count)
            assertEquals(item.title, title.text.toString())
            assertEquals(item.value, value.text.toString())
            assertEquals(item.contentDescription, row.contentDescription)
            assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, style.titleSp, context.resources.displayMetrics), title.textSize, 0.1f)
            assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, style.valueSp, context.resources.displayMetrics), value.textSize, 0.1f)
            assertEquals(renderStyle.primaryTextColor, title.currentTextColor)
            assertEquals(renderStyle.accentTextColor, value.currentTextColor)
            assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, context.resources.displayMetrics).roundToInt(), list.dividerHeight)
            assertEquals((style.paddingVerticalDp * context.resources.displayMetrics.density).toInt(), row.paddingTop)
            assertFalse(root.hasOnClickListeners())
            assertFalse(row.hasOnClickListeners())
        }
    }

    @Test
    fun emptySnapshotShowsTheActualEmptyViewWithoutFabricatedEvents() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val root = buildWidgetPreviewView(
                FrameLayout(context),
                WidgetContentSnapshot(emptyList(), WidgetStylePolicy.resolve(WidgetSizeBucket.SMALL, 1f))
            )
            val list = root.findViewById<ListView>(R.id.widget_list)
            val empty = root.findViewById<TextView>(R.id.widget_empty)

            assertEquals(0, list.adapter.count)
            assertEquals(View.VISIBLE, empty.visibility)
            assertEquals(View.GONE, list.visibility)
            assertEquals(com.example.timeapk.widget.widgetLanguageContext(context).getString(R.string.home_empty_title), empty.text.toString())
        }
    }
}
