package com.example.timeapk.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import com.example.timeapk.ui.theme.RetroDarkAccent
import com.example.timeapk.ui.theme.RetroDarkBackground
import com.example.timeapk.ui.theme.RetroDarkPrimary
import com.example.timeapk.ui.theme.RetroDarkSecondary
import com.example.timeapk.ui.theme.RetroDarkSurface
import com.example.timeapk.ui.theme.RetroDarkTextOnBG
import com.example.timeapk.ui.theme.RetroDarkTextOnSurface
import com.example.timeapk.ui.theme.RetroLightAccent
import com.example.timeapk.ui.theme.RetroLightBackground
import com.example.timeapk.ui.theme.RetroLightPrimary
import com.example.timeapk.ui.theme.RetroLightSecondary
import com.example.timeapk.ui.theme.RetroLightSurface
import com.example.timeapk.ui.theme.RetroLightTextOnBG
import com.example.timeapk.ui.theme.RetroLightTextOnSurface

// 港式复古 (Hong Kong Retro) 主题配置 V4
// 核心修复：primary ≠ surfaceVariant → primary 统一为砖红交互色
private val DarkColorScheme = darkColorScheme(
    primary = RetroDarkPrimary,                  // #D65C5C 砖红 (交互色：按钮、链接、选中态)
    secondary = RetroDarkSecondary,              // #3F6987 钢蓝
    tertiary = RetroDarkAccent,                  // #FDF4DE 奶油 (FAB，与卡片呼应)
    background = RetroDarkBackground,            // #141622 深海蓝黑
    surface = RetroDarkBackground,               // 全局表面保持深色，避免大面积亮瞎眼
    surfaceVariant = RetroDarkSurface,           // #FDF4DE 奶油色卡片
    onPrimary = Color.White,                     // 白字在砖红上 (高对比)
    onSecondary = Color.White,
    onTertiary = RetroDarkTextOnSurface,         // #141622 深色文字在奶油 FAB 上
    onBackground = RetroDarkTextOnBG,            // #BDB4BF 浅灰在深色背景上
    onSurface = RetroDarkTextOnBG,
    onSurfaceVariant = RetroDarkTextOnSurface,   // #141622 深色文字在奶油卡片上
    outline = RetroDarkSecondary.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = RetroLightAccent,                  // #D65C5C 砖红 (交互色，与深色模式一致)
    secondary = RetroLightSecondary,             // #AFC8DA 褪色丹宁
    tertiary = RetroLightPrimary,                // #47709B 海蓝 (FAB，与卡片呼应但稍浅)
    background = RetroLightBackground,           // #FEFFFF 冷瓷白
    surface = RetroLightBackground,              // 全局表面保持白色
    surfaceVariant = RetroLightSurface,          // #355D82 深海蓝卡片 (加深以提高白字对比)
    onPrimary = Color.White,                     // 白字在砖红上
    onSecondary = RetroLightTextOnBG,            // 深色文字在褪色丹宁上
    onTertiary = Color.White,                    // 白字在海蓝 FAB 上
    onBackground = RetroLightTextOnBG,           // #1A232C 深蓝墨色在白色背景上
    onSurface = RetroLightTextOnBG,
    onSurfaceVariant = RetroLightTextOnSurface,  // #FEFFFF 白色文字在深蓝卡片上
    outline = RetroLightTextOnBG.copy(alpha = 0.2f) // 深色描边，在白底和蓝卡上均可见
)

/** @param themeMode 0=跟随系统 1=浅色 2=深色 */
@Composable
fun TimeAPKTheme(
    themeMode: Int = 0,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val resolvedDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> darkTheme
    }
    val colorScheme = when {
        // 动态配色仅用于浅色主题，深色始终使用自定义 Cinematic Dark Glass
        dynamicColor && !resolvedDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (resolvedDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        resolvedDark -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 状态栏使用与背景一致的色彩，营造沉浸感
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !resolvedDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
