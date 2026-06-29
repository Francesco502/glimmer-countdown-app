package com.example.timeapk.widget

object WidgetSizeBucket {
    const val EXTRA_SIZE_BUCKET = "widget_size_bucket"

    const val COMPACT_SQUARE = 0
    const val STANDARD_SQUARE = 1
    const val WIDE_SHORT = 2
    const val TALL = 3

    const val SMALL = COMPACT_SQUARE
    const val MEDIUM = STANDARD_SQUARE
    const val LARGE = WIDE_SHORT

    private const val SMALL_MAX_WIDTH_DP = 130
    private const val MEDIUM_MAX_WIDTH_DP = 250
    private const val STANDARD_MIN_WIDTH_DP = 170
    private const val STANDARD_MIN_HEIGHT_DP = 170
    private const val WIDE_MIN_WIDTH_DP = 250
    private const val WIDE_MAX_HEIGHT_DP = 180
    private const val TALL_MIN_HEIGHT_DP = 240
    private const val TALL_MAX_WIDTH_DP = 200

    fun resolve(minWidthDp: Int, maxWidthDp: Int = minWidthDp): Int {
        val effectiveWidthDp = maxOf(minWidthDp, maxWidthDp)
        return when {
            effectiveWidthDp < SMALL_MAX_WIDTH_DP -> SMALL
            effectiveWidthDp < MEDIUM_MAX_WIDTH_DP -> MEDIUM
            else -> LARGE
        }
    }

    fun resolve(
        minWidthDp: Int,
        maxWidthDp: Int = minWidthDp,
        minHeightDp: Int,
        maxHeightDp: Int = minHeightDp
    ): Int {
        val effectiveWidthDp = maxOf(minWidthDp, maxWidthDp)
        val effectiveHeightDp = maxOf(minHeightDp, maxHeightDp)
        return when {
            effectiveWidthDp >= WIDE_MIN_WIDTH_DP && effectiveHeightDp <= WIDE_MAX_HEIGHT_DP -> WIDE_SHORT
            effectiveHeightDp >= TALL_MIN_HEIGHT_DP && effectiveWidthDp <= TALL_MAX_WIDTH_DP -> TALL
            effectiveWidthDp >= STANDARD_MIN_WIDTH_DP && effectiveHeightDp >= STANDARD_MIN_HEIGHT_DP -> STANDARD_SQUARE
            effectiveWidthDp <= SMALL_MAX_WIDTH_DP -> COMPACT_SQUARE
            effectiveWidthDp < MEDIUM_MAX_WIDTH_DP -> STANDARD_SQUARE
            else -> WIDE_SHORT
        }
    }
}
