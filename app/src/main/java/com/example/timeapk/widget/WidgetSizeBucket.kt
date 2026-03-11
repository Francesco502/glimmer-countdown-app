package com.example.timeapk.widget

object WidgetSizeBucket {
    const val EXTRA_SIZE_BUCKET = "widget_size_bucket"

    const val SMALL = 0
    const val MEDIUM = 1
    const val LARGE = 2

    private const val SMALL_MAX_WIDTH_DP = 130
    private const val MEDIUM_MAX_WIDTH_DP = 250

    fun resolve(minWidthDp: Int, maxWidthDp: Int = minWidthDp): Int {
        val effectiveWidthDp = maxOf(minWidthDp, maxWidthDp)
        return when {
            effectiveWidthDp < SMALL_MAX_WIDTH_DP -> SMALL
            effectiveWidthDp < MEDIUM_MAX_WIDTH_DP -> MEDIUM
            else -> LARGE
        }
    }
}
