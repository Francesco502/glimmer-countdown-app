import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Allow version overrides from gradle.properties or CI.
val versionCodeOverride: Int? = (project.findProperty("VERSION_CODE") as? String)?.toIntOrNull()
val versionNameOverride: String? = project.findProperty("VERSION_NAME") as? String

// Keep release credentials external to the repository. Release packaging is
// gated below, while lint and tests remain usable without signing material.
val keystorePropertiesFile = providers.environmentVariable("TIMEAPK_KEYSTORE_PROPERTIES")
    .map(::file)
    .orElse(provider { rootProject.file("keystore.properties") })
    .get()
val signingProperties = keystorePropertiesFile.takeIf(File::isFile)?.let { propertiesFile ->
    Properties().apply { propertiesFile.reader().use(::load) }
}
val signingKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val resolvedStoreFile = signingProperties?.getProperty("storeFile")?.let { value ->
    keystorePropertiesFile.parentFile.resolve(value).canonicalFile
}
val hasValidReleaseSigning = signingProperties != null &&
    signingKeys.all { !signingProperties.getProperty(it).isNullOrBlank() } &&
    resolvedStoreFile?.isFile == true

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    doLast {
        check(hasValidReleaseSigning) {
            "Missing or invalid release signing configuration: ${keystorePropertiesFile.path}"
        }
    }
}

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
        versionCode = versionCodeOverride ?: 23
        versionName = versionNameOverride ?: "4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasValidReleaseSigning) {
            val releaseSigningProperties = requireNotNull(signingProperties)
            val releaseStoreFile = requireNotNull(resolvedStoreFile)
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasValidReleaseSigning) {
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
val versionNameForApk = versionNameOverride ?: "4.0"
val apkBaseName = "glimmer-countdown-${versionNameForApk.replace(".", "-")}"

tasks.register("renameDirectReleaseApk") {
    dependsOn("packageDirectRelease")
    doLast {
        val releaseDir = layout.buildDirectory.dir("outputs/apk/direct/release").get().asFile
        val source = File(releaseDir, "app-direct-release.apk")
        require(source.isFile) { "Missing signed Direct release APK: ${source.path}" }
        val target = File(releaseDir, "$apkBaseName.apk")
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        require(target.isFile && target.length() > 0L) { "Missing renamed Direct release APK" }
        val metadataFile = File(releaseDir, "output-metadata.json")
        if (metadataFile.exists()) {
            metadataFile.writeText(
                metadataFile.readText().replace("\"outputFile\": \"app-direct-release.apk\"", "\"outputFile\": \"$apkBaseName.apk\"")
            )
        }
    }
}

val releasePackagingTask = Regex("(?i)^(assemble|bundle|package).+Release$")
tasks.configureEach {
    if (releasePackagingTask.matches(name) || name == "renameDirectReleaseApk") {
        dependsOn(validateReleaseSigning)
    }
}

// Fail during task-graph preparation so packaging cannot start compilation
// before missing or invalid credentials are reported.
gradle.taskGraph.whenReady {
    if (hasTask(validateReleaseSigning.get())) {
        check(hasValidReleaseSigning) {
            "Missing or invalid release signing configuration: ${keystorePropertiesFile.path}"
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
