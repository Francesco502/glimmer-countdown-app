package com.example.timeapk.ui.theme

import androidx.compose.ui.graphics.Color

// Apple Human Interface Guidelines 风格系统色
// 参考 iOS 系统色：蓝 / 红 / 绿 / 橙 / 灰
val SystemBlue = Color(0xFF007AFF)
val SystemRed = Color(0xFFFF3B30)
val SystemGreen = Color(0xFF34C759)
val SystemOrange = Color(0xFFFF9500)
val SystemGray = Color(0xFF8E8E93)

// 背景与分组背景（Light / Dark）
val BackgroundLight = Color(0xFFF2F2F7)        // iOS grouped background light
val BackgroundDark = Color(0xFF000000)        // 纯黑背景，适配 OLED 风格
val SurfaceLight = Color(0xFFFFFFFF)          // 卡片 / 列表单元白色
val SurfaceDark = Color(0xFF1C1C1E)           // iOS grouped background dark primary
val SurfaceDarkElevated = Color(0xFF2C2C2E)   // 提升卡片背景

// 分隔线 / 描边
val Separator = Color(0xFFE5E5EA)

// 文本色（Label）
// Primary Label: #000000
// Secondary: #3C3C43 (60%) -> 0x993C3C43
// Tertiary: #3C3C43 (30%) -> 0x4D3C3C43
val LabelPrimary = Color(0xFF000000)
val LabelSecondary = Color(0x993C3C43)
val LabelTertiary = Color(0x4D3C3C43)

// 兼容此前深色主题的一些色值（给旧代码复用）
val Black80 = Color(0xFF1C1C1E)
val BlackBackground = BackgroundDark
val Gold = Color(0xFFD4AF37)
val White = SurfaceLight
val GrayText = SystemGray
val CardBackground = SurfaceDarkElevated

// Cinematic Dark Glass 专用色
val CinematicBackground = Color(0xFF050505)
val CinematicBackgroundDeep = Color(0xFF000000)
val GlassSurfaceThin = Color(0x0AFFFFFF)
val GlassSurface = Color(0x14FFFFFF)
val GlassSurfaceStrong = Color(0x1EFFFFFF)
val GlassBorder = Color(0x26FFFFFF)
val GlassHighlight = Color(0x40FFFFFF)
val TextOnDarkPrimary = Color(0xFFFFFFFF)
val TextOnDarkSecondary = Color(0x99FFFFFF)
val TextOnDarkTertiary = Color(0x66FFFFFF)

// 港式复古 (Hong Kong Retro) 色板 V4 - 修复对比度 & primary/surfaceVariant 冲突
// Dark Mode: 维港夜雾 (黑底 亮卡)
val RetroDarkBackground = Color(0xFF141622)      // 深海蓝黑
val RetroDarkSurface = Color(0xFFFDF4DE)         // 奶油杏色 (高亮卡片)
val RetroDarkPrimary = Color(0xFFD65C5C)         // 复古砖红 (交互强调色，与 surfaceVariant 区分)
val RetroDarkSecondary = Color(0xFF3F6987)       // 钢蓝
val RetroDarkAccent = Color(0xFFFDF4DE)          // 奶油色 (用于 FAB 等强调面)
val RetroDarkTextOnBG = Color(0xFFBDB4BF)        // 背景上的文字：浅灰
val RetroDarkTextOnSurface = Color(0xFF141622)   // 卡片上的文字：深蓝黑 (因为卡片变亮了)

// Light Mode: 蓝白瓷砖 (白底 深卡)
val RetroLightBackground = Color(0xFFFEFFFF)     // 冷瓷白
val RetroLightSurface = Color(0xFF355D82)        // 深海蓝 (加深卡片底色，白字对比度 ≥6:1)
val RetroLightPrimary = Color(0xFF47709B)        // 海蓝 (FAB / 装饰色)
val RetroLightSecondary = Color(0xFFAFC8DA)      // 褪色丹宁
val RetroLightAccent = Color(0xFFD65C5C)         // 砖红 (交互强调色，与 surfaceVariant 区分)
val RetroLightTextOnBG = Color(0xFF1A232C)       // 背景上的文字：深蓝墨色
val RetroLightTextOnSurface = Color(0xFFFEFFFF)  // 卡片上的文字：纯白 (因为卡片变深了)

// 宋代工笔画 (Song Dynasty Gongbi) 色板
// Light Mode: 宣纸、绢本、石色、水色
val SongLightBackground = Color(0xFFF9F7F2)      // 宣纸 (Rice Paper) - 更纯净的暖白，提升通透感
val SongLightSurface = Color(0xFFFFFFFF)         // 留白 (White Space) - 纯白卡片，通过微弱的边框或阴影区分
val SongLightPrimary = Color(0xFF8E354A)         // 胭脂 (Rouge) - 沉稳的深红，不刺眼
val SongLightSecondary = Color(0xFF576E6A)       // 石绿 (Mineral Green) - 雅致的绿
val SongLightTertiary = Color(0xFF7A7A7A)        // 淡墨 (Pale Ink) - 中性灰
val SongLightTextOnBG = Color(0xFF1F1F1F)        // 焦墨 (Charcoal Ink) - 极深灰黑，保证阅读清晰度
val SongLightTextOnSurface = Color(0xFF1F1F1F)   // 焦墨

// Dark Mode: 沉墨、金箔、矿物色
val SongDarkBackground = Color(0xFF101012)       // 漆黑 (Lacquer Black) - 更有深度的黑
val SongDarkSurface = Color(0xFF1C1C1E)          // 墨锭 (Ink Stick)
val SongDarkPrimary = Color(0xFFC7B398)          // 泥金 (Gold Paste) - 降低饱和度，更显高级
val SongDarkSecondary = Color(0xFF6B8E85)        // 黛绿 (Dark Green)
val SongDarkTertiary = Color(0xFF8C4B47)         // 赭石 (Ocher)
val SongDarkTextOnBG = Color(0xFFD9D9D9)         // 银灰 (Silver Grey) - 柔和的白
val SongDarkTextOnSurface = Color(0xFFEBEBEB)    // 霜白 (Frost White)


// 旧的紫色系仍保留，防止某些局部引用
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
