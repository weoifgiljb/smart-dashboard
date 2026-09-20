# Theme tokens (compact summary)

Product: 自律组件 / smart dashboard. Font: Inter. Radius: 8 / 12 / 16. Primary emerald + secondary blue.

## Light (`:root`)

- `--primary` #10b981 / `--primary-hover` #059669 / `--primary-light` #d1fae5 / `--primary-fade` rgba(16,185,129,0.1)
- `--secondary` #3b82f6 / `--secondary-hover` #2563eb / `--secondary-light` #dbeafe
- `--success` #10b981 / `--warning` #f59e0b / `--danger` #ef4444 / `--info` #3b82f6
- `--app-bg` #f8fafc / `--card-bg` #ffffff / `--sidebar-bg` #ffffff / `--header-bg` #ffffff
- `--app-text` #1f2937 / `--text-secondary` #6b7280 / `--text-light` #9ca3af / `--border` #e5e7eb
- `--radius-sm` 8px / `--radius-md` 12px / `--radius-lg` 16px
- `--shadow-sm/md/lg` standard gray; `--shadow-colored` greenish
- `--gradient-primary` 135deg #10b981 → #3b82f6
- Element Plus: `--el-color-primary: var(--primary)` and text/bg/border mapped

## Dark (`[data-theme="dark"]`)

- `--app-bg` #111827 / `--card-bg` #1f2937 / `--sidebar-bg` #1f2937 / `--header-bg` #1f2937
- `--app-text` #f9fafb / `--text-secondary` #d1d5db / `--text-light` #9ca3af / `--border` #374151
- `--primary` #34d399 / `--secondary` #60a5fa
- Incomplete `--el-*` (only `--el-border-color-light`)

## Known inconsistencies

Hardcoded Slate hex, Element `#409eff`, Ant `#f0f2f5`. Missing `--primary-rgb`. Charts use hex not tokens.

## Raw source

See `frontend/src/styles/tokens.css` (full file, 148 lines) and `frontend/src/App.vue` global styles.
Theme apply: `frontend/src/main.ts` sets `data-theme` from localStorage or `prefers-color-scheme`.
