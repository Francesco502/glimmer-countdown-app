# 4.2 公开版本截图

这十张 PNG 原样复制自成功的 [PUBLIC_QA 38239618078](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38239618078) 的 API36 artifact。没有裁剪、缩放、重绘或添加标记；每张归档文件与原图 SHA-256 相同。

- 应用：公开 [v4.2 Release](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.2)，包版本 `4.2 / 25`，公开 APK asset `633513961`，文件 `glimmer-countdown-4-2.apk`，大小 `12513338` bytes。
- APK 源码与 Tag：`v4.2` → `f0937448efa02232d5c6b1d47c531d50cad0797e`（annotated tag 对象 `294035246e8a96072175710efbc4961088531df3`）。QA 工作流从同一 tag 运行，驱动与 APK 源码相同。
- APK SHA-256：`7fe9673e4b3a85f4152c1e5786f8a221779a95fc56578f9c93b660b4e9019e74`。
- 原始证据：下载的 API36 artifact 中 `runtime-restore/`；本地解包路径为 `.tmp/public-qa-38239618078/signed-apk-qa-api36-f0937448efa02232d5c6b1d47c531d50cad0797e/runtime-restore/`。对应 XML、Activity dump、命令记录与 `result.json` 留在同一套原始报告。

环境为 Android API36 Google APIs x86_64 模拟器、竖屏 `1080×1920`、字体比例 `1.0`，系统 locale `en-US`；图中应用文案为中文，系统分享面板为英文。`OSDraft`、`OSNote`、`UpgradeEvent` 文本是自动生成的 QA 夹具，不是用户数据，也不代表完整英文界面验收。

实际 MainActivity 的首页、月历、详情、设置和小组件默认预览均已捕捉；本次 `result.json` 的 `passed`、`share_check.passed` 与 `update_check.passed` 为 `true`。分享实际保存出 MediaStore PNG（`1080×1350`，58,483 bytes），打开系统 chooser 后仅按 Back 取消并返回同一详情；更新页实际显示“已是最新版本”。人工查看这十张原图未发现明确的文字遮挡或主要操作不可达问题。

这些图不提供实体手机或完整字号/语言/主题矩阵覆盖，也不覆盖自定义分类、事件配图、单事件小组件、宽屏双栏与动态取色。`widget-preview.png` 是应用内默认配置预览，不是 Launcher 上真实绑定的小组件；`share-chooser.png` 只证明系统分享面板可打开，不证明接收方读取 URI 或完成投递。

| 归档文件 | 本次原始文件（runtime-restore/） | SHA-256（原图与归档相同） |
| --- | --- | --- |
| [home.png](home.png) | `pages-01-home-cards.png` | `552352881c354d64fdabae1936ce374688f7b207dd390d1078c1e313e75d72f7` |
| [calendar.png](calendar.png) | `pages-02-calendar.png` | `49c804b18cd0000daaa8c5dfc6031fde654f8cbe43a58b77f8d993d5255a43e2` |
| [detail.png](detail.png) | `pages-03-event-detail.png` | `5a35eef0ce39e018a8a0207f302951668fa77818e6b851f4108248b8273e0f8d` |
| [settings.png](settings.png) | `pages-04-settings-root.png` | `5cf3856d826b494fe9615b7fe046036ba9ef0128bd7c924009b153333ac6d7a4` |
| [widget-preview.png](widget-preview.png) | `pages-05-widget-default-preview.png` | `36d752e1df8deff24b06e376eaf273f8c9fa74d63d88e2a48888f9411c3f3142` |
| [share-preview.png](share-preview.png) | `share-01-preview.png` | `49a509f6ba24b20155dc39d5cee86f7972437c4a983c82490c78f3d3cc7c99e5` |
| [share-saved.png](share-saved.png) | `share-saved-image.png` | `8133a800f93a767dbffa0b816b6b449a2b4ab03cef0bca35f598d3291cd7e7bf` |
| [share-chooser.png](share-chooser.png) | `share-04-chooser.png` | `23366c0f106db94266794e8f39e76e41b3ef5db79fba60a3393c81aee188b131` |
| [share-return.png](share-return.png) | `share-05-returned-detail.png` | `fe0cf11d7aced97630cd7cf87fa30b8cde0fec438c375a6354435b0ba24420ab` |
| [update-check.png](update-check.png) | `update-03-already-current.png` | `1a014a52d25132933eafdf018ddfa9762d8637fefa7b46a533d6836764ce4928` |
