package com.example.timeapk.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsWidgetArchitectureTest {
    @Test
    fun settingsCategoriesExposeWidgetAsStandaloneEntry() {
        val components = mainSource("ui/settings/SettingsComponents.kt").readText(Charsets.UTF_8)
        val screen = mainSource("ui/settings/SettingsScreen.kt").readText(Charsets.UTF_8)

        assertTrue(components.contains("WIDGET("))
        assertTrue(screen.contains("SettingsCategory.WIDGET -> WidgetSettingsContent("))
    }

    @Test
    fun appearanceSettingsDoNotOwnWidgetConfigurationUi() {
        val subScreens = mainSource("ui/settings/SettingsSubScreens.kt").readText(Charsets.UTF_8)
        val appearanceBody = subScreens
            .substringAfter("fun AppearanceSettingsContent")
            .substringBefore("@Composable\nprivate fun FontPresetPickerDialog")

        assertFalse(appearanceBody.contains("WidgetConfigRepository"))
        assertFalse(appearanceBody.contains("WidgetConfigEditor("))
        assertFalse(appearanceBody.contains("WidgetInstanceManager("))
        assertFalse(appearanceBody.contains("widgetFontScaleFlow"))
    }

    @Test
    fun existingWidgetEditingUsesConfigActivityInsteadOfInlineEditor() {
        val widgetSettings = mainSource("ui/settings/WidgetSettingsContent.kt").readText(Charsets.UTF_8)
        val activity = mainSource("widget/WidgetConfigActivity.kt").readText(Charsets.UTF_8)
        val managerBody = widgetSettings
            .substringAfter("fun WidgetInstanceManager")
            .substringBefore("@Composable\nprivate fun WidgetOptionGroup")

        assertTrue(widgetSettings.contains("fun WidgetSettingsContent("))
        assertTrue(widgetSettings.contains("WidgetConfigActivity::class.java"))
        assertFalse(managerBody.contains("editingWidgetId"))
        assertFalse(managerBody.contains("WidgetConfigEditor("))
        assertTrue(activity.contains("repository.getConfigForWidget(appWidgetId)"))
        assertFalse(activity.contains("config = repository.getDefaultConfig()"))
    }

    @Test
    fun widgetSettingsPutDisplayControlsInsideDefaultConfiguration() {
        val widgetSettings = mainSource("ui/settings/WidgetSettingsContent.kt").readText(Charsets.UTF_8)
        val contentResolver = mainSource("widget/WidgetContentResolver.kt").readText(Charsets.UTF_8)
        val topLevelBody = widgetSettings
            .substringAfter("fun WidgetSettingsContent(")
            .substringBefore("@Composable\nfun WidgetConfigEditor")
        val editorBody = widgetSettings
            .substringAfter("fun WidgetConfigEditor(")
            .substringBefore("@Composable\nprivate fun WidgetConfigSection")
        val displayBody = editorBody
            .substringAfter("widget_config_layer_display")
            .substringBefore("widget_config_layer_appearance")
        val appearanceBody = editorBody
            .substringAfter("widget_config_layer_appearance")
            .substringBefore("widget_config_layer_content")
        val contentBody = editorBody.substringAfter("widget_config_layer_content")

        assertTrue(topLevelBody.countOccurrences("SettingsExpandableSection(") == 2)
        val defaultSectionBody = topLevelBody
            .substringAfter("widget_config_defaults_title")
            .substringBefore("widget_config_existing_widgets")
        assertFalse(topLevelBody.contains("settings_widget_font_scale_title"))
        assertFalse(topLevelBody.contains("widgetFontScaleFlow"))
        assertFalse(topLevelBody.contains("setWidgetFontScale"))
        assertTrue(defaultSectionBody.contains("summary = widgetConfigSummary(defaultWidgetConfig)"))
        assertTrue(defaultSectionBody.contains("launchWidgetSettingsUpdate"))
        assertTrue(defaultSectionBody.contains("widgetConfigRepository.setDefaultConfig(next)"))
        assertTrue(defaultSectionBody.contains("widget_config_apply_to_all"))
        assertTrue(
            defaultSectionBody.indexOf("widget_config_apply_to_all") <
                defaultSectionBody.indexOf("WidgetConfigEditor(")
        )
        assertTrue(defaultSectionBody.contains("val latestDefault = widgetConfigRepository.getDefaultConfig()"))
        assertTrue(defaultSectionBody.contains("ids.associateWith { latestDefault }"))

        assertFalse(widgetSettings.contains("widget_config_defaults_summary"))
        assertFalse(widgetSettings.contains("widget_config_existing_widgets_summary"))
        assertFalse(widgetSettings.contains("widget_config_existing_widgets_empty_summary"))
        assertFalse(widgetSettings.contains("title = stringResource(R.string.widget_config_existing_widgets),\n            summary = stringResource(R.string.widget_config_defaults_summary)"))

        assertTrue(editorBody.contains("WidgetSettingsLayerHeader("))
        assertTrue(editorBody.contains("widget_config_layer_preview"))
        assertTrue(editorBody.contains("widget_config_layer_display"))
        assertTrue(editorBody.contains("widget_config_layer_appearance"))
        assertTrue(editorBody.contains("widget_config_layer_content"))
        assertFalse(editorBody.contains("widget_config_appearance_summary"))
        assertFalse(editorBody.contains("widget_config_content_summary"))

        assertTrue(widgetSettings.contains("settings_widget_font_scale_title"))
        assertTrue(widgetSettings.contains("settings_widget_font_scale_value"))
        assertTrue(widgetSettings.contains("settings_widget_font_scale_reset_action"))
        assertTrue(displayBody.contains("WidgetFontScaleControl("))
        assertTrue(displayBody.contains("config.fontScale"))
        assertFalse(displayBody.contains("widget_config_size_template"))
        assertFalse(displayBody.contains("widget_config_size_2x2"))
        assertFalse(displayBody.contains("widget_config_size_3x3"))
        assertFalse(displayBody.contains("widget_config_size_4x2"))
        assertTrue(displayBody.contains("widget_config_width_cells"))
        assertTrue(displayBody.contains("widget_config_height_cells"))
        assertTrue(displayBody.contains("(1..5).map"))
        assertTrue(displayBody.contains("widget_config_density"))

        assertTrue(appearanceBody.contains("widget_config_appearance"))
        assertTrue(appearanceBody.contains("widget_config_appearance_group"))
        assertTrue(appearanceBody.contains("widget_config_opacity"))
        assertTrue(appearanceBody.contains("widget_config_border"))
        assertTrue(appearanceBody.contains("widget_config_corner"))
        assertTrue(appearanceBody.contains("widget_config_contrast"))
        assertFalse(appearanceBody.contains("widget_config_size_template"))
        assertFalse(appearanceBody.contains("settings_widget_font_scale_title"))
        assertFalse(appearanceBody.contains("widget_config_density"))

        assertTrue(contentBody.contains("widget_config_content_scope"))
        assertTrue(contentBody.contains("widget_config_content_group"))
        assertTrue(contentBody.contains("widget_config_sort"))
        assertTrue(contentBody.contains("widget_config_show_lunar_prefix"))
        assertFalse(contentBody.contains("widget_config_apply_to_all"))
        assertFalse(contentBody.contains("widget_config_density"))

        assertFalse(contentResolver.contains("widgetFontScaleFlow"))
        assertFalse(contentResolver.contains("effectiveFontScale"))

        val summaryBody = widgetSettings
            .substringAfter("private fun widgetConfigSummary(")
            .substringBefore("private fun appearanceLabelRes")
        assertTrue(summaryBody.contains("widget_config_size_summary"))
        assertTrue(summaryBody.contains("widget_config_percent"))
        assertTrue(summaryBody.contains("contrastLabelRes(config.contrastMode)"))
        assertTrue(summaryBody.contains("densityLabelRes(config.densityMode)"))
        assertFalse(summaryBody.contains("contentScopeLabelRes"))
    }

    @Test
    fun widgetOptionPillsExposeSelectedSemantics() {
        val widgetSettings = mainSource("ui/settings/WidgetSettingsContent.kt").readText(Charsets.UTF_8)
        val pillBody = widgetSettings
            .substringAfter("private fun WidgetOptionPill(")
            .substringBefore("@Composable\nprivate fun WidgetSwitchRow")

        assertTrue(widgetSettings.contains("import androidx.compose.foundation.selection.selectable"))
        assertTrue(widgetSettings.contains("import androidx.compose.ui.semantics.Role"))
        assertTrue(pillBody.contains(".selectable("))
        assertTrue(pillBody.contains("selected = selected"))
        assertTrue(pillBody.contains("role = Role.RadioButton"))
    }

    @Test
    fun widgetPreviewUsesCustomCellAspectRatio() {
        val widgetSettings = mainSource("ui/settings/WidgetSettingsContent.kt").readText(Charsets.UTF_8)
        val previewBody = widgetSettings
            .substringAfter("private fun WidgetConfigPreview(")
            .substringBefore("internal data class WidgetPreviewStyle")

        assertTrue(previewBody.contains("clean.widthCells"))
        assertTrue(previewBody.contains("clean.heightCells"))
        assertTrue(previewBody.contains("previewAspectRatio"))
        assertTrue(previewBody.contains("previewWidth"))
        assertTrue(previewBody.contains("previewHeight"))
        assertTrue(previewBody.contains("compact = clean.widthCells <= 1"))
        assertFalse(previewBody.contains("clean.sizeTemplate"))
        assertFalse(previewBody.contains("SIZE_TEMPLATE_3X3"))
        assertFalse(previewBody.contains("SIZE_TEMPLATE_4X2"))
    }

    @Test
    fun widgetConfigSaveBarAvoidsSystemGestureArea() {
        val activity = mainSource("widget/WidgetConfigActivity.kt").readText(Charsets.UTF_8)
        val saveBar = activity
            .substringAfter("private fun WidgetConfigSaveBar(")
            .substringBefore("@Composable", missingDelimiterValue = activity.substringAfter("private fun WidgetConfigSaveBar("))

        assertTrue(activity.contains("import androidx.compose.foundation.layout.navigationBarsPadding"))
        assertTrue(saveBar.contains(".navigationBarsPadding()"))
    }

    private fun mainSource(relative: String): File {
        return existingFile(
            "src/main/java/com/example/timeapk/$relative",
            "app/src/main/java/com/example/timeapk/$relative"
        )
    }

    private fun existingFile(vararg paths: String): File {
        return paths.map(::File).firstOrNull(File::exists) ?: error("Missing file: ${paths.joinToString()}")
    }

    private fun String.countOccurrences(needle: String): Int {
        return Regex(Regex.escape(needle)).findAll(this).count()
    }
}
