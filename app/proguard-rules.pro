# TimeAPK ProGuard 规则（release 开启 isMinifyEnabled 时生效）

# ─── Kotlin / 通用 ───
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# ─── Room ───
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ─── Compose ───
-keep class androidx.compose.** { *; }

# ─── WorkManager ───
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ─── OkHttp / OkIO ───
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ─── 农历 cn.6tail:lunar（详情页通过反射调用） ───
-keep class com.nlf.calendar.** { *; }

# ─── DataStore ───
-keep class androidx.datastore.** { *; }

# ─── 应用自身 data 类（JSON 导入导出用到字段名） ───
-keep class com.example.timeapk.data.Event { *; }
-keep class com.example.timeapk.data.EventJson** { *; }
