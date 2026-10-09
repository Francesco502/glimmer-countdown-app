package com.example.timeapk.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.BuildConfig
import java.io.File
import java.io.FileOutputStream

/** Only call from isolated component tests whose rendered data is synthetic. */
internal fun captureComponentUiEvidence(
    rule: ComposeContentTestRule,
    name: String,
    componentState: String,
    exportedImage: Bitmap? = null
) {
    require(name.matches(Regex("[a-z0-9-]+")))
    rule.waitForIdle()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    val directory = File(context.cacheDir, "qa-evidence")
    check(directory.mkdirs() || directory.isDirectory)

    fun savePng(bitmap: Bitmap, suffix: String) {
        FileOutputStream(File(directory, "$name-$suffix.png")).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }

    val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) {
        "Device screenshot unavailable for $name"
    }
    try {
        savePng(screenshot, "device")
        File(directory, "$name-context.txt").writeText(
            """
            Evidence: isolated Compose component in instrumentation Activity on Android emulator
            Data: synthetic test fixture; this is not a formal APK MainActivity screenshot
            Application: ${BuildConfig.APPLICATION_ID}
            Flavor: ${BuildConfig.FLAVOR}; debuggable: ${BuildConfig.DEBUG}
            Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
            Android API: ${Build.VERSION.SDK_INT}
            Device screenshot: ${screenshot.width} x ${screenshot.height} px
            Device density: ${context.resources.displayMetrics.densityDpi} dpi
            Device configuration font scale: ${context.resources.configuration.fontScale}
            Controlled component state: $componentState
            """.trimIndent()
        )
    } finally {
        screenshot.recycle()
    }
    File(directory, "$name-compose-tree.txt").writeText(
        rule.onRoot(useUnmergedTree = true).printToString()
    )
    exportedImage?.let { savePng(it, "export") }
}
