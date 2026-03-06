package com.example.timeapk.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp

@Immutable
object AnimationSpecs {
    const val DurationMicroMs = SongDesignTokens.MotionFastMs
    const val DurationSmallMs = SongDesignTokens.MotionNormalMs
    const val DurationMediumMs = SongDesignTokens.MotionSlowMs

    @Volatile
    private var reducedMotionEnabled: Boolean = false

    val EaseOut = LinearOutSlowInEasing
    val EaseIn = FastOutSlowInEasing

    private fun effectiveDuration(duration: Int): Int = if (reducedMotionEnabled) 0 else duration

    fun setReducedMotionEnabled(enabled: Boolean) {
        reducedMotionEnabled = enabled
    }

    fun microTween() = tween<Float>(effectiveDuration(DurationMicroMs), easing = EaseOut)

    fun smallTween() = tween<Float>(effectiveDuration(DurationSmallMs), easing = EaseOut)

    fun smallTweenDp() = tween<Dp>(effectiveDuration(DurationSmallMs), easing = EaseOut)

    fun mediumTween() = tween<Float>(effectiveDuration(DurationMediumMs), easing = EaseOut)

    fun mediumTweenIntOffset() = tween<IntOffset>(effectiveDuration(DurationMediumMs), easing = EaseOut)

    private val springButtonDefault = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    private val springItemDefault = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    private val springItemPlacementDefault = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    private val springButtonReduced = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    private val springItemReduced = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    private val springItemPlacementReduced = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val springButton: FiniteAnimationSpec<Float>
        get() = if (reducedMotionEnabled) springButtonReduced else springButtonDefault

    val springItem: FiniteAnimationSpec<Float>
        get() = if (reducedMotionEnabled) springItemReduced else springItemDefault

    val springItemPlacement: FiniteAnimationSpec<IntOffset>
        get() = if (reducedMotionEnabled) springItemPlacementReduced else springItemPlacementDefault
}
