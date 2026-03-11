import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 鍙€夛細浠?gradle.properties 璇诲彇鐗堟湰锛屼究浜?CI/鑴氭湰缁熶竴鏀圭増锛堟湭瀹氫箟鍒欑敤涓嬮潰 defaultConfig 涓殑榛樿鍊硷級
val versionCodeOverride: Int? = (project.findProperty("VERSION_CODE") as? String)?.toIntOrNull()
val versionNameOverride: String? = project.findProperty("VERSION_NAME") as? String

// 鍙€夛細release 绛惧悕銆傚垱寤?keystore 鍚庯紝鍦ㄩ」鐩牴鐩綍娣诲姞 keystore.properties锛堜笉瑕佹彁浜ゅ埌 Git锛夛細
//   storeFile=../timeapk-release.keystore
//   storePassword=xxx
//   keyAlias=timeapk
//   keyPassword=xxx
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasSigningConfig = keystorePropertiesFile.exists()

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

android {
    namespace = "com.example.timeapk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.timeapk"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeOverride ?: 9
        versionName = versionNameOverride ?: "3.5"

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
            // 瀹樼綉/鐩磋娓犻亾锛屼娇鐢ㄩ粯璁?applicationId
        }
        create("play") {
            dimension = "channel"
            applicationIdSuffix = ".play"
            versionNameSuffix = "-play"
        }
    }
    compileOptions {
        // 浣跨敤鏇撮珮鐨?Java 鐗堟湰浠ユ秷闄?JDK 21 瀵?1.8 鐨勫純鐢ㄨ鍛?        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("androidTest").assets {
            directories.add("$projectDir/schemas")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    bundle {
        language {
            enableSplit = false
        }
    }
}

// Release APK 输出名：例如 glimmer-countdown-3-5.apk（版本号中 . 改为 -）
val versionNameForApk = versionNameOverride ?: "3.5"
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

    // 妫€鏌ユ洿鏂帮細鎷夊彇 GitHub Release 淇℃伅
    implementation("com.squareup.okhttp3:okhttp:5.3.2")

    // 鍐滃巻銆佸共鏀紙璇︽儏椤点€岀紭璧凤綔宸插巻锝滈潤鍊欍€嶅睍绀猴級
    implementation("cn.6tail:lunar:1.7.7")

    // 棣栭〉鍗＄墖/鍒楄〃鎷栨嫿鎺掑簭
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


