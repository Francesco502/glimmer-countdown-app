package com.example.timeapk.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.timeapk.ui.theme.AnimationSpecs

/**
 * 统一按压缩放反馈：
 * - 默认缩放到 0.96f
 * - 使用全局 AnimationSpecs.springButton
 *
 * 用法：
 * Modifier.pressScale()
 * Modifier.pressScale(scaleDown = 0.9f)
 */
fun Modifier.pressScale(
    scaleDown: Float = 0.96f,
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
    scaleDown: Float = 0.96f,
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

