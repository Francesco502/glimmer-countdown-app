package com.example.timeapk.widget

import android.content.res.Configuration
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class SingleEventWidgetTodayLayoutTest {
    @Test
    fun birthdayTodayFitsSmallWidgetWithLargestConfiguredAndSystemFonts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            listOf(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH).forEach { locale ->
                listOf(1f, 1.6f).forEach { systemFontScale ->
                    val base = instrumentation.targetContext
                    val context = base.createConfigurationContext(Configuration(base.resources.configuration).apply {
                        setLocale(locale)
                        fontScale = systemFontScale
                    })
                    val todayText = context.getString(R.string.widget_birthday_today)
                    val text = SingleEventWidgetText(7, "Birthday", "", todayText, "", "2026-10-20", true)
                    val root = SingleEventWidgetProvider.buildViews(
                        context = context,
                        appWidgetId = 7,
                        text = text,
                        config = WidgetConfig(fontScale = 1.6f),
                        theme = WidgetThemeResolver.resolve(context),
                        widthDp = 110,
                        heightDp = 110
                    ).apply(context, FrameLayout(context))
                    val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 110f, context.resources.displayMetrics).roundToInt()
                    val exact = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
                    root.measure(exact, exact)
                    root.layout(0, 0, size, size)
                    val number = root.findViewById<TextView>(R.id.single_number)
                    val available = number.width - number.compoundPaddingLeft - number.compoundPaddingRight

                    assertEquals(todayText, number.text.toString())
                    assertEquals(1, number.lineCount)
                    assertTrue("$locale at system scale $systemFontScale clips birthday text", number.paint.measureText(todayText) <= available + 0.5f)
                    assertTrue(number.measuredWidth <= (number.parent as View).width)
                    assertEquals(View.GONE, root.findViewById<View>(R.id.single_unit).visibility)
                }
            }
        }
    }
}
