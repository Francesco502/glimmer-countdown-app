package com.example.timeapk.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.SongDesignTokens

/**
 * 统一按压反馈：保持轻微尺度变化，主要依靠墨色和细线状态表达按下。
 */
fun Modifier.pressScale(
    scaleDown: Float = SongDesignTokens.PressScaleSubtle,
    interactionSource: MutableInteractionSource? = null
): Modifier {
    return this.then(
        PressScaleModifier(
            scaleDown = scaleDown,
            externalInteractionSource = interactionSource
        )
    )
}

private fun PressScaleModifier(
    scaleDown: Float,
    externalInteractionSource: MutableInteractionSource?
): Modifier = Modifier.graphicsLayer {
    // 实际的动画在 Composable 中完成，这里只是占位以保持 API 简洁
}

@Composable
fun rememberPressScale(
    scaleDown: Float = SongDesignTokens.PressScaleSubtle,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Pair<Modifier, MutableInteractionSource> {
    val isPressed = interactionSource.collectIsPressedAsState().value
    val scale = animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = AnimationSpecs.springButton,
        label = "pressScale"
    ).value
    val modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    return modifier to interactionSource
}
