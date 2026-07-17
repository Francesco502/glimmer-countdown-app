# Widget Preview Size Guidance Design

**Status:** Approved design option A; awaiting written-spec review.

**Goal:** Make it explicit that the width and height controls in widget settings change only the in-app preview proportions. Users resize the actual home-screen widget through their launcher.

## Context

The widget editor labels the controls as “预览宽度” and “预览高度”, but it does not explain why changing them does not resize an already placed launcher widget. This can make correct behavior look like a broken size setting.

The existing widget configuration, preview, and launcher-resize behavior are correct and remain unchanged. This change adds guidance only.

## Copy

- Simplified Chinese and the default Chinese resource:
  `这里只调整预览比例，不会改变桌面小组件的实际尺寸。实际尺寸请在桌面拖动小组件边框调整。`
- English:
  `These controls only adjust the preview proportions. Resize the actual widget from your launcher.`

The message uses one dedicated string resource with matching entries in the default, Simplified Chinese, and English resource sets.

## Placement and Visual Treatment

Place one helper-text node immediately after both preview width and preview height option groups, before display density. Keeping a single message after the pair makes it clear that it applies to both controls without repeating a long explanation.

Use the current settings design system:

- `MaterialTheme.typography.bodySmall`
- `MaterialTheme.colorScheme.onSurfaceVariant`
- existing section spacing and horizontal alignment

Do not introduce a new component, icon, background, divider, or warning treatment. This is explanatory guidance, not an error or destructive-action warning.

## Accessibility and Reflow

The helper remains a normal readable text node in the accessibility tree and does not receive a button, switch, or alert role. It wraps naturally within the existing scrollable settings section. At 150% system font size, the full message and the following display-density controls must remain reachable without clipping or overlap.

## Testing and Acceptance

Implementation is accepted when:

1. Tests first prove that the default, Simplified Chinese, and English resources contain the approved guidance and that the widget editor renders that resource after the height options and before density.
2. Targeted unit/source tests pass after implementation.
3. Current-run emulator inspection at 100% and 150% system font confirms readable wrapping, correct visual hierarchy, no clipping, and continued access to the following controls.
4. The existing widget configuration, multi-instance isolation, sorting, and full release gates remain green.

## Non-Goals

- Changing the actual launcher widget size or resize mode.
- Changing preview dimension calculations or the 1–5 cell options.
- Migrating or altering widget configuration storage.
- Adding a launcher-specific resize tutorial or onboarding flow.
- Redesigning the widget settings screen.
