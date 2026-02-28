# 发布前检查清单

**检查日期**：2025-02-28  
**版本**：3.0 (versionCode 4)

---

## 1. 构建与编译

| 项目 | 状态 | 说明 |
|------|------|------|
| Release 构建 | 通过 | `assembleDirectRelease` 成功 |
| Lint 检查 | 通过 | 无阻塞性错误 |
| ProGuard/R8 | 已配置 | `isMinifyEnabled=true`, `proguard-rules.pro` 已保留 Room/Compose |
| 签名 | 需配置 | 需创建 `keystore.properties` 并配置 release 签名；未配置时使用 debug 签名 |

---

## 2. Manifest 与配置

| 项目 | 状态 | 说明 |
|------|------|------|
| MainActivity | 已修复 | 添加 `launchMode="singleTop"` 与 `onNewIntent`，小组件点击在应用已打开时能正确跳转事件详情 |
| 权限 | 正常 | `POST_NOTIFICATIONS`, `INTERNET`, `REQUEST_INSTALL_PACKAGES` |
| 导出标志 | 合理 | MainActivity、CountdownAppWidgetProvider 为 exported；FileProvider、CountdownWidgetService 为 false |
| FileProvider | 正常 | `file_paths.xml` 配置 `updates/` 用于 APK 更新 |

---

## 3. 版本与发布

| 项目 | 值 |
|------|-----|
| versionCode | 4 |
| versionName | 3.0 |
| minSdk | 26 |
| targetSdk | 35 |
| 渠道 | direct / play (`applicationIdSuffix=".play"`) |
| APK 输出 | `app/build/outputs/apk/direct/release/glimmer-countdown-3-0.apk` |

---

## 4. 多语言与资源

| 项目 | 状态 |
|------|------|
| values (默认) | 有 strings.xml |
| values-zh | 有 |
| values-en | 有 |
| 备份规则 | `backup_rules.xml`、`data_extraction_rules.xml` 已配置 |

---

## 5. 潜在问题与空指针

| 检查项 | 结论 |
|--------|------|
| UpdateInstaller | 已有 `response.body == null` 检查，`!!` 使用安全 |
| GitHubReleaseUpdateChecker | 同上 |
| TimeApplication.repository | 单例注入，正常 |
| Widget runBlocking | 在 RemoteViewsService 线程执行，非主线程，可接受 |

---

## 6. 编译期警告（非阻塞）

以下为 deprecation/优化类警告，不影响发布：

- `Theme.kt`: `statusBarColor` 已弃用
- `CountdownAppWidgetProvider.kt`: `setRemoteAdapter` 已弃用（可后续迁移到 `setRemoteViewsAdapter`）
- `WidgetUpdater.kt`: `notifyAppWidgetViewDataChanged` 已弃用（暂无替代 API）
- Gradle properties 中部分选项将在 AGP 10.0 移除

---

## 7. Lint 报告（部分）

- **OldTargetApi**：`tools:targetApi="31"` 可考虑更新
- **SmallSp**：小组件 tag 使用 10sp，略低于 11sp 建议，可保留
- **UnusedResources**：可能有未引用资源
- 完整报告：`app/build/reports/lint-results-directRelease.html`

---

## 8. 本次修复

1. **MainActivity 小组件跳转**：当应用已在后台或前台时，点击小组件列表项会正确打开对应事件详情（`launchMode="singleTop"` + `onNewIntent`）。

---

## 9. 发布前建议步骤

1. [ ] 配置 `keystore.properties` 并完成 Release 签名
2. [ ] 在真机上测试 direct 与 play 渠道安装、升级
3. [ ] 测试小组件：添加/编辑/删除事件后即时刷新；点击列表项跳转详情
4. [ ] 测试检查更新、下载安装流程
5. [ ] 测试导出/导入 JSON、CSV
6. [ ] 验证深色/浅色模式与自定义主题
7. [ ] 确认 `applicationId` 与发布渠道一致（Play 商店使用 `.play` 后缀）
