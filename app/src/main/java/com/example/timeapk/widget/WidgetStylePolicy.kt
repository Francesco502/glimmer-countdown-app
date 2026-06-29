package com.example.timeapk.widget

data class WidgetTextStyle(
    val titleSp: Float,
    val valueSp: Float,
    val paddingHorizontalDp: Int,
    val paddingVerticalDp: Int,
    val valueMaxEms: Int,
    val dividerHeightDp: Int = 6,
    val emptyTextSp: Float = 12f,
    val useShortValueText: Boolean = false
)

object WidgetStylePolicy {
    const val FontScaleMin = 0.85f
    const val FontScaleMax = 1.60f

    fun resolve(sizeBucket: Int, fontScale: Float): WidgetTextStyle =
        resolve(sizeBucket, fontScale, DENSITY_STANDARD)

    fun resolve(sizeBucket: Int, fontScale: Float, densityMode: Int): WidgetTextStyle {
        val scale = fontScale.coerceIn(FontScaleMin, FontScaleMax)
        val base = when (sizeBucket) {
            WidgetSizeBucket.COMPACT_SQUARE -> WidgetTextStyle(
                titleSp = 9.5f * scale,
                valueSp = 9.5f * scale,
                paddingHorizontalDp = 6,
                paddingVerticalDp = 3,
                valueMaxEms = 8,
                dividerHeightDp = 5,
                emptyTextSp = 11f * scale,
                useShortValueText = true
            )

            WidgetSizeBucket.WIDE_SHORT -> WidgetTextStyle(
                titleSp = 12f * scale,
                valueSp = 12f * scale,
                paddingHorizontalDp = 10,
                paddingVerticalDp = 4,
                valueMaxEms = 16,
                dividerHeightDp = 6,
                emptyTextSp = 12f * scale,
                useShortValueText = true
            )

            WidgetSizeBucket.TALL -> WidgetTextStyle(
                titleSp = 11.5f * scale,
                valueSp = 11.5f * scale,
                paddingHorizontalDp = 9,
                paddingVerticalDp = 5,
                valueMaxEms = 13,
                dividerHeightDp = 7,
                emptyTextSp = 12f * scale
            )

            else -> WidgetTextStyle(
                titleSp = 11f * scale,
                valueSp = 11f * scale,
                paddingHorizontalDp = 8,
                paddingVerticalDp = 3,
                valueMaxEms = 11,
                dividerHeightDp = 6,
                emptyTextSp = 12f * scale
            )
        }
        return base.applyDensity(densityMode)
    }

    private fun WidgetTextStyle.applyDensity(densityMode: Int): WidgetTextStyle {
        return when (densityMode) {
            DENSITY_COMPACT -> copy(
                paddingVerticalDp = (paddingVerticalDp - 1).coerceAtLeast(1),
                dividerHeightDp = (dividerHeightDp - 2).coerceAtLeast(2),
                valueMaxEms = (valueMaxEms - 1).coerceAtLeast(6)
            )

            DENSITY_COMFORTABLE -> copy(
                paddingVerticalDp = paddingVerticalDp + 2,
                dividerHeightDp = dividerHeightDp + 2
            )

            else -> this
        }
    }
}
