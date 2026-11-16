package com.example.timeapk.ui.theme

import androidx.compose.ui.graphics.Color

object SongDesignTokens {
    const val RadiusXs = 2
    const val RadiusSm = 4
    const val RadiusMd = 8
    const val RadiusLg = 12

    const val StandardRadius = RadiusSm // Global default corner radius: 4dp.
    const val BorderWidth = 0.5f // Global default border width: 0.5dp.
    const val BorderAlphaSoft = 0.18f
    const val BorderAlphaStrong = 0.32f
    const val PressScaleSubtle = 0.985f

    const val PaddingBase = 8 // Base spacing unit.
    const val PaddingList = 16 // Default list item spacing.

    // 4.2 spacing scale; new code uses these instead of dp literals.
    const val SpaceXs = 4
    const val SpaceSm = 8
    const val SpaceMd = 12
    const val SpaceLg = 16
    const val SpaceXl = 24
    const val SpaceXxl = 32

    // Icon sizes.
    const val IconSmall = 16
    const val IconDefault = 20
    const val IconLarge = 24

    /** Seal-styled add button size and the list padding that keeps the last item clear of it. */
    const val FabSize = 56
    const val FabClearance = FabSize + 2 * SpaceLg

    /** Content max width for forms and settings on wide windows. */
    const val ContentMaxWidth = 640

    const val RippleAlpha = 0.10f
    const val MotionFastMs = 140
    const val MotionNormalMs = 220
    const val MotionSlowMs = 300

    // 4.3.2 纸笺 / 账页 / 历书层级令牌。
    /** 纸纹透明度：0.02 在浅色纸面上几乎不可见，0.035 才能读出纸张质感。 */
    const val PaperTextureAlpha = 0.035f

    /** 墨线宽度：列表分隔线与月历行栏线共用。 */
    const val RuleLineWidth = 0.36f
    const val RuleAlpha = 0.14f

    /** 印章描边宽度：朱文印框与月历“今天”方印框。 */
    const val SealLineWidth = 0.75f

    const val ListDateColumnWidth = 40

    /** 月历格中的分类短横标记。 */
    const val CalendarBarWidth = 7
    const val CalendarBarHeight = 2
    const val CalendarBarSpacing = 2

    const val CalendarCellMinHeight = 48

    const val BaseFontScaleMin = 0.85f
    const val BaseFontScaleMax = 1.30f
    const val WidgetFontScaleMin = 0.85f
    const val WidgetFontScaleMax = 1.60f
}

object SongPalette {
    val Paper = Color(0xFFF5F3ED)
    val PaperWarm = Color(0xFFFFFBF5)
    val PaperMuted = Color(0xFFF2EDE3)
    val PaperDeep = Color(0xFFEDE8DD)
    val Ink = Color(0xFF1F1F1F)
    val InkMuted = Color(0xFF6A6256)
    val InkSoft = Color(0x331F1F1F)
    val Seal = Color(0xFFAF4E31)
    val SealDark = Color(0xFF86351C)
    val SealOnDark = Color(0xFFE29881)
    val Celadon = Color(0xFF457080)
    val CeladonOnDark = Color(0xFF9ABFC5)
    val CeladonWash = Color(0xFFE8EEE6)
    val Jade = Color(0xFF5B8E79)
    val Gold = Color(0xFFAC8F62)
    val DarkPaper = Color(0xFF1C1C1E)
    val DarkPaperVariant = Color(0xFF27272B)
    val DarkInk = Color(0xFFEDE8DD)
}
