# Widget Preview Size Guidance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add approved guidance to widget settings so users understand that width and height controls affect only the preview and that the launcher controls the actual home-screen widget size.

**Architecture:** Keep the change inside the existing `WidgetConfigEditor`: one localized `Text` node appears after the width/height pair and before density. Source-level contracts pin the exact three-locale copy, position, typography, and color; no widget configuration or launcher behavior changes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android string resources, JUnit 4 source/resource contract tests, Gradle, ADB on the existing API 37 emulator.

## Global Constraints

- Keep `versionName=4.0` and `versionCode=23`.
- Simplified Chinese and default copy must be exactly: `这里只调整预览比例，不会改变桌面小组件的实际尺寸。实际尺寸请在桌面拖动小组件边框调整。`
- English copy must be exactly: `These controls only adjust the preview proportions. Resize the actual widget from your launcher.`
- Use one helper text after preview width and preview height, before display density.
- Use `MaterialTheme.typography.bodySmall` and `MaterialTheme.colorScheme.onSurfaceVariant` with existing section spacing.
- Do not change launcher resize behavior, preview calculations, 1–5 cell choices, widget configuration storage, sorting, or multi-instance behavior.
- Verify the current implementation at 100% and 150% system font; restore the emulator font scale to 100% afterward.

---

## File Map

- `app/src/test/java/com/example/timeapk/i18n/StringResourceSanityTest.kt`: exact default/zh/en copy contract.
- `app/src/test/java/com/example/timeapk/ui/settings/SettingsWidgetArchitectureTest.kt`: widget-editor placement and visual-token contract.
- `app/src/main/res/values/strings.xml`: default Chinese guidance.
- `app/src/main/res/values-zh/strings.xml`: Simplified Chinese guidance.
- `app/src/main/res/values-en/strings.xml`: English guidance.
- `app/src/main/java/com/example/timeapk/ui/settings/WidgetSettingsContent.kt`: render the helper text in the existing display section.
- `CHANGELOG.md`: record the clarification and runtime verification under 4.0.
- `docs/RELEASE_CHECKLIST.md`: close the exact widget-default-config row with fresh 100%/150% evidence and append already completed 4.0 runtime evidence without closing unrelated open gates.

### Task 1: Add the Localized Helper Text with a Red-Green Cycle

**Files:**
- Modify: `app/src/test/java/com/example/timeapk/i18n/StringResourceSanityTest.kt`
- Modify: `app/src/test/java/com/example/timeapk/ui/settings/SettingsWidgetArchitectureTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/java/com/example/timeapk/ui/settings/WidgetSettingsContent.kt`

**Interfaces:**
- Consumes: existing resource readers and the `WidgetConfigEditor` source layout.
- Produces: `R.string.widget_config_size_preview_guidance`, a passive Compose text node, and exact copy/placement/visual-token contracts.

- [ ] **Step 1: Extend the exact localization contract**

Replace the body of `widgetSizeCopyClarifiesPreviewNotLauncherResize()` after its three resource reads with:

```kotlin
        val zhGuidance = "<string name=\"widget_config_size_preview_guidance\">这里只调整预览比例，不会改变桌面小组件的实际尺寸。实际尺寸请在桌面拖动小组件边框调整。</string>"
        val enGuidance = "<string name=\"widget_config_size_preview_guidance\">These controls only adjust the preview proportions. Resize the actual widget from your launcher.</string>"

        listOf(defaultText, zhText).forEach { xml ->
            assertTrue(xml.contains("<string name=\"widget_config_width_cells\">预览宽度</string>"))
            assertTrue(xml.contains("<string name=\"widget_config_height_cells\">预览高度</string>"))
            assertTrue(xml.contains(zhGuidance))
            assertFalse(xml.contains("<string name=\"widget_config_width_cells\">宽度</string>"))
            assertFalse(xml.contains("<string name=\"widget_config_height_cells\">高度</string>"))
        }
        assertTrue(enText.contains("<string name=\"widget_config_width_cells\">Preview width</string>"))
        assertTrue(enText.contains("<string name=\"widget_config_height_cells\">Preview height</string>"))
        assertTrue(enText.contains(enGuidance))
```

- [ ] **Step 2: Add the widget-editor wiring contract**

In `widgetSettingsPutDisplayControlsInsideDefaultConfiguration()`, after the existing height assertion and before the density assertion, add:

```kotlin
        assertTrue(displayBody.contains("widget_config_size_preview_guidance"))
        assertTrue(
            displayBody.indexOf("widget_config_height_cells") <
                displayBody.indexOf("widget_config_size_preview_guidance")
        )
        assertTrue(
            displayBody.indexOf("widget_config_size_preview_guidance") <
                displayBody.indexOf("widget_config_density")
        )
        assertTrue(displayBody.contains("style = MaterialTheme.typography.bodySmall"))
        assertTrue(displayBody.contains("color = MaterialTheme.colorScheme.onSurfaceVariant"))
```

- [ ] **Step 3: Run the focused tests and verify red**

Run:

```bash
JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.i18n.StringResourceSanityTest.widgetSizeCopyClarifiesPreviewNotLauncherResize --tests com.example.timeapk.ui.settings.SettingsWidgetArchitectureTest.widgetSettingsPutDisplayControlsInsideDefaultConfiguration
```

Expected: the build fails with both targeted tests reporting missing `widget_config_size_preview_guidance`; the architecture test also lacks the required body-small/on-surface-variant helper wiring.

- [ ] **Step 4: Review the red diff**

Run:

```bash
git diff --check
git diff -- app/src/test/java/com/example/timeapk/i18n/StringResourceSanityTest.kt app/src/test/java/com/example/timeapk/ui/settings/SettingsWidgetArchitectureTest.kt
```

Expected: no whitespace errors; only the two intended contract-test edits are present.

- [ ] **Step 5: Add the three aligned resource entries**

Immediately after `widget_config_height_cells` in both Chinese files, add:

```xml
    <string name="widget_config_size_preview_guidance">这里只调整预览比例，不会改变桌面小组件的实际尺寸。实际尺寸请在桌面拖动小组件边框调整。</string>
```

Immediately after `widget_config_height_cells` in the English file, add:

```xml
    <string name="widget_config_size_preview_guidance">These controls only adjust the preview proportions. Resize the actual widget from your launcher.</string>
```

- [ ] **Step 6: Render the helper after height and before density**

In `WidgetConfigEditor`, insert this block between the height `WidgetOptionGroup` and density `WidgetOptionGroup`:

```kotlin
            Text(
                text = stringResource(R.string.widget_config_size_preview_guidance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
```

This uses existing imports and design tokens; add no semantics role or click handler.

- [ ] **Step 7: Run the focused tests and verify green**

Run:

```bash
JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.i18n.StringResourceSanityTest.widgetSizeCopyClarifiesPreviewNotLauncherResize --tests com.example.timeapk.ui.settings.SettingsWidgetArchitectureTest.widgetSettingsPutDisplayControlsInsideDefaultConfiguration
```

Expected: `BUILD SUCCESSFUL`; both focused tests pass.

- [ ] **Step 8: Run the complete resource and widget-settings test classes**

Run:

```bash
JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew testDirectDebugUnitTest --tests com.example.timeapk.i18n.StringResourceSanityTest --tests com.example.timeapk.ui.settings.SettingsWidgetArchitectureTest
```

Expected: `BUILD SUCCESSFUL`; no localization-key alignment or widget architecture regressions.

- [ ] **Step 9: Commit the implementation**

```bash
git add app/src/test/java/com/example/timeapk/i18n/StringResourceSanityTest.kt app/src/test/java/com/example/timeapk/ui/settings/SettingsWidgetArchitectureTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml app/src/main/res/values-en/strings.xml app/src/main/java/com/example/timeapk/ui/settings/WidgetSettingsContent.kt
git commit -m "fix: clarify widget preview size controls"
```

Expected: one focused commit containing tests, resources, and the single Compose text node.

### Task 2: Verify Reflow and Accessibility on the Emulator

**Files:**
- Evidence only: `/tmp/timeapk-v4-widget-size-guidance/`

**Interfaces:**
- Consumes: Direct Debug build from Task 1 and connected `emulator-5554`.
- Produces: current-run screenshots and UI hierarchy dumps at 100% and 150% font scale.

- [ ] **Step 1: Confirm the target and install a fresh build**

Run:

```bash
/Users/francesco/Library/Android/sdk/platform-tools/adb devices
JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew installDirectDebug
mkdir -p /tmp/timeapk-v4-widget-size-guidance
```

Expected: `emulator-5554` is `device`; Gradle reports `BUILD SUCCESSFUL` and installs `com.example.timeapk`.

- [ ] **Step 2: Inspect 100% font scale**

Run:

```bash
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell settings put system font_scale 1.0
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell am force-stop com.example.timeapk
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell am start -n com.example.timeapk/.MainActivity
```

Navigate to 设置 → 小组件 → 默认配置, expand “文字与布局”, and scroll until preview height, the guidance, and display density are visible. Then run:

```bash
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 exec-out screencap -p > /tmp/timeapk-v4-widget-size-guidance/widget-guidance-100.png
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell uiautomator dump /sdcard/widget-guidance-100.xml
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 pull /sdcard/widget-guidance-100.xml /tmp/timeapk-v4-widget-size-guidance/widget-guidance-100.xml
```

Expected: the full Chinese message is readable below the two size controls, is visibly lower-emphasis than option titles, and density remains reachable.

- [ ] **Step 3: Inspect 150% font scale**

Run:

```bash
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell settings put system font_scale 1.5
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell am force-stop com.example.timeapk
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell am start -n com.example.timeapk/.MainActivity
```

Return to 设置 → 小组件 → 默认配置, expand “文字与布局”, and scroll through the same region. Then run:

```bash
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 exec-out screencap -p > /tmp/timeapk-v4-widget-size-guidance/widget-guidance-150.png
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell uiautomator dump /sdcard/widget-guidance-150.xml
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 pull /sdcard/widget-guidance-150.xml /tmp/timeapk-v4-widget-size-guidance/widget-guidance-150.xml
```

Expected: the message wraps without clipping or overlap; its full text appears in the UI hierarchy; display density remains reachable by scrolling.

- [ ] **Step 4: Restore the test device and inspect evidence**

Run:

```bash
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell settings put system font_scale 1.0
/Users/francesco/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell rm -f /sdcard/widget-guidance-100.xml /sdcard/widget-guidance-150.xml
rg -n "这里只调整预览比例|显示密度" /tmp/timeapk-v4-widget-size-guidance/widget-guidance-100.xml /tmp/timeapk-v4-widget-size-guidance/widget-guidance-150.xml
```

Expected: both hierarchy files contain the complete guidance and density label; the emulator is restored to 100% font.

### Task 3: Run Full Gates and Update 4.0 Evidence

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `docs/RELEASE_CHECKLIST.md`

**Interfaces:**
- Consumes: green implementation and current-run UI evidence.
- Produces: updated 4.0 release documentation without claiming unavailable formal-signing, physical-device, PowerShell, or GitHub-publishing evidence.

- [ ] **Step 1: Run the full deterministic quality gate**

Run:

```bash
JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew testDirectDebugUnitTest testPlayDebugUnitTest compileDirectDebugAndroidTestKotlin lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease assembleDirectDebug assemblePlayDebug
```

Expected: `BUILD SUCCESSFUL`; Direct and Play JVM suites, Android-test compilation, five lint/vital-lint tasks, and both Debug assemblies pass.

- [ ] **Step 2: Run the connected suite**

Run:

```bash
JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew connectedDirectDebugAndroidTest
```

Expected: `BUILD SUCCESSFUL`; all connected tests pass on `emulator-5554`, including migrations, filtered reorder, edit recreation, and widget multi-instance isolation.

- [ ] **Step 3: Update the 4.0 changelog**

Under `## [4.0] - 待发布` → `### 修复`, add:

```markdown
- 在小组件默认配置中补充尺寸说明，明确“预览宽度 / 预览高度”只调整应用内预览比例，桌面实际尺寸仍由 Launcher 拖动边框调整。
```

Under `### 测试`, add:

```markdown
- 小组件默认配置在 100% 与 150% 系统字体下完成运行态复测；尺寸说明可完整换行阅读，后续密度控件可滚动到达。
```

- [ ] **Step 4: Update the release checklist conservatively**

Change only this exact row to checked:

```markdown
- [x] 小组件默认配置页显示 1-5 格“预览宽度 / 预览高度”，并说明这些选项只改变预览比例；Launcher 实际尺寸仍在桌面拖动边框调整
```

Append a `2026-07-17` runtime record under `## 五、4.0 实测记录` that includes:

```markdown
- 小组件设置说明（2026-07-17）：API 37 `emulator-5554` 的 Direct Debug 在 100% / 150% 系统字体下验证默认配置页；1-5 格预览宽高、完整 Launcher 尺寸说明及后续显示密度均可读且可滚动到达，系统字体已恢复 100%。同轮资源与架构契约覆盖默认中文、简体中文、英文文案及说明节点位置。
```

Append these already completed runtime records from the same 4.0 candidate:

```markdown
- 核心交互补充（2026-07-17）：API 37 Direct Debug 验证新建事件在拒绝通知与日历权限后主体仍可保存；详情页删除后 Snackbar 撤销成功恢复事件；置顶语义更新为“取消置顶”；搜索无结果时可清除搜索，生日与其他分类筛选结果正确。测试事件均已清理。由于该轮未覆盖导出往返、全部重复周期和设备重启提醒，相关综合清单项继续保留未勾选。
- 分享链路补充（2026-07-17）：详情分享预览成功，MediaStore 保存 `image/png` 的 1080×1350 图片，并真实打开 Android 系统分享选择器；测试图片和事件均已清理。Direct 正式 APK 安装路径仍依赖正式签名产物，因此分享与更新综合项继续保留未勾选。
- 导入链路补充（2026-07-17）：无效文本 `notjson` 返回可解释错误且未改变现有事件；通过系统文件选择器导入脱敏 `.mdb` fixture，首次识别 2 条并导入 2 条，第二次识别 2 条重复且禁用导入，0 条解析错误。导入事件与下载目录 fixture 均已清理；JSON / CSV 完整导入导出往返仍未完成，因此导入综合项继续保留未勾选。
```

- [ ] **Step 5: Review the final diff and documentation claims**

Run:

```bash
git diff --check
git diff --stat
git status --short
```

Expected: no whitespace errors; only the approved source/tests/resources plus 4.0 changelog/checklist evidence are changed; formal-signing, physical-device, performance, PowerShell, and GitHub-publication rows remain unchecked.

- [ ] **Step 6: Commit verification records**

```bash
git add CHANGELOG.md docs/RELEASE_CHECKLIST.md
git commit -m "docs: record widget size guidance verification"
```

Expected: a focused documentation commit after all automated and current-run UI gates pass.

- [ ] **Step 7: Confirm clean handoff state**

Run:

```bash
git status --short
git log -4 --oneline
```

Expected: working tree is clean and the implementation plus verification commits sit on top of the approved design and plan history.
