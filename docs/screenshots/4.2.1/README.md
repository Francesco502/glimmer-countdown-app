# 4.2.1 正式版本截图

十张页面/分享/更新 PNG 原样复制自成功的 [公开包 QA 38742224120](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38742224120) 的 API36 artifact；另三张 Launcher 原图来自本地独立 API36 模拟器。所有13张均已逐张查看，未裁剪、缩放、重绘或添加标记；归档与原图 SHA-256 相同，详见 [provenance.json](provenance.json)。

- 正式 [v4.2.1 Release](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.2.1)：4.2.1 / 26，唯一资产643936757，`glimmer-countdown-4-2-1.apk`，12,517,434 bytes。
- APK 来自不可变 `v4.2.1` → `e532c3333e10148c5771906fd2996adb53735104`（tag对象51440e1875824a6c8d95fdaeff8a0d2fd812d020）的成功标签CI；SHA-256 `21563bc13a72be41d0f6cb21e8db136bcafd20bcf7c94a6390fd53e085cd896c`。
- 签名包 QA 驱动 `d3c5ff9b1f196f4598c27816d61414eb05fe58ce` 只缩短升级夹具固定标签，保留全部数据断言、逐字符核对、12位随机标识和原时限；APK源码未变。公开API26/31/36升级与AMS恢复均通过，API36五页、分享及实际更新检查通过。
- 公开页面原始报告：`.tmp/public-qa-38742224120/signed-apk-qa-api36-d3c5ff9b1f196f4598c27816d61414eb05fe58ce/runtime-restore/`，相应XML、Activity dump、命令和result.json保留在同一报告。

页面图环境为API36 Google APIs x86_64模拟器、1080×1920、字体1.0、系统en-US；应用文案中文，分享面板英文。UE-/UN-、OSDraft/OSNote、UpgradeEvent均为合成QA数据。

`launcher-*.png` 是实际Pixel Launcher绑定的列表和单日小组件，经历日间→夜间→日间切换。它们安装的是最终签名候选，安装后读回base.apk的SHA-256与公开资产一致；原始记录为 `.tmp/finaltag-launcher-v421/`。这是模拟器中的真实宿主绑定，实体手机及其Launcher未测。

`widget-preview.png` 为应用内默认预览；`share-saved.png` 为实际保存到MediaStore的1080×1350 PNG（58,334 bytes）。chooser仅打开后按Back取消，没有选择接收方或验证投递；更新页实际显示“已是最新版本”。这些截图不代表完整字体/语言/尺寸/旋转/TalkBack矩阵、物理通知/日历或长期性能验收。

| 归档文件 | 原始文件 | SHA-256（原图与归档相同） |
|---|---|---|
| [home.png](home.png) | `pages-01-home-cards.png` | `9aa1a855ffd8073a46acb8ba72d7dbefe26d521d9f3b3c1859284fd4f21fc0c4` |
| [calendar.png](calendar.png) | `pages-02-calendar.png` | `c00638ee85b3bfa09a8cc7823323556827641785efba0cb6a608e544c1733eab` |
| [detail.png](detail.png) | `pages-03-event-detail.png` | `7848341e72db10b04f088a253090bad917447d540cb4906bab567c17e391b628` |
| [settings.png](settings.png) | `pages-04-settings-root.png` | `39df08a040a94470822564eba81d5d867904e5b3c5726d61fde3d49cd079de90` |
| [widget-preview.png](widget-preview.png) | `pages-05-widget-default-preview.png` | `c04f5add11b1c1a087c51b4c5f6f313c806a0783c45020ff2b8abcf0c16e4d93` |
| [share-preview.png](share-preview.png) | `share-01-preview.png` | `d94e90076589b387fdb977548758f7273f13a2de598866b2f1a70289503455ac` |
| [share-saved.png](share-saved.png) | `share-saved-image.png` | `f3a2aa5ee314bb1efea1c4f68ea7707cdaa619bb1bf7d9f64944f24bad6ce658` |
| [share-chooser.png](share-chooser.png) | `share-04-chooser.png` | `f16a7e45e1f17a9a44aab553022904f0d170fc2f3cd72683ff537f1b4af024b2` |
| [share-return.png](share-return.png) | `share-05-returned-detail.png` | `2fc228036fb924710fcb2e0a7f06f6916bd65a03521f51c5bf19840b68ebf333` |
| [update-check.png](update-check.png) | `update-03-already-current.png` | `843e1f218eac6e16cfdb4ca9d3300582295a7ef802e78541bcf8fb3048a4b6f4` |
| [launcher-light.png](launcher-light.png) | `widgets-light.png` | `389607f299e23b87a049e66ff577571a512d729d11062bc93ed032b7993914e4` |
| [launcher-dark.png](launcher-dark.png) | `widgets-dark.png` | `6787a4f1eda82d33b04157f3e0e007c90950256a2e46d17dde54921dc46c12c6` |
| [launcher-returned-light.png](launcher-returned-light.png) | `widgets-returned-light.png` | `422ef1311da90072f995af3444f016a8bc4712f9dacaf14df1865da087cf9429` |
