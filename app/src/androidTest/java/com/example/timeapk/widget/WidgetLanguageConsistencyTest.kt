package com.example.timeapk.widget

import android.content.res.Configuration
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.Event
import com.example.timeapk.data.LANG_EN
import com.example.timeapk.data.LANG_ZH
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.settings.buildWidgetPreviewView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class WidgetLanguageConsistencyTest {
    @Test
    fun languageSwitchMatchesPreviewAndDesktopDespiteOppositeSystemLocale() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as TimeApplication
        val originalLanguage = app.userPrefs.languageModeFlow.first()
        val event = Event(title = "Locale birthday", category = CATEGORY_BIRTHDAY, repeatType = REPEAT_YEARLY,
            date = LocalDate.now().plusDays(12).withYear(2000).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        val id = app.repository.insertEvent(event).toInt()
        try {
            for (mode in listOf(LANG_EN, LANG_ZH, LANG_EN)) {
                app.userPrefs.setLanguageMode(mode)
                val appLocale = if (mode == LANG_EN) Locale.ENGLISH else Locale.SIMPLIFIED_CHINESE
                val systemLocale = if (mode == LANG_EN) Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
                fun context(locale: Locale) = app.createConfigurationContext(
                    Configuration(app.resources.configuration).apply { setLocale(locale) })
                val desktopContext = context(systemLocale)
                val previewContext = context(appLocale)
                val processLocale = Locale.getDefault()
                val desktop = WidgetContentResolver.load(desktopContext, null, WidgetSizeBucket.MEDIUM,
                    WidgetConfig.default(), 180)
                val preview = WidgetContentResolver.load(previewContext, null, WidgetSizeBucket.MEDIUM,
                    WidgetConfig.default(), 180)
                val item = desktop.items.single { it.eventId == id }
                assertEquals(item, preview.items.single { it.eventId == id })
                assertTrue(item.fullValue.contains(if (mode == LANG_EN) "birthday" else "生日"))
                assertEquals(processLocale, Locale.getDefault())
                instrumentation.runOnMainSync {
                    val root = buildWidgetPreviewView(FrameLayout(previewContext), preview)
                    val list = root.findViewById<ListView>(R.id.widget_list)
                    val index = preview.items.indexOfFirst { it.eventId == id }
                    val previewRow = list.adapter.getView(index, null, list)
                    val desktopRow = buildWidgetItemRemoteViews(desktopContext, item, desktop.textStyle,
                        desktop.renderStyle, desktop.widthDp).apply(desktopContext, FrameLayout(desktopContext))
                    assertEquals(
                        desktopRow.findViewById<TextView>(R.id.widget_item_value).text.toString(),
                        previewRow.findViewById<TextView>(R.id.widget_item_value).text.toString())
                    val emptyPreview = buildWidgetPreviewView(FrameLayout(previewContext), preview.copy(items = emptyList()))
                    val emptyDesktop = CountdownAppWidgetProvider.buildWidgetRemoteViews(
                        desktopContext, 62001, WidgetSizeBucket.MEDIUM, WidgetConfig.default(),
                        WidgetThemeSnapshot(false, false), attachRemoteAdapter = false
                    ).apply(desktopContext, FrameLayout(desktopContext))
                    val expectedEmpty = widgetLanguageContext(desktopContext).getString(R.string.home_empty_title)
                    assertEquals(expectedEmpty, emptyPreview.findViewById<TextView>(R.id.widget_empty).text.toString())
                    assertEquals(expectedEmpty, emptyDesktop.findViewById<TextView>(R.id.widget_empty).text.toString())
                }
            }
        } finally {
            app.repository.deleteEvent(event.copy(id = id))
            app.userPrefs.setLanguageMode(originalLanguage)
        }
    }
}
