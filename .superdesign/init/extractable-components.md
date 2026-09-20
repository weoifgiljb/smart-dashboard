# Extractable Superdesign components

## AppShell
- Source: `frontend/src/layouts/MainLayout.vue`
- Category: layout
- Description: Header + dark sidebar-capable aside + main content
- Extractable props: `activeItem` (string, default: "/"), `isCollapsed` (boolean, default: false), `username` (string, default: "用户"), `isDark` (boolean, default: false)
- Hardcoded: menu labels, Element icons, logo text “自律组件”

## AppHeader
- Source: `frontend/src/layouts/MainLayout.vue` (header block)
- Category: layout
- Description: Collapse control, theme switch, welcome text, logout
- Extractable props: `username` (string, default: "用户"), `isDark` (boolean, default: false)
- Hardcoded: 暗/亮 labels, 退出 button

## AppSidebar
- Source: `frontend/src/layouts/MainLayout.vue` (aside block)
- Category: layout
- Description: Brand title + vertical nav
- Extractable props: `activeItem` (string, default: "/"), `isCollapsed` (boolean, default: false)
- Hardcoded: 8 menu items and icons

## AppCard
- Source: `frontend/src/components/ui/AppCard.vue`
- Category: basic
- Description: Hover card — skip extraction (too simple)

## AppButton
- Source: `frontend/src/components/ui/AppButton.vue`
- Category: basic
- Description: Button wrapper — skip extraction
