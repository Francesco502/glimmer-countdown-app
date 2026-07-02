import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Allow version overrides from gradle.properties or CI.
val versionCodeOverride: Int? = (project.findProperty("VERSION_CODE") as? String)?.toIntOrNull()
val versionNameOverride: String? = project.findProperty("VERSION_NAME") as? String

// Read release signing info from keystore.properties at the repo root.
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
        versionCode = versionCodeOverride ?: 20
        versionName = versionNameOverride ?: "3.15"

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

    flavorDimensions += "channel"
    productFlavors {
        create("direct") {
            dimension = "channel"
            buildConfigField("boolean", "DIRECT_APK_UPDATES_ENABLED", "true")
        }
        create("play") {
            dimension = "channel"
            applicationIdSuffix = ".play"
            versionNameSuffix = "-play"
            buildConfigField("boolean", "DIRECT_APK_UPDATES_ENABLED", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
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

// Rename the direct release APK to the glimmer-countdown-x-y style.
val versionNameForApk = versionNameOverride ?: "3.15"
val apkBaseName = "glimmer-countdown-${versionNameForApk.replace(".", "-")}"

tasks.register("renameDirectReleaseApk") {
    dependsOn("packageDirectRelease")
    doLast {
        val releaseDir = layout.buildDirectory.dir("outputs/apk/direct/release").get().asFile
        val fromFile = File(releaseDir, "app-direct-release.apk")
        val toFile = File(releaseDir, "$apkBaseName.apk")
        if (fromFile.exists()) {
            if (toFile.exists()) {
                toFile.delete()
            }
            fromFile.renameTo(toFile)
        }
        val metadataFile = File(releaseDir, "output-metadata.json")
        if (metadataFile.exists()) {
            metadataFile.writeText(
                metadataFile.readText().replace("\"outputFile\": \"app-direct-release.apk\"", "\"outputFile\": \"$apkBaseName.apk\"")
            )
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

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("cn.6tail:lunar:1.7.7")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260522")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
