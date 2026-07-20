# 首页卡片满宽布局 Design QA

- Source visual truth: `/var/folders/xk/m2w8r0dj2bb3gzpy0qmv032r0000gn/T/codex-clipboard-2a803bcc-4900-444a-ad46-f17034e15093.png`
- Implementation screenshot: `/tmp/timeapk-card-full-width-final.png`
- Focused implementation crop: `/tmp/timeapk-card-full-width-final-crop.png`
- Viewport: 1172 × 2748 px; focused card comparison crop 1172 × 432 px
- State: Chinese, light theme, card view, custom ordering enabled, one anonymous event

## Full-view comparison evidence

The final runtime screenshot was captured from the Android emulator after installing the rebuilt Play Debug package. The card bounds are `[48,468][1124,816]`, which gives equal 48 px left and right margins. The former dedicated reorder node and its separate right-side rail are absent from the final UI hierarchy.

## Focused comparison evidence

The supplied problem screenshot and the final focused crop were opened together in one comparison input. A separate magnified detail pass was unnecessary because the relevant border, horizontal margins, text, and former icon area are all clearly visible in the focused crops.

The date and date-delta values differ because the final screenshot uses a newly created anonymous event; those values were excluded from fidelity judgment. The title, category, typography treatment, card structure, and target spacing state are directly comparable.

## Findings

- No actionable P0, P1, or P2 mismatch remains.
- Fonts and typography: unchanged from the existing Song-style card; hierarchy, weights, wrapping, and alignment remain intact.
- Spacing and layout rhythm: the card now fills the available content width with equal 48 px outer margins; no blank trailing column remains.
- Colors and visual tokens: existing paper surface, border, accent, and text colors are unchanged.
- Image and asset fidelity: this component contains no raster imagery; the unnecessary line icon was removed instead of replaced.
- Copy and content: event title, date, category, and date-delta content are unchanged.
- Interaction: custom ordering remains available by long-pressing and dragging anywhere on the card; the filtered-order persistence UI test passes.

## Comparison history

- Initial issue: card bounds ended at x=980 while a dedicated reorder node occupied x=980…1124, creating a visibly asymmetric trailing rail.
- Fix: removed the icon, semantic node, 48dp reserved padding, and obsolete strings; moved reordering to whole-card long-press drag.
- Post-fix evidence: card bounds extend from x=48 to x=1124, both margins are 48 px, and the targeted connected gesture test passes.

## Implementation checklist

- [x] Remove the trailing icon and dedicated rail.
- [x] Give cards and list rows the full content width.
- [x] Preserve custom sorting through whole-card long-press drag.
- [x] Verify runtime bounds and semantics.
- [x] Compare the supplied screenshot and final implementation visually.

## Follow-up polish

No P3 follow-up is required for this scoped change.

final result: passed
