# Song Aesthetic Audit - 2026-07-03

## Scope

- Build: `glimmer-countdown-3-16.apk`
- Device: `sdk_gphone16k_arm64`, Android API 37
- Captured screens:
  - `01-home-card.png`
  - `02-home-list.png`
  - `03-home-month.png`
  - `04-overflow-menu.png`
  - `05-settings.png`
  - `06-settings-reminders.png`
  - `07-event-entry.png`
  - `08-event-entry-reminder.png`
  - `09-detail.png`

## Verdict

The app is broadly aligned with a Song-style direction, especially in color restraint, whitespace, serif typography, thin dividers, and low-motion interaction. It is not yet a fully pure Song aesthetic experience because several surfaces still rely on modern Material patterns: long popup menus, standard toolbar icons, form cards, switches, bottom action bars, and utilitarian status copy.

Estimated fit: 75/100.

## Evidence Notes

1. Home card and list
   - Healthy: large paper-toned blank space, quiet brand wordmark, sparse event rows, thin accent line, and restrained cinnabar highlights.
   - Issue: current test data title `3％1520A％20` breaks the intended literary tone and makes the visual look like debug content.
   - Accessibility risk: right-side time is tappable and has a content description in UI tree, but visual affordance is very subtle.

2. Month calendar
   - Healthy: good balance of grid structure and whitespace; date cells are bounded and no longer consume the whole viewport.
   - Issue: the grid still reads like a modern scheduler. A lighter ink-line treatment or fewer boxed cells would feel more like a handscroll ledger.

3. Overflow menu
   - Healthy: moving timeline shortcuts into a called menu supports calm default composition.
   - Issue: the menu is the least Song-like home element. The long filled panel, shadow, and stacked Material menu density feel like Android system UI, not a paper slip or folding panel.

4. Settings
   - Healthy: this is the strongest Song-style screen. It uses generous row height, thin separators, quiet icons, and clear hierarchy.
   - Issue: icons are still generic Material concepts recolored in cinnabar. They do not yet use white-line, vessel, seal, fan, plum, or ruyi-derived forms.

5. Event entry
   - Healthy: form sections are legible, serif type is consistent, and reminder chips are calmer than the previous wheel.
   - Issue: section cards, outlined inputs, classical toggles, and the fixed bottom save bar create a modern admin-form feel. The screen is functional but less poetic.

6. Detail
   - Healthy: the main panel has strong paper-note presence, centered date/lunar copy, and generous vertical rhythm.
   - Issue: the error strip copy (`No writable calendar`) and bottom action toolbar are utilitarian. They interrupt the otherwise quiet detail composition.

## Code Notes

- Color basis is Song-aligned: paper, ink, seal, celadon, jade, and gold tokens are defined in `SongDesignTokens.kt`.
- Typography is Song-aligned: Noto Serif SC and ZCOOL XiaoWei are bundled, with zero letter spacing and light serif body options in `Type.kt`.
- Motion is partially aligned: fade/vertical slide and no-bouncy springs are used, with reduced-motion support. It is still more "soft app motion" than "ink dissolve" or "handscroll unfold".
- Component primitives are mostly restrained: `SongModeTabRow`, `SongFilterChip`, `SongCalendarCell`, `SongPaperSurface`, and thin dividers match the current direction.
- Sound design is not implemented. No raw audio or click sound layer was found.

## Priority Gaps

1. Replace generic Material icons with a small Song-style icon set.
2. Redesign the overflow menu as a lighter paper-slip action panel with less shadow and less filled area.
3. Introduce subtle paper/celadon texture only in large surfaces, at very low opacity.
4. Make form screens less card-heavy by using section rhythm, ink dividers, and fewer framed boxes.
5. Rewrite system feedback copy to reduce hard utilitarian wording where safe.
6. Add optional low-volume natural / instrument-inspired sound only if it can be disabled.

## Limits

- Screenshots cannot prove full motion quality, audio quality, screen-reader flow, or all dark-theme states.
- The audit used existing simulator data; debug-like event titles should be replaced with realistic sample data before final visual review.
