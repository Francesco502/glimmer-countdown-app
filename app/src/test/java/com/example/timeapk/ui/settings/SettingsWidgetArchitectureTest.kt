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
            .substringBefore("/** 预览常用尺寸")
        val appearanceBody = editorBody
            .substringAfter("if (selectedTab == 0) {")
            .substringBefore("} else {\n            WidgetOptionGroup(\n                title = stringResource(R.string.widget_config_content_scope)")
        val contentBody = editorBody.substringAfter("R.string.widget_config_content_scope")

        assertTrue(topLevelBody.countOccurrences("SettingsExpandableSection(") == 2)
        val defaultSectionBody = topLevelBody
            .substringAfter("title = stringResource(R.string.widget_config_defaults_title)")
            .substringBefore("title = stringResource(R.string.widget_config_existing_widgets)")
        assertFalse(topLevelBody.contains("widgetFontScaleFlow"))
        assertTrue(defaultSectionBody.contains("summary = widgetConfigSummary(defaultWidgetConfig)"))
        assertTrue(defaultSectionBody.contains("widgetConfigRepository.setDefaultConfig(next)"))
        assertTrue(defaultSectionBody.contains("widget_config_apply_to_all"))
        // Applying the default to every widget now asks first.
        assertTrue(topLevelBody.contains("R.string.widget_config_apply_to_all_confirm"))
        assertTrue(topLevelBody.contains("val latestDefault = widgetConfigRepository.getDefaultConfig()"))
        assertTrue(topLevelBody.contains("ids.associateWith { latestDefault }"))

        // Preview first, then two tabs instead of two-level headings.
        assertTrue(editorBody.contains("WidgetSettingsLayerHeader(title = stringResource(R.string.widget_config_layer_preview))"))
        assertTrue(editorBody.indexOf("WidgetConfigPreview(") < editorBody.indexOf("SongModeTabRow("))
        assertFalse(editorBody.contains("widget_config_layer_display"))

        assertTrue(appearanceBody.contains("widget_config_appearance"))
        assertTrue(appearanceBody.contains("widgetAppearanceSupportsOpacity(config.appearancePreset)"))
        assertTrue(appearanceBody.contains("widget_config_border"))
        assertTrue(appearanceBody.contains("widget_config_corner"))
        assertTrue(appearanceBody.contains("widget_config_contrast"))
        assertTrue(appearanceBody.contains("WidgetFontScaleControl("))
        assertFalse(appearanceBody.contains("widget_config_density"))

        assertTrue(contentBody.contains("widget_config_sort"))
        assertTrue(contentBody.contains("widget_config_display_policy"))
        assertTrue(contentBody.contains("selected = config.displayPolicy"))
        assertTrue(contentBody.contains("config.copy(displayPolicy = it)"))
        assertTrue(contentBody.contains("widget_config_display_policy_help"))
        assertTrue(contentBody.contains("widget_config_density"))
        assertTrue(contentBody.contains("widget_config_show_lunar_prefix"))
        assertTrue(contentBody.contains("widget_config_preview_size"))
        assertFalse(contentBody.contains("widget_config_width_cells"))
        assertFalse(contentBody.contains("widget_config_apply_to_all"))

        assertTrue(widgetSettings.contains("settings_widget_font_scale_title"))
        assertTrue(widgetSettings.contains("settings_widget_font_scale_value"))
        assertTrue(widgetSettings.contains("settings_widget_font_scale_reset_action"))
        assertFalse(contentResolver.contains("widgetFontScaleFlow"))
        assertFalse(contentResolver.contains("effectiveFontScale"))
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
            .substringBefore("internal fun buildWidgetPreviewView")

        assertTrue(previewBody.contains("clean.widthCells"))
        assertTrue(previewBody.contains("clean.heightCells"))
        assertTrue(previewBody.contains("previewAspectRatio"))
        assertTrue(previewBody.contains("previewWidth"))
        assertTrue(previewBody.contains("previewHeight"))
        assertTrue(previewBody.contains("resolveWidgetPreviewSizeBucket("))
        assertTrue(previewBody.contains("WidgetContentResolver.load("))
        assertTrue(previewBody.contains("configOverride = clean"))
        assertTrue(previewBody.contains("changes.collectLatest"))
        assertTrue(previewBody.contains("widget_preview_load_failed"))
        assertFalse(previewBody.contains("widget_config_preview_event"))
        assertFalse(previewBody.contains("val samples"))
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
