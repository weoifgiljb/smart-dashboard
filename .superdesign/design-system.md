# Design system — 自律组件

Self-management web app: calendar/check-in, pomodoro, SM-2 vocabulary, tasks, diary, AI chat, books. Vue 3 + Element Plus. Chinese UI copy.

## Visual direction (shipping)

柔和成长风. One emerald brand. Runtime theme via CSS custom properties (`data-theme` on `<html>`), not compile-time Less vars.

- Light: cool slate canvas, white elevated cards, dark ink text
- Dark: #141414 canvas (screenshot family), #1c1c1c cards, #f5f5f5 text
- Sidebar is a **local theme**: always deep navy-slate (`--color-bg` overridden on `.aside`) so content can be light while nav stays dark
- No Element default blue `#409eff`. Charts, buttons, focus rings use `--color-primary`
- No mixed Gray vs Slate vs Ant gray — one slate-neutral ramp

## Color tokens (canonical)

Runtime names (what components must use):

| Token | Light | Dark |
| --- | --- | --- |
| `--color-bg` | #f4f6f8 | #141414 |
| `--color-bg-elevated` | #ffffff | #1c1c1c |
| `--color-bg-muted` | #eef1f4 | #222222 |
| `--color-text` | #111827 | #f5f5f5 |
| `--color-text-secondary` | #4b5563 | #c4c4c4 |
| `--color-text-muted` | #9ca3af | #8a8a8a |
| `--color-border` | #e5e7eb | #2a2a2a |
| `--color-primary` | #10b981 | #34d399 |
| `--color-primary-hover` | #059669 | #10b981 |
| `--color-primary-soft` | rgba(16,185,129,.12) | rgba(52,211,153,.16) |
| `--color-secondary` | #3b82f6 | #60a5fa |
| `--color-success` | #10b981 | #34d399 |
| `--color-warning` | #f59e0b | #fbbf24 |
| `--color-danger` | #ef4444 | #f87171 |
| `--color-info` | #3b82f6 | #60a5fa |
| `--color-primary-rgb` | 16, 185, 129 | 52, 211, 153 |

Sidebar local overrides: `--color-bg` #0f172a, `--color-text` #e2e8f0, `--color-text-secondary` #94a3b8, `--color-border` #1e293b, `--color-primary-soft` rgba(52,211,153,.16)

Legacy aliases kept for a short window: `--app-bg` = `--color-bg`, `--card-bg` = `--color-bg-elevated`, `--app-text` = `--color-text`, `--primary` = `--color-primary`.

## Type

- Family: Inter, system-ui, "Segoe UI", sans-serif
- Page title: 22px / 700
- Section title: 16px / 600
- Body: 14px / 400, line-height 1.6
- Caption: 12px / `--color-text-muted`

## Spacing & radius

- Page padding 24px; card padding 20px; stack gap 16px; KPI gutter 16px
- `--radius-sm` 10px, `--radius-md` 16px, `--radius-lg` 24px
- Cards: no border, `--shadow-sm` only — no hover lift
- Header 56px; sidebar 232px (64px collapsed)

## Motion

- Theme/color 180ms ease
- Sidebar width 200ms ease
- No card translateY on hover

## Component styles

- Primary button: filled `--color-primary`, white text, no green drop-shadow
- Menu active: `--color-primary-soft` background + `--color-primary` text
- Inputs: inset border `--color-border`, focus `--color-primary`
- KPI icon wells: `--color-primary-soft` / warning-soft / info-soft — never raw Tailwind palette hex
- Charts: read tokens from `getComputedStyle(document.documentElement)`

## Constraints for generated designs

Use ONLY these fonts, colors, spacing, and component styles. Do not introduce serif, neon, purple, or Element default blue. Keep Chinese labels and the existing information architecture (sidebar + header + KPI + charts).
