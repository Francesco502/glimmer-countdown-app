package com.example.timeapk.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset

/**
 * 统一动效规范 (Interaction Design)
 * - 100–150ms: 微反馈 (hover/click)
 * - 200–300ms: 小过渡 (toggle, dropdown)
 * - 300–500ms: 页面/内容切换
 * 使用 transform + opacity 保证 60fps；尊重 prefers-reduced-motion。
 */
@Immutable
object AnimationSpecs {
    /** 微反馈：点击、hover、焦点变化 */
    const val DurationMicroMs = 150

    /** 小过渡：下拉、开关、小弹层 */
    const val DurationSmallMs = 250

    /** 中等过渡：页面切换、模态、内容入场 */
    const val DurationMediumMs = 350

    /** 入场/强调：轻微减速 (ease-out) */
    val EaseOut = LinearOutSlowInEasing

    /** 出场：轻微加速 (ease-in) */
    val EaseIn = FastOutSlowInEasing

    /** 微反馈 tween */
    fun microTween() = tween<Float>(DurationMicroMs, easing = EaseOut)

    /** 小过渡 tween */
    fun smallTween() = tween<Float>(DurationSmallMs, easing = EaseOut)

    /** 中等过渡 tween（页面/内容） */
    fun mediumTween() = tween<Float>(DurationMediumMs, easing = EaseOut)

    /** 用于 slide/offset 过渡（IntOffset） */
    fun mediumTweenIntOffset() = tween<IntOffset>(DurationMediumMs, easing = EaseOut)

    /** 弹性按压（按钮、FAB、卡片） */
    val springButton = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** 列表项入场：略柔和 */
    val springItem = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** 列表项位置变化（animateItemPlacement 用 IntOffset） */
    val springItemPlacement = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
}
