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
    fun zhCopyAvoidsHardEngineeringToneForSongAestheticSurfaces() {
        val defaultText = readResourceText("values/strings.xml")
        val zhText = readResourceText("values-zh/strings.xml")
        val enText = readResourceText("values-en/strings.xml")
        val reminderSource = readSourceText("ui/reminder/ReminderStatusModels.kt")

        listOf(defaultText, zhText).forEach { text ->
            assertFalse(text.contains(">保存失败，请重试。<"))
            assertFalse(text.contains(">文件保存失败<"))
            assertFalse(text.contains(">事件已删除<"))
            assertFalse(text.contains(">删除<"))
            assertFalse(text.contains(">打开设置<"))
            assertFalse(text.contains("先定事件气质"))
            assertFalse(text.contains("落笔补充细节"))
            assertFalse(text.contains("以纸笺行书写"))
            assertFalse(text.contains("控制首页"))
            assertFalse(text.contains("高级入口"))
            assertFalse(text.contains("检查更新失败"))
            assertFalse(text.contains("下载失败"))
            assertFalse(text.contains("系统日程同步失败"))
            assertFalse(text.contains("事件不存在或已删除"))
            assertFalse(text.contains("删除 %1\$d 天节点"))
            assertFalse(text.contains("请查看权限与日历账户"))
            assertFalse(text.contains("请输入 24 小时制时间"))
            assertFalse(text.contains("例如 2026/02/28"))
            assertFalse(text.contains("你可以前往系统设置"))
            assertFalse(text.contains("当前系统已关闭"))
            assertFalse(text.contains("当前无法访问日历"))
            assertFalse(text.contains("当前内容尚未保存，离开此页后修改将丢失"))
            assertFalse(text.contains("粘贴 JSON，或导入 JSON"))
            assertFalse(text.contains("请导入 JSON"))
            assertFalse(text.contains("HH:mm（例如"))
            assertFalse(text.contains("当前自定义色"))
            assertFalse(text.contains("当前 %1\$d%%"))
            assertFalse(text.contains("当前：%1\$s"))
            assertFalse(text.contains("前往权限页"))
            assertFalse(text.contains("添加天数，如"))
        }
        assertFalse(enText.contains("Save failed"))
        assertFalse(enText.contains("Download failed"))
        assertFalse(enText.contains("System schedule sync failed"))
        assertFalse(enText.contains("Delete event"))
        assertFalse(enText.contains("Event deleted"))
        assertFalse(enText.contains("Choose the event tone first"))
        assertFalse(enText.contains("fill in the details"))
        assertFalse(enText.contains("Control home density"))
        assertFalse(enText.contains("Please wait and try again"))
        assertFalse(enText.contains("Open system settings"))
        assertFalse(enText.contains("Enter a valid date, for example"))
        assertFalse(enText.contains("Use 24-hour time, e.g."))
        assertFalse(enText.contains("Paste JSON, or import JSON"))
        assertFalse(enText.contains("This file could not be recognized. Import JSON"))
        assertFalse(enText.contains("You have unsaved edits. Leave this page"))
        assertFalse(enText.contains("Current custom color"))
        assertFalse(enText.contains("Current: %1\$s"))
        assertFalse(enText.contains("Add days, e.g."))
        assertTrue(defaultText.contains("落笔未成"))
        assertTrue(zhText.contains("落笔未成"))
        assertTrue(reminderSource.contains("scheduleSyncDisplayDetail("))
        assertFalse(reminderSource.contains("detail = event.lastScheduleSyncError"))
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
