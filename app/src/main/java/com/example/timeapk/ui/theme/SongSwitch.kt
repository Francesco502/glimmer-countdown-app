package com.example.timeapk.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TrackWidth = 40.dp
private val TrackHeight = 22.dp
private val ThumbSize = 16.dp
private val ThumbInset = 3.dp

/**
 * 宋韵开关的视觉部分：方角轨道与方形滑块。开启为朱红实底，关闭为细描边。
 * 语义（角色、状态描述）由调用方的 toggleable 修饰符提供。
 */
@Composable
fun SongSwitchVisual(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val primary = MaterialTheme.colorScheme.primary
    val trackColor by animateColorAsState(
        targetValue = if (checked) primary else Color.Transparent,
        animationSpec = AnimationSpecs.smallTweenColor(),
        label = "songSwitchTrack"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) {
            ColorContrastGuardrail.ensureReadableText(MaterialTheme.colorScheme.onPrimary, primary)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = AnimationSpecs.smallTweenColor(),
        label = "songSwitchThumb"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - ThumbSize - ThumbInset * 2 else 0.dp,
        animationSpec = AnimationSpecs.smallTweenDp(),
        label = "songSwitchOffset"
    )
    val shape = RoundedCornerShape(SongDesignTokens.RadiusSm.dp)
    Box(
        modifier = modifier
            .size(TrackWidth, TrackHeight)
            .background(trackColor.copy(alpha = if (enabled) trackColor.alpha else trackColor.alpha * 0.4f), shape)
            .border(
                BorderStroke(
                    1.dp,
                    if (checked) primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ),
                shape
            )
            .padding(ThumbInset),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(ThumbSize)
                .background(thumbColor, RoundedCornerShape(3.dp))
        )
    }
}
