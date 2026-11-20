package com.example.timeapk.release

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogoThemeTest {
    @Test
    fun launcherAndSplashRenderApprovedColorsInBothSystemModes() {
        verifyPalette(Configuration.UI_MODE_NIGHT_NO, "#F5F4F2", "#1F251F", "#7CA4A0")
        verifyPalette(Configuration.UI_MODE_NIGHT_YES, "#1C211F", "#ECEBE5", "#95B6B0")
    }

    private fun verifyPalette(nightMode: Int, paper: String, ink: String, celadon: String) {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(target.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
        val context = target.createConfigurationContext(configuration)
        val appIcon = requireNotNull(context.getDrawable(context.applicationInfo.icon))
        assertTrue(appIcon is AdaptiveIconDrawable)
        assertColors(appIcon, paper, ink, celadon)

        val activity = requireNotNull(
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.component
        )
        val theme = context.packageManager.getActivityInfo(activity, 0).themeResource
        val splash = ContextThemeWrapper(context, theme)
        val attributes = if (Build.VERSION.SDK_INT >= 31) {
            splash.obtainStyledAttributes(
                intArrayOf(
                    android.R.attr.windowSplashScreenAnimatedIcon,
                    android.R.attr.windowSplashScreenBackground
                )
            )
        } else {
            splash.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        }
        try {
            val drawable = requireNotNull(attributes.getDrawable(0))
            if (Build.VERSION.SDK_INT >= 31) {
                assertColors(drawable, ink, celadon)
                assertEquals(Color.parseColor(paper), attributes.getColor(1, Color.TRANSPARENT))
            } else {
                assertColors(drawable, paper, ink, celadon)
            }
        } finally {
            attributes.recycle()
        }
    }

    private fun assertColors(drawable: Drawable, vararg expected: String) {
        val size = 240
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            val pixels = IntArray(size * size)
            bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
            expected.forEach { color ->
                assertTrue("Missing rendered color $color", Color.parseColor(color) in pixels)
            }
        } finally {
            bitmap.recycle()
        }
    }
}
