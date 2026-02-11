# 应用图标与素材说明

本文档说明图标设计规范及如何替换 App 图标，与 `design_plan_hk_retro.md`、`canvas_design_philosophy.md` 的视觉规范一致。

## 设计要点（港式复古 / Harbor Glow）

- **关键词**：情绪、时间、颗粒感、秩序  
- **深色**：维港夜雾 — 深海蓝黑 `#141622`、奶油杏色暖光 `#FDF4DE`、复古砖红 `#D65C5C`  
- **浅色**：蓝白瓷砖 — 冷瓷白 `#FEFFFF`、复古海蓝 `#47709B`、砖红 `#D65C5C`  
- **气质**：港式电影感、胶片/冲印室、霓虹余晖、老黄历/场记板  

## AI 生图 Prompt（可选）

用于 Midjourney、DALL·E、Stable Diffusion 等生成替换用图标素材，生成后裁剪为正方形。

**英文**：
```
App icon, square format, 1024x1024. Hong Kong retro cinema aesthetic, film grain texture, moody and nostalgic. Deep navy blue background (#141622), soft cream or warm ivory light glow (#FDF4DE) as main symbol — abstract "glimmer" or "gathering light" motif. One small accent in brick red (#D65C5C). Flat design, minimal shapes, no text. Slight vignette, subtle film grain overlay.
```

**中文**：
```
应用图标，正方形，1024x1024。港式复古电影感，带轻微胶片颗粒，怀旧、有情绪。背景为深海蓝黑（#141622），主图形为奶油杏色或暖象牙色（#FDF4DE）的一缕光或「拾光」抽象符号。点缀一小块复古砖红色（#D65C5C）。扁平、几何简洁、无文字。整体略带暗角与轻微颗粒。
```

## 如何用作 App 图标

1. **直接替换 drawable**  
   将 PNG 复制到 `app/src/main/res/drawable/`，或在 `AndroidManifest.xml` 中把 `android:icon` / `android:roundIcon` 指向新资源。

2. **多密度 mipmap（发布推荐）**  
   从 1024×1024 导出：mdpi 48、hdpi 72、xhdpi 96、xxhdpi 144、xxxhdpi 192，分别放入 `mipmap-mdpi/` 等，在 Manifest 中设置 `@mipmap/ic_launcher`。

当前工程使用 `app/src/main/res/drawable/ic_launcher.xml` 矢量图标，可按需替换为 PNG 或 mipmap。
