export function cssVar(name: string, fallback = ''): string {
  if (typeof document === 'undefined') return fallback
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

export function chartPalette() {
  return {
    text: cssVar('--color-text-muted', '#9ca3af'),
    border: cssVar('--color-border', '#e5e7eb'),
    bg: cssVar('--color-bg-elevated', '#ffffff'),
    primary: cssVar('--color-primary', '#10b981'),
    secondary: cssVar('--color-secondary', '#3b82f6'),
    warning: cssVar('--color-warning', '#f59e0b'),
    danger: cssVar('--color-danger', '#ef4444'),
    success: cssVar('--color-success', '#10b981'),
    info: cssVar('--color-info', '#3b82f6'),
    muted: cssVar('--color-text-muted', '#94a3b8'),
  }
}
