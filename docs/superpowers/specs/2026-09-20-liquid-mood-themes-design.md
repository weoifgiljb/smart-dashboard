# Design: Liquid Natural Mood Themes (池核 / 海洋核 / 水晶核 / 雨核)

**Date:** 2026-09-20  
**Project:** 自律组件 (`frontend/`, Vite + Vue 3 + Element Plus)  
**Goal:** Reduce generic "AI dashboard" look; keep layout; add switchable liquid-natural atmospheres.

## Problem

Current UI reads as template SaaS / AI dashboard:

- Emerald (`#10b981`) + blue dual gradient on logo and primary CTA
- Fixed slate sidebar (`#0f172a`)
- Inter + soft gray cards + generic KPI icon chips
- Only `light` / `dark` via `data-theme`

## Decisions (approved)

| Topic | Choice |
| --- | --- |
| Intensity | Medium: palettes + soft glass + liquid gradients; layout unchanged |
| Theme model | 4 moods × light/dark |
| Implementation | Semantic CSS tokens; `data-mood` + `data-theme` |
| Default | Mood `ocean`; theme follows system (existing light/dark behavior) |
| Persistence | `localStorage`: `mood` + `theme` |

Moods:

1. **池核 (`pool`)** — moss green + still-water teal; soft surface glow
2. **海洋核 (`ocean`)** — deep ocean blue + foam cyan; tidal gradient
3. **水晶核 (`crystal`)** — lilac crystal + cool gray refraction; bright glass
4. **雨核 (`rain`)** — rain slate + mist blue; wet low contrast

## Non-goals

- No layout / IA / route changes
- No full-bleed water/rain texture videos or heavy canvas effects
- No redesign of individual feature pages beyond shared tokens and glass recipe
- No new design-system package outside existing `styles/` + `useTheme`

## Architecture

### Attributes

On `document.documentElement`:

- `data-theme`: `light` | `dark` (existing)
- `data-mood`: `pool` | `ocean` | `crystal` | `rain` (new)

Selectors compose, e.g. `[data-mood='ocean'][data-theme='dark']`.

### Token layers

1. **Primitives** (per mood, in `palette.less` or `moods.less`): hue ramps for primary / secondary / surface water / mist.
2. **Semantic** (in `theme.less`): map to existing `--color-*`, plus new glass / liquid tokens:
   - `--color-bg`, `--color-bg-elevated`, `--color-bg-muted`
   - `--color-text*`, `--color-border`
   - `--color-primary*`, `--color-secondary`, status colors
   - `--sidebar-local-*` (mood-tinted; no fixed slate)
   - `--gradient-primary` (single-family liquid gradient, not emerald×blue)
   - `--glass-bg`, `--glass-border`, `--glass-blur`, `--glass-shadow`
3. **Component recipes** (small CSS in `global.less` / layout):
   - `.app-card` / `.el-card`: glass elevated surface
   - Logo title: solid primary or very soft mono-family gradient (no dual SaaS clip)
   - Primary buttons / KPI accents: use `--gradient-primary` sparingly

Charts keep reading `chartPalette()` / `cssVar('--color-*')` — they pick up mood automatically once tokens change.

### Runtime API

Extend `composables/useTheme.ts`:

```ts
export type ThemeName = 'light' | 'dark'
export type MoodName = 'pool' | 'ocean' | 'crystal' | 'rain'

applyTheme(theme: ThemeName)
applyMood(mood: MoodName)
resolveInitialTheme(): ThemeName  // existing storage + prefers-color-scheme
resolveInitialMood(): MoodName    // storage, default 'ocean'
```

`main.ts` (or existing boot) calls both resolvers once on startup.

### UI chrome

`MainLayout.vue` header:

- Keep light/dark switch
- Add mood select: 池核 / 海洋核 / 水晶核 / 雨核 (labels Chinese, values English keys)
- Sidebar colors bind to `--sidebar-local-*` which update with mood
- Active menu: `--color-primary-soft` background + primary text

## Visual rules (medium intensity)

- Glass: semi-transparent elevated fill + `backdrop-filter: blur(...)` + 1px soft border; blur disabled / reduced under `prefers-reduced-motion` or when unsupported (fallback solid elevated)
- Gradients: same hue family (e.g. blue→cyan), never complementary neon pairs
- Contrast: body text and controls must remain WCAG AA against surfaces in both light and dark for every mood
- Motion: optional very subtle background wash via CSS gradient only; no particle rain

## File changes (expected)

| File | Change |
| --- | --- |
| `src/styles/palette.less` | Mood primitive ramps |
| `src/styles/theme.less` | Mood × theme semantic maps + glass tokens |
| `src/styles/global.less` | Glass card recipe; title treatment |
| `src/composables/useTheme.ts` | Mood types, apply/resolve, storage |
| `src/layouts/MainLayout.vue` | Mood picker; sidebar/logo tweaks |
| `src/utils/themeTokens.ts` | Optional: expose glass/chart helpers if needed |
| tests | Unit: resolve/apply mood+theme; optional snapshot of token presence |

Out of scope unless tokens break them: individual `views/*` one-off colors. Prefer fixing by replacing hardcoded hex with `var(--color-*)`.

## Data flow

1. User picks mood or toggles theme
2. `applyMood` / `applyTheme` set attributes + `localStorage`
3. CSS variables recompute
4. Element Plus bridge (`element-bridge.less`) and charts already consume `--color-*`

## Error / edge handling

- Invalid stored mood/theme → fall back to `ocean` / system theme
- `backdrop-filter` unsupported → solid `--color-bg-elevated`
- SSR/tests without `document` → no-op apply helpers (guard as today)

## Testing

- Unit: `resolveInitialMood` / `applyMood` set attribute + storage
- Unit: `resolveInitialTheme` still respects storage and `prefers-color-scheme`
- Manual: switch all 4×2 combinations on Dashboard; check sidebar, cards, primary button, charts
- No e2e required for first ship unless existing e2e asserts theme classes — update if they break

## Success criteria

- User can switch among 池核 / 海洋核 / 水晶核 / 雨核 and light/dark independently
- Choice persists across reload
- Layout structure unchanged
- Emerald+blue SaaS gradient logo / primary look is gone
- Cards read as soft glass liquid surfaces under medium intensity

## Implementation order

1. Token primitives + semantic maps for 4×2
2. Glass recipes + logo/sidebar cleanup
3. `useTheme` mood API + boot wiring
4. Mood picker in header
5. Sweep hardcoded emerald/blue in shared UI if any remain
6. Tests + manual 4×2 pass
