import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Release signing validation has no outputs")
abstract class ValidateReleaseSigningTask : DefaultTask() {
    @get:Input
    abstract val signingIsValid: Property<Boolean>

    @get:Input
    abstract val signingConfigPath: Property<String>

    @TaskAction
    fun validateSigning() {
        check(signingIsValid.get()) {
            "Missing or invalid release signing configuration: ${signingConfigPath.get()}"
        }
    }
}

@DisableCachingByDefault(because = "Renames an Android Gradle Plugin packaging output in place")
abstract class RenameDirectReleaseApkTask : DefaultTask() {
    @get:Internal
    abstract val sourceFile: RegularFileProperty

    @get:Internal
    abstract val targetFile: RegularFileProperty

    @get:Internal
    abstract val metadataFile: RegularFileProperty

    @get:Input
    abstract val expectedMetadataEntry: Property<String>

    @TaskAction
    fun renameSignedApk() {
        val source = sourceFile.get().asFile
        val target = targetFile.get().asFile
        val metadata = metadataFile.get().asFile

        if (source.isFile) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        require(target.isFile && target.length() > 0L) {
            "Missing signed Direct release APK: ${source.path}"
        }
        require(metadata.isFile) { "Missing Direct release output metadata: ${metadata.path}" }

        val sourceMetadataEntry = "\"outputFile\": \"${source.name}\""
        val expectedMetadataEntry = expectedMetadataEntry.get()
        val originalMetadataText = metadata.readText()
        val metadataText = originalMetadataText.replace(sourceMetadataEntry, expectedMetadataEntry)
        if (metadataText != originalMetadataText) {
            metadata.writeText(metadataText)
        }
        require(metadataText.contains(expectedMetadataEntry)) {
            "Direct release output metadata does not reference ${target.name}"
        }
    }
}

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

val validateReleaseSigning = tasks.register<ValidateReleaseSigningTask>("validateReleaseSigning") {
    signingIsValid.set(hasValidReleaseSigning)
    signingConfigPath.set(keystorePropertiesFile.path)
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
val directReleaseDir = layout.buildDirectory.dir("outputs/apk/direct/release")

val renameDirectReleaseApk = tasks.register<RenameDirectReleaseApkTask>("renameDirectReleaseApk") {
    dependsOn("packageDirectRelease")
    sourceFile.set(directReleaseDir.map { it.file("app-direct-release.apk") })
    targetFile.set(directReleaseDir.map { it.file("$apkBaseName.apk") })
    metadataFile.set(directReleaseDir.map { it.file("output-metadata.json") })
    expectedMetadataEntry.set("\"outputFile\": \"$apkBaseName.apk\"")
}

val releasePackagingTask = Regex(
    "(?i)^(?:(?:assemble|bundle).+Release|package.+Release(?:Bundle|UniversalApk)?)$"
)
tasks.configureEach {
    if (releasePackagingTask.matches(name) || name == "renameDirectReleaseApk") {
        dependsOn(validateReleaseSigning)
    } else if (name != "validateReleaseSigning") {
        // This does not add validation to lint, compilation, or tests. When a
        // final packaging task does add it, validation runs before its other
        // graph dependencies so an invalid release fails before compilation.
        mustRunAfter(validateReleaseSigning)
    }
}

tasks.matching { it.name == "assembleDirectRelease" }.configureEach {
    finalizedBy(renameDirectReleaseApk)
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
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
