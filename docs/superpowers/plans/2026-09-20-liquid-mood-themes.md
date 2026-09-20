# Liquid Mood Themes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the emerald/blue AI-dashboard look of 自律组件 with four liquid-natural moods (池核 / 海洋核 / 水晶核 / 雨核) × light/dark, soft glass surfaces, and importable SVG/texture/icon materials — without changing layout.

**Architecture:** Keep existing CSS-variable theme system. Add `data-mood` beside `data-theme` on `document.documentElement`. Semantic tokens in `theme.less` cover all 4×2 combinations. `useTheme` owns apply/resolve/persistence. Decorative assets live under `src/assets/moods/` and are selected via CSS variables / mood classes. Medium intensity only (no particle/video effects).

**Tech Stack:** Vue 3 + Vite + TypeScript + LESS + Element Plus + Pinia + Vitest (existing frontend).

## Global Constraints

- Intensity: medium — palettes + soft glass + liquid gradients; layout unchanged
- Moods: `pool` | `ocean` | `crystal` | `rain` (UI labels: 池核 / 海洋核 / 水晶核 / 雨核)
- Default: mood `ocean`; theme follows system via existing `resolveInitialTheme`
- Persistence keys: `localStorage` `mood` + `theme`
- Gradients: single hue family only — never emerald×blue dual SaaS gradient
- Materials: SVG decorations + transparent texture PNGs + icon set; CC0/original + `SOURCES.md`
- Decorative opacity typically 0.06–0.18; never block text/controls
- Paths relative to repo root `仪表盘迭代/`; frontend code under `frontend/`
- Do not commit unrelated dirty test files already in the working tree

---

## File map

| File | Responsibility |
| --- | --- |
| `frontend/src/composables/useTheme.ts` | Theme + mood apply/resolve/storage |
| `frontend/src/composables/useTheme.spec.ts` | Unit tests for theme/mood |
| `frontend/src/main.ts` | Boot: apply initial theme + mood |
| `frontend/src/styles/palette.less` | Mood primitive color ramps |
| `frontend/src/styles/theme.less` | Semantic `--color-*` + glass tokens for 4×2 |
| `frontend/src/styles/global.less` | Glass card recipe; mood wash hook |
| `frontend/src/layouts/MainLayout.vue` | Mood picker; logo/sidebar cleanup |
| `frontend/src/assets/moods/**` | SVG motifs, textures, icons + `SOURCES.md` |
| `frontend/src/components/ui/EmptyState.vue` | Optional mood icon slot |
| `frontend/src/utils/themeTokens.ts` | Keep `cssVar` / `chartPalette` (no break) |

---

### Task 1: Mood API + unit tests

**Files:**
- Create: `frontend/src/composables/useTheme.spec.ts`
- Modify: `frontend/src/composables/useTheme.ts`
- Test: `frontend/src/composables/useTheme.spec.ts`

**Interfaces:**
- Consumes: existing `ThemeName`, `applyTheme`, `resolveInitialTheme`
- Produces:
  - `export type MoodName = 'pool' | 'ocean' | 'crystal' | 'rain'`
  - `export function applyMood(mood: MoodName): void`
  - `export function resolveInitialMood(): MoodName`
  - `useTheme()` also returns `{ mood, applyMood, setMood }` where `setMood` applies and updates ref

- [ ] **Step 1: Write the failing test**

```ts
// frontend/src/composables/useTheme.spec.ts
import { beforeEach, describe, expect, it } from 'vitest'
import {
  applyMood,
  applyTheme,
  resolveInitialMood,
  resolveInitialTheme,
  type MoodName,
} from './useTheme'

describe('useTheme mood', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.removeAttribute('data-mood')
    document.documentElement.classList.remove('dark')
  })

  it('applyMood sets data-mood and persists', () => {
    applyMood('pool')
    expect(document.documentElement.getAttribute('data-mood')).toBe('pool')
    expect(localStorage.getItem('mood')).toBe('pool')
  })

  it('resolveInitialMood defaults to ocean', () => {
    expect(resolveInitialMood()).toBe('ocean')
  })

  it('resolveInitialMood reads storage and ignores invalid', () => {
    localStorage.setItem('mood', 'ocean')
    expect(resolveInitialMood()).toBe('ocean')
    localStorage.setItem('mood', 'nope')
    expect(resolveInitialMood()).toBe('ocean')
  })

  it('applyTheme still toggles dark class', () => {
    applyTheme('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    expect(resolveInitialTheme()).toBe('dark')
  })

  it('accepts all MoodName values', () => {
    const moods: MoodName[] = ['pool', 'ocean', 'crystal', 'rain']
    for (const m of moods) {
      applyMood(m)
      expect(document.documentElement.getAttribute('data-mood')).toBe(m)
    }
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run (from `frontend/`): `npx vitest run src/composables/useTheme.spec.ts`

Expected: FAIL — `applyMood` / `resolveInitialMood` not exported.

- [ ] **Step 3: Write minimal implementation**

Replace `frontend/src/composables/useTheme.ts` with:

```ts
import { onMounted, onBeforeUnmount, ref } from 'vue'

export type ThemeName = 'light' | 'dark'
export type MoodName = 'pool' | 'ocean' | 'crystal' | 'rain'

const MOODS: MoodName[] = ['pool', 'ocean', 'crystal', 'rain']

export function applyTheme(theme: ThemeName) {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('data-theme', theme)
  document.documentElement.classList.toggle('dark', theme === 'dark')
  localStorage.setItem('theme', theme)
}

export function applyMood(mood: MoodName) {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('data-mood', mood)
  localStorage.setItem('mood', mood)
}

export function resolveInitialTheme(): ThemeName {
  const stored = localStorage.getItem('theme') as ThemeName | null
  if (stored === 'light' || stored === 'dark') return stored
  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
  return prefersDark ? 'dark' : 'light'
}

export function resolveInitialMood(): MoodName {
  const stored = localStorage.getItem('mood')
  if (stored && (MOODS as string[]).includes(stored)) return stored as MoodName
  return 'ocean'
}

export function useTheme() {
  const isDark = ref(document.documentElement.getAttribute('data-theme') === 'dark')
  const mood = ref<MoodName>(
    (document.documentElement.getAttribute('data-mood') as MoodName) || resolveInitialMood(),
  )

  const syncFromDom = () => {
    isDark.value = document.documentElement.getAttribute('data-theme') === 'dark'
    const m = document.documentElement.getAttribute('data-mood') as MoodName | null
    if (m && (MOODS as string[]).includes(m)) mood.value = m
  }

  const toggleTheme = () => {
    applyTheme(isDark.value ? 'light' : 'dark')
    isDark.value = !isDark.value
  }

  const setMood = (next: MoodName) => {
    applyMood(next)
    mood.value = next
  }

  onMounted(() => {
    window.addEventListener('storage', syncFromDom)
  })
  onBeforeUnmount(() => {
    window.removeEventListener('storage', syncFromDom)
  })

  return { isDark, toggleTheme, applyTheme, mood, setMood, applyMood }
}
```

Note: fix toggle so it applies the *next* theme correctly — when `isDark` is true, next is light:

```ts
  const toggleTheme = () => {
    const next: ThemeName = isDark.value ? 'light' : 'dark'
    applyTheme(next)
    isDark.value = next === 'dark'
  }
```

(Existing code had a likely invert bug; keep behavior matching current UI switch: `v-model="isDark"` `@change="toggleTheme"` — if switch already flipped `isDark`, prefer reading the new value. Safer pattern for Element Plus switch:)

In MainLayout later, use `@change` with explicit theme from `isDark` after v-model updates, OR change toggle to:

```ts
  const setDark = (dark: boolean) => {
    applyTheme(dark ? 'dark' : 'light')
    isDark.value = dark
  }
```

and bind `@change="setDark"` — do this in Task 4 when touching the layout. For Task 1 keep `toggleTheme` as:

```ts
  const toggleTheme = () => {
    const next: ThemeName = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark'
    applyTheme(next)
    isDark.value = next === 'dark'
  }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npx vitest run src/composables/useTheme.spec.ts`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/composables/useTheme.ts frontend/src/composables/useTheme.spec.ts
git commit -m "feat(theme): add mood apply/resolve API with tests"
```

---

### Task 2: Boot mood on startup

**Files:**
- Modify: `frontend/src/main.ts`
- Test: covered by Task 1 unit tests; manual smoke after Task 4

**Interfaces:**
- Consumes: `applyMood`, `resolveInitialMood` from `./composables/useTheme`
- Produces: `data-mood` set before `app.mount`

- [ ] **Step 1: Update main.ts imports and boot**

Change the theme import/boot block to:

```ts
import {
  applyMood,
  applyTheme,
  resolveInitialMood,
  resolveInitialTheme,
} from './composables/useTheme'

// ... keep other setup ...

applyTheme(resolveInitialTheme())
applyMood(resolveInitialMood())

app.mount('#app')
```

- [ ] **Step 2: Typecheck**

Run: `npm run typecheck`  
Expected: PASS (or only pre-existing unrelated errors)

- [ ] **Step 3: Commit**

```bash
git add frontend/src/main.ts
git commit -m "feat(theme): apply initial mood on app boot"
```

---

### Task 3: Palette + semantic tokens (4×2) + glass variables

**Files:**
- Modify: `frontend/src/styles/palette.less`
- Modify: `frontend/src/styles/theme.less`
- Test: manual / screenshot after Task 4; optional CSS presence check not required

**Interfaces:**
- Consumes: existing `--color-*` contract used by `global.less`, `element-bridge.less`, charts
- Produces: all moods define full semantic set; new `--glass-*`, `--mood-wash-opacity`; `--gradient-primary` mono-family; `--sidebar-local-*` mood-tinted

- [ ] **Step 1: Rewrite `palette.less` primitives**

Keep light/dark neutrals; replace single primary with mood ramps:

```less
// Compile-time palette only. Runtime UI must use var(--color-*).

@bg-light: #f4f7f8;
@bg-elevated-light: #ffffff;
@bg-muted-light: #e8eef1;
@text-light: #12202a;
@text-secondary-light: #3d5564;
@text-muted-light: #7a90a0;
@border-light: #d7e2e8;

@bg-dark: #0c1419;
@bg-elevated-dark: #142028;
@bg-muted-dark: #1a2830;
@text-dark: #e8f1f4;
@text-secondary-dark: #b3c5cf;
@text-muted-dark: #7d93a1;
@border-dark: #243440;

// Mood primaries (light / dark)
@pool-primary-l: #3d8b6e; @pool-primary-d: #5ec4a0; @pool-secondary-l: #2a6f7a; @pool-secondary-d: #4db8c4;
@ocean-primary-l: #2b6ea8; @ocean-primary-d: #5eb0e0; @ocean-secondary-l: #2a9bb0; @ocean-secondary-d: #5ed0d8;
@crystal-primary-l: #7b6bb5; @crystal-primary-d: #b5a4e8; @crystal-secondary-l: #6a7fad; @crystal-secondary-d: #a8b8e0;
@rain-primary-l: #5a7385; @rain-primary-d: #8eabbc; @rain-secondary-l: #5b7f96; @rain-secondary-d: #9bc0d4;

@radius-sm: 10px;
@radius-md: 16px;
@radius-lg: 24px;
```

- [ ] **Step 2: Structure `theme.less` with shared maps + mood overrides**

Keep `:root` as **ocean light** defaults (matches default mood). Add `[data-mood='…']` blocks that override primary/secondary/sidebar/gradient/glass. Nest or duplicate `[data-theme='dark']` / mood×dark.

Concrete approach (clear, no LESS mixin magic required):

1. `:root` / `[data-mood='ocean']` — ocean light tokens (full semantic block as today + glass)
2. `[data-mood='pool']`, `[data-mood='crystal']`, `[data-mood='rain']` — light overrides for primary/secondary/soft/sidebar/gradient/glass/bg tint
3. `[data-theme='dark']` and each `[data-mood='…'][data-theme='dark']` — dark counterparts
4. Keep `prefers-color-scheme` fallback only when `data-theme` absent — apply ocean dark (or respect mood attribute if present)

Glass tokens on every combination:

```less
--glass-bg: rgba(255, 255, 255, 0.62);
--glass-border: rgba(255, 255, 255, 0.45);
--glass-blur: 14px;
--glass-shadow: 0 8px 28px rgba(15, 40, 60, 0.08);
--mood-wash-opacity: 0.12;
```

Dark glass example:

```less
--glass-bg: rgba(20, 32, 40, 0.72);
--glass-border: rgba(255, 255, 255, 0.08);
--glass-blur: 16px;
--glass-shadow: 0 10px 32px rgba(0, 0, 0, 0.35);
--mood-wash-opacity: 0.14;
```

Ocean light primary example:

```less
--color-primary: @ocean-primary-l;
--color-primary-hover: darken(@ocean-primary-l, 6%);
--color-primary-rgb: 43, 110, 168;
--color-primary-soft: rgba(43, 110, 168, 0.14);
--color-secondary: @ocean-secondary-l;
--gradient-primary: linear-gradient(135deg, @ocean-primary-l 0%, @ocean-secondary-l 100%);
--sidebar-local-bg: #0e2a3d;
--sidebar-local-text: #e5f4fa;
--sidebar-local-text-secondary: #9bb8c8;
--sidebar-local-border: #1a3d52;
```

Pool / crystal / rain: same shape with their `@*-primary-l` etc. Dark: use `@*-primary-d` and deeper sidebar water colors.

Also retune `--color-bg` slightly per mood (cool green-gray for pool, blue-gray ocean, lilac-gray crystal, slate-gray rain) — keep elevated readable.

Status colors (success/warning/danger/info) may stay shared neutrals; optionally tint `success` toward mood primary for pool/ocean only — keep shared for YAGNI.

- [ ] **Step 3: Manual sanity**

With Vite already on `:3000`, set in DevTools:

```js
document.documentElement.setAttribute('data-mood','pool')
document.documentElement.setAttribute('data-theme','dark')
```

Confirm CSS variables update in Computed styles.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/palette.less frontend/src/styles/theme.less
git commit -m "feat(theme): add pool/ocean/crystal/rain × light/dark tokens"
```

---

### Task 4: Glass recipes + logo + mood picker UI

**Files:**
- Modify: `frontend/src/styles/global.less`
- Modify: `frontend/src/layouts/MainLayout.vue`

**Interfaces:**
- Consumes: `useTheme().mood`, `setMood`, `isDark`, theme setters; `--glass-*`, `--gradient-primary`
- Produces: header mood `<el-select>`; glass cards; mono logo color

- [ ] **Step 1: Glass + wash hooks in `global.less`**

Append/update:

```less
.el-card,
.app-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
}

@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .el-card,
  .app-card {
    background: var(--color-bg-elevated);
    backdrop-filter: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .el-card,
  .app-card {
    backdrop-filter: none;
    background: var(--color-bg-elevated);
  }
}

.mood-wash {
  background-image: var(--mood-wash-image, none);
  background-size: cover;
  background-position: center;
  opacity: 1;
}

.mood-wash::before {
  content: '';
  pointer-events: none;
  position: fixed;
  inset: 0;
  z-index: 0;
  opacity: var(--mood-wash-opacity);
  background-image: var(--mood-wash-image, none);
  background-size: 480px;
  background-repeat: repeat;
}
```

(Wire `--mood-wash-image` in Task 6; until then `none` is fine.)

- [ ] **Step 2: Update MainLayout header + logo + mood select**

In template `header-right`, before the dark switch:

```vue
<el-select
  v-model="mood"
  size="small"
  style="width: 110px; margin-right: 12px"
  @change="onMoodChange"
>
  <el-option label="池核" value="pool" />
  <el-option label="海洋核" value="ocean" />
  <el-option label="水晶核" value="crystal" />
  <el-option label="雨核" value="rain" />
</el-select>
```

Script:

```ts
import { useTheme, type MoodName } from '@/composables/useTheme'

const { isDark, applyTheme, mood, setMood } = useTheme()

const onMoodChange = (value: MoodName) => setMood(value)
const onDarkChange = (dark: boolean) => {
  applyTheme(dark ? 'dark' : 'light')
}
```

Bind switch: `v-model="isDark"` `@change="onDarkChange"` (Element Plus passes the new boolean).

Logo CSS — replace gradient text clip with:

```less
.app-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 0.5px;
  background: none;
  -webkit-text-fill-color: unset;
}
```

Remove hardcoded `rgba(52, 211, 153, 0.16)` in aside; rely on `--color-primary-soft`.

- [ ] **Step 3: Smoke on localhost:3000**

Switch all 4 moods × light/dark; confirm sidebar tint, glass cards, logo color, persistence after reload.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/global.less frontend/src/layouts/MainLayout.vue
git commit -m "feat(theme): glass cards, mood picker, solid logo color"
```

---

### Task 5: Create importable mood materials

**Files:**
- Create: `frontend/src/assets/moods/SOURCES.md`
- Create: `frontend/src/assets/moods/ocean/wave.svg`
- Create: `frontend/src/assets/moods/pool/ripple.svg`
- Create: `frontend/src/assets/moods/crystal/facet.svg`
- Create: `frontend/src/assets/moods/rain/streaks.svg`
- Create: `frontend/src/assets/moods/textures/mist.png` (generate via script)
- Create: `frontend/src/assets/moods/icons/empty-droplet.svg`
- Create: `frontend/src/assets/moods/icons/kpi-wave.svg`
- Create: `frontend/scripts/gen-mood-textures.mjs` (optional one-shot generator)

**Interfaces:**
- Consumes: none
- Produces: static assets importable as URL from Vue/CSS

- [ ] **Step 1: Write `SOURCES.md`**

```md
# Mood materials sources

All SVG/icons in this folder are original assets drawn for 自律组件 (CC0 / public domain dedication by project authors).

Textures (`textures/*.png`) are generated programmatically (soft noise + alpha) via `scripts/gen-mood-textures.mjs` — no third-party bitmap.

Do not add non-CC0 / non-original files without updating this document.
```

- [ ] **Step 2: Add SVG motifs** (minimal inline examples)

`ocean/wave.svg`:

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 80" fill="none">
  <path d="M0 40c30-24 50-24 80 0s50 24 80 0 50-24 80 0" stroke="currentColor" stroke-width="3" opacity="0.55"/>
  <path d="M0 55c30-18 50-18 80 0s50 18 80 0 50-18 80 0" stroke="currentColor" stroke-width="2" opacity="0.35"/>
</svg>
```

`pool/ripple.svg`: concentric arcs; `crystal/facet.svg`: diamond facets; `rain/streaks.svg`: diagonal soft lines — same `currentColor` style.

- [ ] **Step 3: Generate mist texture**

Add `frontend/scripts/gen-mood-textures.mjs` using pure Node canvas OR run from box with Pillow and CopyFromBox. Prefer a tiny PNG written by Python on the agent box then copied into the repo if Node canvas is unavailable:

```python
# run once; output mist.png RGBA soft noise
from PIL import Image
import random
img = Image.new('RGBA', (256, 256))
px = img.load()
for y in range(256):
  for x in range(256):
    v = random.randint(180, 255)
    a = random.randint(10, 40)
    px[x, y] = (v, v, v, a)
img.save('mist.png')
```

Place at `frontend/src/assets/moods/textures/mist.png`.

- [ ] **Step 4: Icons**

`empty-droplet.svg` / `kpi-wave.svg` — simple monoline, `stroke="currentColor"`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/assets/moods frontend/scripts/gen-mood-textures.mjs
git commit -m "assets(moods): add SVG motifs, mist texture, icons"
```

---

### Task 6: Wire materials into theme + EmptyState

**Files:**
- Modify: `frontend/src/styles/theme.less` (set `--mood-wash-image` per mood)
- Modify: `frontend/src/layouts/MainLayout.vue` or `Dashboard.vue` (optional motif)
- Modify: `frontend/src/components/ui/EmptyState.vue`
- Modify: `frontend/src/views/Dashboard.vue` KPI icons only if currently using generic chips that look "AI" — prefer tint via CSS, optional `kpi-wave` mask

**Interfaces:**
- Consumes: asset URLs via Vite `url(...)` in LESS or imported URLs in Vue
- Produces: visible wash + empty icon using mood materials

- [ ] **Step 1: LESS wash URLs**

In `theme.less` (Vite resolves from file location — use relative path from styles to assets):

```less
[data-mood='ocean'] {
  --mood-wash-image: url('../assets/moods/textures/mist.png');
}
[data-mood='pool'] {
  --mood-wash-image: url('../assets/moods/textures/mist.png');
}
[data-mood='crystal'] {
  --mood-wash-image: url('../assets/moods/textures/mist.png');
}
[data-mood='rain'] {
  --mood-wash-image: url('../assets/moods/textures/mist.png');
}
```

(Same mist is OK for v1; optional later: tint via CSS `filter` on wash pseudo.)

Add class `mood-wash` on `el-main.main` in MainLayout.

- [ ] **Step 2: EmptyState icon**

```vue
<template>
  <div class="empty-state">
    <img class="mood-icon" :src="emptyIcon" alt="" />
    <div class="title">{{ title }}</div>
    <div v-if="$slots.action" class="action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
import emptyIcon from '@/assets/moods/icons/empty-droplet.svg'
withDefaults(defineProps<{ title?: string }>(), { title: '暂无数据' })
</script>
```

Style `.mood-icon { width: 40px; height: 40px; opacity: 0.55; }` — SVG uses currentColor only if inlined; for `<img>` use a pre-colored neutral stroke in the SVG (`#7a90a0`) or switch to inline component. Prefer inline Vue SFC or raw SVG component `MoodEmptyIcon.vue` that inherits `color: var(--color-text-muted)`.

Recommended: create `frontend/src/components/ui/MoodEmptyIcon.vue` with inline SVG path from `empty-droplet.svg` and `color: currentColor`.

- [ ] **Step 3: Optional dashboard motif**

In Dashboard rhythm card corner, absolute-position `<img>` or inline SVG of current mood motif at opacity 0.1 — read mood from `useTheme().mood` and pick import map.

```ts
import wave from '@/assets/moods/ocean/wave.svg'
import ripple from '@/assets/moods/pool/ripple.svg'
import facet from '@/assets/moods/crystal/facet.svg'
import streaks from '@/assets/moods/rain/streaks.svg'

const motifSrc = computed(() => ({
  ocean: wave, pool: ripple, crystal: facet, rain: streaks,
}[mood.value]))
```

- [ ] **Step 4: Manual check** — wash visible but subtle; empty state shows droplet; mood switch changes motif

- [ ] **Step 5: Commit**

```bash
git add frontend/src/styles/theme.less frontend/src/layouts/MainLayout.vue frontend/src/components/ui/EmptyState.vue frontend/src/components/ui/MoodEmptyIcon.vue frontend/src/views/Dashboard.vue
git commit -m "feat(theme): wire mood wash, empty icon, dashboard motif"
```

---

### Task 7: Sweep leftover AI-dashboard accents + final QA

**Files:**
- Modify: any `frontend/src/**` still hardcoding `#10b981`, `#34d399`, `#3b82f6`, dual gradients
- Test: `frontend/src/composables/useTheme.spec.ts`; manual 4×2 matrix

**Interfaces:**
- Consumes: semantic tokens only
- Produces: no emerald/blue SaaS leftovers in shared chrome

- [ ] **Step 1: Search and replace hardcoded accents**

From `frontend/`:

```bash
rg -n "#10b981|#34d399|#3b82f6|#60a5fa|135deg.*10b981|gradient-primary" src
```

Replace hits in shared UI with `var(--color-primary)` / `var(--color-secondary)` / soft tokens. Skip chart demo constants if they already call `chartPalette()`.

- [ ] **Step 2: Run unit tests + typecheck**

```bash
npx vitest run src/composables/useTheme.spec.ts
npm run typecheck
```

Expected: PASS

- [ ] **Step 3: Manual 4×2 checklist**

For each mood × {light, dark}: sidebar tint, glass cards, primary button, logo, wash, motif, reload persistence.

- [ ] **Step 4: Commit**

```bash
git add -u frontend/src
git commit -m "refactor(theme): remove leftover emerald/blue accents"
```

---

## Spec coverage check

| Spec requirement | Task |
| --- | --- |
| 4 moods × light/dark tokens | 3 |
| `data-mood` + `data-theme` | 1–2 |
| Default ocean + system theme | 1–2 |
| localStorage mood+theme | 1 |
| Glass cards medium intensity | 4 |
| Mood picker + keep dark switch | 4 |
| Sidebar mood-tinted | 3–4 |
| Logo no dual SaaS gradient | 4 |
| SVG + textures + icons | 5–6 |
| Charts follow tokens | 3 (via existing `chartPalette`) |
| Tests | 1, 7 |

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-20-liquid-mood-themes.md`. Two execution options:

**1. Subagent-Driven (recommended)** — fresh subagent per task, review between tasks  
**2. Inline Execution** — execute tasks in this session with checkpoints

Which approach?
