# 4.1 公开版本截图

这十张 PNG 原样复制自成功的 [PUBLIC_QA 37995185711](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37995185711) 的 API36 artifact。没有裁剪、缩放、重绘或添加标记；每张归档文件与原图 SHA-256 相同。

- 应用：公开 [v4.1 Release](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.1)，包版本 `4.1 / 24`，公开 APK asset `626232462`，文件 `glimmer-countdown-4-1.apk`，大小 `26463543` bytes。
- APK 源码与 Tag：`v4.1` → `6c52a3a0e7f495e015f12c035ea94bd614fc2813`。
- APK SHA-256：`8fbeee7590f89104995ecce98769842dc2a75cf84d8641efd6a0c7b67619e5b4`。
- QA 驱动源码：`6a1bdb5d73a5e6228303d631e29007a5ad32ca91`。这是运行工作流与 adb 验收脚本的版本，与上述已发布 APK 的源码版本分开记录。
- 原始证据：下载的 API36 artifact 中 `runtime-restore/`；本地解包路径为 `.tmp/public-qa-37995185711/api36/runtime-restore/`。对应 XML、Activity dump、命令记录与 `result.json` 留在同一套原始报告。

环境为 Android API36 Google APIs x86_64 模拟器、竖屏 `1080×1920`、`420dpi / 411dp` 宽、字体比例 `1.0`。实际 Android Activity context locale 为 `en-US`；图中应用文案主要为中文，系统分享面板为英文。英文 `OSDraft`、`OSNote`、`UpgradeEvent` 文本是自动生成的 QA 夹具，不是用户数据，也不代表完整英文界面验收。

实际 MainActivity 的首页、月历、详情、设置和小组件默认预览均已捕捉；本次 `result.json` 的 `passed`、`share_check.passed` 与 `update_check.passed` 为 `true`。分享实际保存出 MediaStore PNG（`1080×1350`），打开系统 chooser 后仅按 Back 取消并返回同一详情；更新页实际显示“已是最新版本”。人工查看这十张原图未发现明确的文字遮挡或主要操作不可达问题。

这些图不提供实体手机或完整字号/语言/主题矩阵覆盖。`widget-preview.png` 是应用内默认配置预览，不是 Launcher 上真实绑定的小组件；`share-chooser.png` 只证明系统分享面板可打开，不证明接收方读取 URI 或完成投递。

| 归档文件 | 本次原始文件（runtime-restore/） | SHA-256（原图与归档相同） |
| --- | --- | --- |
| [home.png](home.png) | `pages-01-home-cards.png` | `f58d989abea2b4ac360082434ce387825e5dee5544823ab9ac156c068b17d442` |
| [calendar.png](calendar.png) | `pages-02-calendar.png` | `1464a7f3fca05fcbd01209868465000cbee72928f65a716c5d457e1492df1d03` |
| [detail.png](detail.png) | `pages-03-event-detail.png` | `428bb5d0e58a746eed2d5e29d50c11647d7e98013b3db6fd573e05a8e1cec561` |
| [settings.png](settings.png) | `pages-04-settings-root.png` | `30c5e0fe23eadd5c9aa9608752767f04bb70dd8859c438b3ff3f51a20e768366` |
| [widget-preview.png](widget-preview.png) | `pages-05-widget-default-preview.png` | `c8ddd895ce2a064b3830c51c8b68727562a01b89342b37d04b1b010f8f8f190e` |
| [share-preview.png](share-preview.png) | `share-01-preview.png` | `ae01cfa58597270937cbeeea61267a24d043df271ba912f75f7b0d3d4d60c5a0` |
| [share-saved.png](share-saved.png) | `share-saved-image.png` | `d36ec8dbdd4403fbf078ebaa57963b94693e0efc9a8190644a1db9ab3f66da5b` |
| [share-chooser.png](share-chooser.png) | `share-04-chooser.png` | `263c13114a249c9a09f0fd4c91b01ac411b84a898c9b5ea3a0e62e5d69ce8aab` |
| [share-return.png](share-return.png) | `share-05-returned-detail.png` | `8fea6e1d2005e9f9343b4ce87c1085060b58bba50d159ad06672b03a74098327` |
| [update-check.png](update-check.png) | `update-03-already-current.png` | `bba2983adfd97cc3702f16cd1b482f2950577a354efb0ae1f349174d1dd13e0f` |
