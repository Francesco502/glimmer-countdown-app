package com.example.timeapk.widget

data class WidgetTextStyle(
    val titleSp: Float,
    val valueSp: Float,
    val paddingHorizontalDp: Int,
    val paddingVerticalDp: Int,
    val valueMaxEms: Int
)

object WidgetStylePolicy {
    const val FontScaleMin = 0.85f
    const val FontScaleMax = 1.60f

    fun resolve(sizeBucket: Int, fontScale: Float): WidgetTextStyle {
        val scale = fontScale.coerceIn(FontScaleMin, FontScaleMax)
        return when (sizeBucket) {
            WidgetSizeBucket.SMALL -> WidgetTextStyle(
                titleSp = 10f * scale,
                valueSp = 10f * scale,
                paddingHorizontalDp = 4,
                paddingVerticalDp = 1,
                valueMaxEms = 6
            )

            WidgetSizeBucket.LARGE -> WidgetTextStyle(
                titleSp = 12f * scale,
                valueSp = 12f * scale,
                paddingHorizontalDp = 8,
                paddingVerticalDp = 2,
                valueMaxEms = 14
            )

            else -> WidgetTextStyle(
                titleSp = 11f * scale,
                valueSp = 11f * scale,
                paddingHorizontalDp = 6,
                paddingVerticalDp = 1,
                valueMaxEms = 10
            )
        }
    }
}
