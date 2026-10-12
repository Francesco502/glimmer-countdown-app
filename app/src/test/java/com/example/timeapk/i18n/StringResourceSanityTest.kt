package com.example.timeapk.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StringResourceSanityTest {

    @Test
    fun resourceKeys_areAlignedAcrossDefaultZhAndEn() {
        val defaultKeys = extractKeys(readResourceText("values/strings.xml"))
        val zhKeys = extractKeys(readResourceText("values-zh/strings.xml"))
        val enKeys = extractKeys(readResourceText("values-en/strings.xml"))
        val defaultPluralKeys = extractPluralKeys(readResourceText("values/strings.xml"))
        val zhPluralKeys = extractPluralKeys(readResourceText("values-zh/strings.xml"))
        val enPluralKeys = extractPluralKeys(readResourceText("values-en/strings.xml"))

        assertEquals(enKeys, defaultKeys)
        assertEquals(enKeys, zhKeys)
        assertEquals(enPluralKeys, defaultPluralKeys)
        assertEquals(enPluralKeys, zhPluralKeys)
    }

    @Test
    fun englishPack_doesNotContainChineseCharacters() {
        val en = readResourceText("values-en/strings.xml")
        val hasHan = Regex("\\p{IsHan}").containsMatchIn(en)
        assertFalse("English resources contain Chinese text", hasHan)
    }

    @Test
    fun englishLunarDate_usesAVisibleSeparatorWithoutPlaceholderPunctuation() {
        val en = readResourceText("values-en/strings.xml")
        val lunarFormat = Regex(
            "<string name=\\\"lunar_date_full_format\\\">(.*?)</string>"
        ).find(en)?.groupValues?.get(1).orEmpty()

        assertTrue("English lunar format must exist", lunarFormat.isNotBlank())
        assertTrue("English lunar format must use a visible separator", lunarFormat.contains(" · "))
        assertTrue("Lunar month must wrap as one phrase", lunarFormat.contains("Lunar&#160;month"))
        assertFalse("English lunar format must not show a literal question mark", lunarFormat.contains('?'))
    }

    @Test
    fun englishCalendarLunarWrapper_doesNotRepeatThePrefixFromTheFormattedValue() {
        val en = readResourceText("values-en/strings.xml")
        val calendarWrapper = Regex(
            "<string name=\\\"calendar_selected_date_lunar\\\">(.*?)</string>"
        ).find(en)?.groupValues?.get(1).orEmpty()

        assertEquals(
            "The full English lunar value already names the calendar system",
            "%1\$s",
            calendarWrapper
        )
    }

    @Test
    fun zhPacks_doNotContainCommonMojibakeMarkers() {
        val suspicious = listOf(
            "\u951b",
            "\u9286",
            "\u9225",
            "\uFFFD",
            "\u95C7\u20AC",
            "\u9359"
        )
        val defaultText = readResourceText("values/strings.xml")
        val zhText = readResourceText("values-zh/strings.xml")

        suspicious.forEach { token ->
            assertFalse("Default strings contain mojibake token: $token", defaultText.contains(token))
            assertFalse("zh strings contain mojibake token: $token", zhText.contains(token))
        }
    }

    @Test
    fun releaseResources_doNotKeep315UnusedOrQuantityStringDebt() {
        val defaultText = readResourceText("values/strings.xml")
        val zhText = readResourceText("values-zh/strings.xml")
        val enText = readResourceText("values-en/strings.xml")

        listOf(defaultText, zhText, enText).forEach { xml ->
            assertFalse(xml.contains("reminder_and_calendar_title"))
            assertFalse(xml.contains("field_card_color"))
            assertFalse(xml.contains("cd_custom_event_color"))
            assertFalse(xml.contains("button_reminder_calendar"))
            assertFalse(xml.contains("<string name=\"cd_delete\""))
            assertFalse(xml.contains("month_highlights_"))
            assertFalse(xml.contains("<string name=\"home_timeline_count_format\""))
            assertTrue(xml.contains("<plurals name=\"home_timeline_count_format\">"))
        }
    }

    @Test
    fun zhFunctionalCopyIsPlainWhileDisplayCopyKeepsSongTone() {
        val defaultText = readResourceText("values/strings.xml")
        val zhText = readResourceText("values-zh/strings.xml")
        val enText = readResourceText("values-en/strings.xml")

        // 4.2 review decision: buttons and prompts say plainly what they do...
        listOf(defaultText, zhText).forEach { text ->
            assertTrue(text.contains("<string name=\"button_save_event\">保存</string>"))
            assertTrue(text.contains("<string name=\"button_delete\">删除</string>"))
            assertTrue(text.contains("<string name=\"share_send_image\">分享</string>"))
            assertTrue(text.contains("<string name=\"permission_dialog_button_open_settings\">去设置</string>"))
            assertFalse(text.contains(">落笔<"))
            assertFalse(text.contains(">唤起分享<"))
            assertFalse(text.contains(">移去<"))
            assertFalse(text.contains("探寻新章"))
            assertFalse(text.contains("先定事件气质"))
            assertFalse(text.contains("以纸笺行书写"))
            // ...while display surfaces keep the Song voice.
            assertTrue(text.contains("<string name=\"days_today_label\">正当此时</string>"))
            assertTrue(text.contains("<string name=\"home_empty_title\">空空如也，待录佳期</string>"))
            assertTrue(text.contains("<string name=\"share_card_title\">分享笺</string>"))
        }
        assertTrue(enText.contains("<string name=\"button_delete\">Delete</string>"))
        assertFalse(enText.contains("Choose the event tone first"))
        assertTrue(enText.contains("<string name=\"milestone_label_prefix\">Next milestone: %1\$s</string>"))
        assertTrue(enText.contains("<string name=\"milestone_7\">Day 7</string>"))
    }

    @Test
    fun milestoneLabelCopyClarifiesNextMilestoneTarget() {
        val defaultText = readResourceText("values/strings.xml")
        val zhText = readResourceText("values-zh/strings.xml")
        val enText = readResourceText("values-en/strings.xml")

        listOf(defaultText, zhText).forEach { text ->
            assertFalse(text.contains("<string name=\"milestone_label_prefix\">下一个：%1\$s</string>"))
            assertTrue(text.contains("<string name=\"milestone_label_prefix\">下一节点：%1\$s</string>"))
            assertTrue(text.contains("<string name=\"milestone_7\">第 7 天</string>"))
        }
        assertFalse(enText.contains("<string name=\"milestone_label_prefix\">Next: %1\$s</string>"))
        assertTrue(enText.contains("<string name=\"milestone_label_prefix\">Next milestone: %1\$s</string>"))
        assertTrue(enText.contains("<string name=\"milestone_7\">Day 7</string>"))
    }

    @Test
    fun scheduleCalendarWarningClarifiesMissingDeviceCalendar() {
        val defaultText = readResourceText("values/strings.xml")
        val zhText = readResourceText("values-zh/strings.xml")
        val enText = readResourceText("values-en/strings.xml")

        listOf(defaultText, zhText).forEach { xml ->
            assertTrue(xml.contains("<string name=\"settings_schedule_calendar_no_writable\">手机系统日历里暂无可写日历；先在系统日历中添加账户或日历。</string>"))
            assertTrue(xml.contains("<string name=\"save_event_partial_warning_no_writable_calendar\">已保存；手机系统日历里暂无可写日历，未写入系统日程。</string>"))
            assertTrue(xml.contains("<string name=\"reminder_status_no_writable_calendar\">手机系统日历里暂无可写日历</string>"))
        }
        assertTrue(enText.contains("<string name=\"settings_schedule_calendar_no_writable\">No writable device calendar is available. Add an account or calendar in the system Calendar app.</string>"))
        assertTrue(enText.contains("<string name=\"save_event_partial_warning_no_writable_calendar\">Committed; no writable device calendar, so sync paused.</string>"))
        assertTrue(enText.contains("<string name=\"reminder_status_no_writable_calendar\">No writable device calendar; app reminder is still available</string>"))
    }

    @Test
    fun widgetSizeCopyClarifiesPreviewNotLauncherResize() {
        val defaultText = readResourceText("values/strings.xml")
        val zhText = readResourceText("values-zh/strings.xml")
        val enText = readResourceText("values-en/strings.xml")

        // 4.2 merged width/height into a single preview-size choice with the same caveat.
        listOf(defaultText, zhText).forEach { xml ->
            assertTrue(xml.contains("<string name=\"widget_config_preview_size\">预览尺寸</string>"))
            assertTrue(xml.contains("只影响上方预览的比例；桌面上的实际大小请长按小组件拖动边框调整。"))
            assertFalse(xml.contains("name=\"widget_config_width_cells\""))
        }
        assertTrue(enText.contains("<string name=\"widget_config_preview_size\">Preview size</string>"))
        assertTrue(enText.contains("Resize the real widget by long-pressing it on the home screen."))
    }

    @Test
    fun detailShareResourcesAndProviderPathAreReadyForImageSharing() {
        val requiredStrings = listOf(
            "button_share",
            "share_card_title",
            "share_save_image",
            "share_send_image",
            "share_image_saved",
            "share_image_failed",
            "share_chooser_title"
        )
        val defaultText = readResourceText("values/strings.xml")
        val zhText = readResourceText("values-zh/strings.xml")
        val enText = readResourceText("values-en/strings.xml")

        requiredStrings.forEach { key ->
            listOf(defaultText, zhText, enText).forEach { xml ->
                assertTrue("Missing share string: $key", xml.contains("<string name=\"$key\">"))
            }
        }

        val filePaths = readResourceText("xml/file_paths.xml")
        assertTrue(filePaths.contains("<cache-path name=\"share_images\" path=\"share/\""))
        assertTrue(filePaths.contains("<files-path name=\"apk\" path=\"updates/\""))
    }

    @Test
    fun birthdayDetailResourcesAddAgeSpacingAndDescriptors() {
        val defaultText = readResourceText("values/strings.xml")
        val zhText = readResourceText("values-zh/strings.xml")
        val enText = readResourceText("values-en/strings.xml")
        val requiredStrings = listOf(
            "detail_zodiac_value_format",
            "detail_constellation_value_format",
            "zodiac_branch_horse",
            "constellation_element_water"
        )

        listOf(defaultText, zhText).forEach { xml ->
            assertTrue(xml.contains("<string name=\"detail_birthday_age_format_ymd\">%1\$d岁 %2\$d月 %3\$d日</string>"))
        }
        assertTrue(enText.contains("<string name=\"detail_birthday_age_format_ymd\">%1\$d y %2\$d mo %3\$d d</string>"))
        requiredStrings.forEach { key ->
            listOf(defaultText, zhText, enText).forEach { xml ->
                assertTrue("Missing birthday detail string: $key", xml.contains("<string name=\"$key\">"))
            }
        }
        assertTrue(defaultText.contains("<string name=\"zodiac_branch_horse\">午马</string>"))
        assertTrue(zhText.contains("<string name=\"zodiac_branch_horse\">午马</string>"))
        listOf(defaultText, zhText).forEach { xml ->
            assertTrue(xml.contains("<string name=\"detail_zodiac_value_format\">%1\$s %2\$s</string>"))
            assertFalse(xml.contains("<string name=\"zodiac_yang\">"))
            assertFalse(xml.contains("<string name=\"zodiac_yin\">"))
        }
        assertTrue(enText.contains("<string name=\"detail_zodiac_value_format\">%1\$s %2\$s</string>"))
        assertFalse(enText.contains("<string name=\"zodiac_yang\">"))
        assertFalse(enText.contains("<string name=\"zodiac_yin\">"))
        assertTrue(defaultText.contains("<string name=\"constellation_element_water\">水象</string>"))
        assertTrue(zhText.contains("<string name=\"constellation_element_water\">水象</string>"))
        listOf(defaultText, zhText).forEach { xml ->
            assertTrue(xml.contains("<string name=\"detail_constellation_value_format\">%1\$s %2\$s</string>"))
            assertFalse(xml.contains("<string name=\"constellation_ruler_neptune\">"))
        }
        assertTrue(enText.contains("<string name=\"detail_constellation_value_format\">%1\$s %2\$s</string>"))
        assertFalse(enText.contains("<string name=\"constellation_ruler_neptune\">"))
    }

    private fun readResourceText(relative: String): String {
        val direct = File("src/main/res/$relative")
        if (direct.exists()) {
            return direct.readText(Charsets.UTF_8)
        }
        val fromRoot = File("app/src/main/res/$relative")
        require(fromRoot.exists()) { "Missing resource file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }

    private fun extractKeys(xml: String): Set<String> {
        return Regex("<string name=\"([^\"]+)\">").findAll(xml).map { it.groupValues[1] }.toSet()
    }

    private fun extractPluralKeys(xml: String): Set<String> {
        return Regex("<plurals name=\"([^\"]+)\">").findAll(xml).map { it.groupValues[1] }.toSet()
    }

    private fun readSourceText(relative: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relative")
        if (direct.exists()) {
            return direct.readText(Charsets.UTF_8)
        }
        val fromRoot = File("app/src/main/java/com/example/timeapk/$relative")
        require(fromRoot.exists()) { "Missing source file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }
}
