package com.example.timeapk.widget

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.toEventUiState
import com.example.timeapk.ui.utils.DisplayModes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class WidgetBirthdayRenderingTest {
    private val today = LocalDate.of(2026, 10, 20)

    @Test
    fun chineseBirthdayShrinksToSemanticTextAndKeepsFullAccessibility() {
        val context = localizedContext(Locale.SIMPLIFIED_CHINESE)
        val state = birthday(LocalDate.of(2000, 11, 1))
        val full = render(context, state, maxChars = 40)
        val small = render(context, state, maxChars = 8)

        assertEquals("距下次生日还有 12 天", full.value)
        assertEquals("生日余12天", small.value)
        assertEquals(full.value, small.fullValue)
        assertEquals("Birthday, ${full.value}", small.contentDescription)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val style = WidgetStylePolicy.resolve(WidgetSizeBucket.SMALL, 1.6f)
            val renderStyle = WidgetRenderPolicy.resolve(WidgetConfig.default(), WidgetThemeSnapshot(false, false))
            val view = buildWidgetItemRemoteViews(context, small, style, renderStyle).apply(context, FrameLayout(context))
            assertEquals(small.contentDescription, view.findViewById<View>(R.id.widget_item_root).contentDescription)
            assertTrue(view.findViewById<TextView>(R.id.widget_item_value).text.toString().endsWith("天"))
        }
    }

    @Test
    fun narrowRowsKeepTitleAndCompleteTimeTextAtLargeFontScales() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (locale in listOf(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH)) {
                for (systemScale in listOf(1f, 1.6f, 2f)) {
                    val base = localizedContext(locale)
                    val context = base.createConfigurationContext(Configuration(base.resources.configuration).apply {
                        fontScale = systemScale
                    })
                    for (width in listOf(110, 180, 320)) {
                        val style = WidgetStylePolicy.resolve(WidgetSizeBucket.resolve(width, width), 1.6f)
                        for (origin in listOf(LocalDate.of(2000, 11, 1), LocalDate.of(2000, 10, 20))) {
                            val state = birthday(origin).let {
                                it.copy(event = it.event.copy(title = "Very long birthday title / 一个很长的生日标题"))
                            }
                            val item = WidgetContentResolver.buildRenderedItem(
                                context, state, DisplayModes.PAST_DAYS, DISPLAY_POLICY_AUTO,
                                false, true, style, today
                            )
                            for ((appearance, dark) in listOf(APPEARANCE_SOLID to false,
                                APPEARANCE_TRANSPARENT to false, APPEARANCE_TRANSPARENT to true)) {
                                val renderStyle = WidgetRenderPolicy.resolve(
                                    WidgetConfig(appearancePreset = appearance), WidgetThemeSnapshot(dark, false)
                                )
                                val row = buildWidgetItemRemoteViews(context, item, style, renderStyle, width)
                                    .apply(context, FrameLayout(context))
                                val available = (width * context.resources.displayMetrics.density).toInt() -
                                    2 * context.resources.getDimensionPixelSize(R.dimen.widget_content_padding)
                                row.measure(View.MeasureSpec.makeMeasureSpec(available, View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                                row.layout(0, 0, row.measuredWidth, row.measuredHeight)
                                val title = row.findViewById<TextView>(R.id.widget_item_title)
                                val value = row.findViewById<TextView>(R.id.widget_item_value)
                                assertTrue("Title lost at width=$width scale=$systemScale", title.width > 0)
                                assertTrue(title.layout.getLineEnd(0) > title.layout.getEllipsisCount(0))
                                assertEquals(value.text.length, value.layout.getLineEnd(value.layout.lineCount - 1))
                                for (line in 0 until value.layout.lineCount) {
                                    assertEquals(0, value.layout.getEllipsisCount(line))
                                    assertTrue(value.layout.getLineWidth(line) <= value.width + 1f)
                                }
                                assertEquals(item.contentDescription, row.contentDescription)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun birthdayTodayHasSpecificTextInListAndSingleWidget() {
        listOf(Locale.SIMPLIFIED_CHINESE to "今天生日", Locale.ENGLISH to "Birthday today").forEach { (locale, expected) ->
            val context = localizedContext(locale)
            val state = birthday(LocalDate.of(2000, 10, 20))
            assertEquals(expected, render(context, state, 8).value)
            assertEquals(expected, SingleEventWidgetPolicy.buildText(context, state).number)
        }
    }

    @Test
    fun englishBirthdayUsesSingularPluralAndKeepsCompactMeaning() {
        val context = localizedContext(Locale.ENGLISH)
        assertEquals("1 day until next birthday", render(context, birthday(LocalDate.of(2000, 10, 21)), 50).value)
        assertEquals("2 days until next birthday", render(context, birthday(LocalDate.of(2000, 10, 22)), 50).value)
        assertEquals("Birthday in 12d", render(context, birthday(LocalDate.of(2000, 11, 1)), 20).value)
        assertEquals("In 12d", render(context, birthday(LocalDate.of(2000, 11, 1)), 8).value)
    }

    @Test
    fun followAppPastYmdKeepsPastDirectionAndFullAccessibleText() {
        val context = localizedContext(Locale.SIMPLIFIED_CHINESE)
        val item = render(
            context, birthday(LocalDate.of(2000, 11, 1)), maxChars = 2,
            policy = DISPLAY_POLICY_FOLLOW_APP, preferred = DisplayModes.PAST_YMD
        )

        assertTrue(item.value.startsWith("已"))
        assertTrue(item.value.endsWith("天"))
        assertFalse(item.value.startsWith("-"))
        assertTrue(item.fullValue.contains("年"))
        assertTrue(item.contentDescription.contains(item.fullValue))
    }

    @Test
    fun ordinaryCountdownAndEndedSingleEventRetainDirectionAndUnits() {
        val context = localizedContext(Locale.SIMPLIFIED_CHINESE)
        val upcoming = state(Event(title = "Future", date = epoch(today.plusDays(12)), category = CATEGORY_OTHER))
        val past = state(Event(title = "Past", date = epoch(today.minusDays(12)), category = CATEGORY_OTHER))

        assertEquals("余12天", render(context, upcoming, 1, DISPLAY_POLICY_COUNTDOWN).value)
        assertEquals("已12天", render(context, past, 1, DISPLAY_POLICY_COUNTDOWN).value)
        assertEquals(REPEAT_NONE, past.event.repeatType)
    }

    private fun render(
        context: Context,
        state: EventUiState,
        maxChars: Int,
        policy: Int = DISPLAY_POLICY_AUTO,
        preferred: Int = DisplayModes.PAST_DAYS
    ): WidgetRenderedItem = WidgetContentResolver.buildRenderedItem(
        context = context,
        state = state,
        preferredMode = preferred,
        displayPolicy = policy,
        showMilestone = false,
        showLunarPrefix = true,
        textStyle = WidgetStylePolicy.resolve(WidgetSizeBucket.SMALL, 1.6f).copy(valueMaxEms = maxChars),
        today = today
    )

    private fun localizedContext(locale: Locale): Context {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) })
    }

    private fun birthday(origin: LocalDate): EventUiState = state(
        Event(title = "Birthday", date = epoch(origin), category = CATEGORY_BIRTHDAY, repeatType = REPEAT_YEARLY)
    )

    private fun state(event: Event): EventUiState =
        event.toEventUiState(now = today.atStartOfDay(ZoneOffset.UTC).toInstant(), zoneId = ZoneOffset.UTC)

    private fun epoch(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
