# 发布检查清单（v3.6）

**版本**：`3.6`（`versionCode=10`）  
**计划发布日期**：`2026-03-17`

## 一、版本与文档

- [ ] `gradle.properties` 中 `VERSION_CODE=10`、`VERSION_NAME=3.6` 正确
- [ ] `app/build.gradle.kts` 的版本读取、APK 重命名与 Play AAB 构建逻辑正常
- [ ] `README.md`、`CHANGELOG.md`、发布流程文档内容与当前实现一致
- [ ] GitHub Release 标签与标题使用 `v3.6`

## 二、功能回归

- [ ] 重复提醒在日 / 周 / 月 / 半年 / 年场景下均可正确调度
- [ ] 提前 N 天提醒会按天连续写入系统日历 / 日程
- [ ] 开机、时区变更、手动改时间、冷启动后统一重建正常
- [ ] 通知点击可直达对应事件详情
- [ ] Android 13+ 首次启用提醒时可正常申请通知权限
- [ ] 系统日历同步的权限、写入、更新、删除链路一致
- [ ] 编辑页中的提醒与系统日历配置入口清晰可达
- [ ] 首页搜索、分类筛选、自定义排序 / 按剩余天数 / 按目标日期、置顶与月历视图可正常协同
- [ ] 旧用户升级到 `3.6` 后，首页排序会一次性重置为默认“自定义排序”
- [ ] 仅“自定义排序”支持长按拖拽，首页长按不会再进入编辑
- [ ] 首页跨天停留后倒计时会自动刷新
- [ ] 生日详情仅显示农历、岁数、属相、星座

## 三、UI 与可读性

- [ ] 设置页结构、命名与宋式主题视觉一致
- [ ] 表格模式可完整显示“已经 / 还有 / X年X月X日”等中文时间表达
- [ ] 小组件关键数字与中文时间表达不被截断
- [ ] 小 / 中 / 大三类小组件尺寸下展示策略切换正确
- [ ] 深浅主题下关键文本对比可读

## 四、构建与产物

- [ ] `./gradlew test` 通过
- [ ] `./gradlew lintDirectDebug` 通过
- [ ] `./gradlew assembleDirectRelease` 成功
- [ ] `./gradlew bundlePlayRelease` 成功
- [ ] APK 输出路径正确：
  `app/build/outputs/apk/direct/release/glimmer-countdown-3-6.apk`
- [ ] AAB 输出路径正确：
  `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 五、发布动作

- [ ] 代码已提交并 push 到 `origin/main`
- [ ] 已创建并 push 标签 `v3.6`
- [ ] 已创建 GitHub Release，并上传 APK 与 3.6 版本说明
