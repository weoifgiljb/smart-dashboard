# Shared layouts

## App root

- Path: `frontend/src/App.vue`
- Description: Bare `router-view` plus global CSS reset, Element Plus tweaks, scrollbar.

```vue
<template>
  <router-view />
</template>

<script setup lang="ts"></script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  background: var(--app-bg);
}
#app {
  min-height: 100vh;
  background: var(--app-bg);
  color: var(--app-text);
}
.el-button { font-weight: 500; border-radius: var(--radius-sm); }
.el-button--primary {
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.2);
}
.el-button--primary:hover, .el-button--primary:focus {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
}
.el-input__wrapper {
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
}
.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--primary) inset !important;
}
.el-card { border-radius: var(--radius-md); border: none; }
::-webkit-scrollbar { width: 8px; height: 8px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 4px; }
::-webkit-scrollbar-thumb:hover { background: #9ca3af; }
</style>
```

## MainLayout (app shell)

- Path: `frontend/src/layouts/MainLayout.vue`
- Description: Top header (collapse, dark switch, welcome, logout) + left sidebar menu + main `router-view` with Suspense/ErrorBoundary.

Structure:
- `el-container.layout-container` (100vh)
- `el-header.header` 64px: fold button | theme switch + username + logout
- `el-aside.aside` 240px / 64px collapsed: logo “自律组件” + `el-menu` routes
- `el-main.main` padding 24px, `var(--app-bg)`

Menu items (hardcoded): `/` 首页, `/calendar` 日历 & 打卡, `/words` 背单词, `/pomodoro` 番茄钟, `/tasks` 任务（增强）, `/diary` 我的日记, `/ai-chat` AI问答, `/books` 书籍推送.

Theme: `document.documentElement.setAttribute('data-theme', 'light'|'dark')` + localStorage.

Full source is in `frontend/src/layouts/MainLayout.vue` (226 lines). Key styles:

```css
.header { background: var(--header-bg); height: 64px; padding: 0 24px; box-shadow: var(--shadow-sm); }
.aside { background: var(--sidebar-bg); border-right: 1px solid var(--el-border-color-light); }
.app-title { background: var(--gradient-primary); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.el-menu-item.is-active { background-color: var(--primary-light); color: var(--primary); }
.main { background: var(--app-bg); padding: 24px; overflow-y: auto; }
```
