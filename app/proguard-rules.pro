# TimeAPK ProGuard 规则（release 开启 isMinifyEnabled 时生效）
#
# 只保留反射/序列化真正需要的项。Compose、Room、DataStore、WorkManager、OkHttp
# 均由 AAR/JAR 自带 consumer rules（或完全不需要 keep），曾在此处整包 keep
# （androidx.compose.** / okhttp3.** / okio.** / androidx.datastore.**）会关闭
# R8 的类合并与裁剪，已移除。改动后请用同版本 APK 体积与启动耗时对比验证。

# ─── Kotlin / 通用 ───
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# ─── 农历 cn.6tail:lunar（详情页通过反射调用） ───
-keep class com.nlf.calendar.** { *; }

# ─── OkHttp / OkIO：仅屏蔽可选依赖的缺失告警 ───
-dontwarn okhttp3.**
-dontwarn okio.**
