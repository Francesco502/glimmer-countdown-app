package com.example.timeapk.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExportShareLabelContractTest {
    @Test
    fun shareActionsDescribeSharedTextWhileFileActionsKeepSaveLabels() {
        val settings = readProjectFile("app/src/main/java/com/example/timeapk/ui/settings/SettingsSubScreens.kt")
        val dataSettings = settings.substringAfter("fun DataSettingsContent(")
            .substringBefore("@Composable\nfun AboutSettingsContent")
        val baseStrings = readProjectFile("app/src/main/res/values/strings.xml")
        val englishStrings = readProjectFile("app/src/main/res/values-en/strings.xml")

        assertTrue(dataSettings.contains("label = stringResource(R.string.export_events)"))
        assertTrue(dataSettings.contains("label = stringResource(R.string.export_csv)"))
        assertTrue(dataSettings.contains("label = stringResource(R.string.export_events_file)"))
        assertTrue(dataSettings.contains("label = stringResource(R.string.export_csv_file)"))

        assertTrue(baseStrings.contains("<string name=\"export_events\">分享 JSON 文本</string>"))
        assertTrue(baseStrings.contains("<string name=\"export_csv\">分享 CSV 文本</string>"))
        assertTrue(baseStrings.contains("<string name=\"export_events_file\">保存 JSON 文件</string>"))
        assertTrue(baseStrings.contains("<string name=\"export_csv_file\">保存 CSV 文件</string>"))
        assertTrue(baseStrings.contains("<string name=\"cd_clear_search\">清除搜索</string>"))

        assertTrue(englishStrings.contains("<string name=\"export_events\">Share JSON text</string>"))
        assertTrue(englishStrings.contains("<string name=\"export_csv\">Share CSV text</string>"))
        assertTrue(englishStrings.contains("<string name=\"export_events_file\">Save JSON file</string>"))
        assertTrue(englishStrings.contains("<string name=\"export_csv_file\">Save CSV file</string>"))
        assertTrue(englishStrings.contains("<string name=\"cd_clear_search\">Clear search</string>"))
    }

    private fun readProjectFile(path: String): String {
        val direct = File(path)
        if (direct.exists()) return direct.readText(Charsets.UTF_8)
        val fromApp = File("../$path")
        require(fromApp.exists()) { "Missing project file: $path" }
        return fromApp.readText(Charsets.UTF_8)
    }
}
