# TimeAPK ProGuard 规则（release 开启 isMinifyEnabled 时生效）
# 保留 Room、Compose、序列化等所需类，避免 release 运行异常

-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

-keep class androidx.compose.** { *; }

# 若使用 Gson/其他序列化，按需添加 keep 规则
# -keepattributes Signature
# -keepattributes *Annotation*
