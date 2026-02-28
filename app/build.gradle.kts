import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 可选：从 gradle.properties 读取版本，便于 CI/脚本统一改版（未定义则用下面 defaultConfig 中的默认值）
val versionCodeOverride: Int? = (project.findProperty("VERSION_CODE") as? String)?.toIntOrNull()
val versionNameOverride: String? = project.findProperty("VERSION_NAME") as? String

// 可选：release 签名。创建 keystore 后，在项目根目录添加 keystore.properties（不要提交到 Git）：
//   storeFile=../timeapk-release.keystore
//   storePassword=xxx
//   keyAlias=timeapk
//   keyPassword=xxx
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasSigningConfig = keystorePropertiesFile.exists()

android {
    namespace = "com.example.timeapk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.timeapk"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeOverride ?: 4
        versionName = versionNameOverride ?: "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                val keystoreProperties = Properties().apply { load(keystorePropertiesFile.reader()) }
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    // 渠道包（可选）：assemblePlayRelease / assembleDirectRelease，便于分渠道统计与更新
    flavorDimensions += "channel"
    productFlavors {
        create("direct") {
            dimension = "channel"
            // 官网/直装渠道，使用默认 applicationId
        }
        create("play") {
            dimension = "channel"
            applicationIdSuffix = ".play"
            versionNameSuffix = "-play"
        }
    }
    compileOptions {
        // 使用更高的 Java 版本以消除 JDK 21 对 1.8 的弃用警告
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Release APK 输出名：glimmer-countdown-1-0.apk（版本号中 . 改为 -）
val versionNameForApk = versionNameOverride ?: "3.0"
val apkBaseName = "glimmer-countdown-${versionNameForApk.replace(".", "-")}"
tasks.register("renameDirectReleaseApk") {
    dependsOn("packageDirectRelease")
    doLast {
        val releaseDir = layout.buildDirectory.dir("outputs/apk/direct/release").get().asFile
        val fromFile = File(releaseDir, "app-direct-release.apk")
        val toFile = File(releaseDir, "$apkBaseName.apk")
        if (fromFile.exists()) {
            fromFile.renameTo(toFile)
        }
    }
}
project.afterEvaluate {
    tasks.findByName("assembleDirectRelease")?.finalizedBy("renameDirectReleaseApk")
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Room（升级到包含 KSP2 相关修复的稳定版本，避免 unexpected jvm signature V）
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Icons
    implementation("androidx.compose.material:material-icons-extended")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 检查更新：拉取 GitHub Release 信息
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 农历、干支（详情页「缘起｜已历｜静候」展示）
    implementation("cn.6tail:lunar:1.7.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
