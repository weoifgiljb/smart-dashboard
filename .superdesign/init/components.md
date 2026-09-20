# Shared UI primitives

Framework: Vue 3 + TypeScript + Vite. Component library: Element Plus. CSS: vanilla CSS variables in `frontend/src/styles/tokens.css`.

## AppButton

- Path: `frontend/src/components/ui/AppButton.vue`
- Description: Element Plus button wrapper with semantic variants
- Props: `variant` (primary|secondary|danger|success|warning|info|text), `type`, `plain`, `round`, `link`, `disabled`, `loading`

```vue
<template>
  <el-button
    :type="typeMap[variant] || type"
    :plain="plain"
    :round="round"
    :link="link"
    :disabled="disabled"
    :loading="loading"
    @click="$emit('click', $event)"
  >
    <slot />
  </el-button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type Variant = 'primary' | 'secondary' | 'danger' | 'success' | 'warning' | 'info' | 'text'

withDefaults(
  defineProps<{
    variant?: Variant
    type?: string
    plain?: boolean
    round?: boolean
    link?: boolean
    disabled?: boolean
    loading?: boolean
  }>(),
  {
    variant: 'primary',
    type: '',
    plain: false,
    round: false,
    link: false,
    disabled: false,
    loading: false,
  },
)

defineEmits<{ (e: 'click', ev: MouseEvent): void }>()

const typeMap = computed<Record<string, any>>(() => ({
  primary: 'primary',
  secondary: 'info',
  danger: 'danger',
  success: 'success',
  warning: 'warning',
  info: 'info',
  text: '',
}))
</script>
```

## AppCard

- Path: `frontend/src/components/ui/AppCard.vue`
- Description: Card shell with optional header slot
- Props: `bodyPadding` (default `20px`)

```vue
<template>
  <el-card class="app-card" shadow="hover" :body-style="{ padding: bodyPadding }">
    <template v-if="$slots.header" #header>
      <div class="card-header">
        <slot name="header" />
      </div>
    </template>
    <slot />
  </el-card>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    bodyPadding?: string
  }>(),
  {
    bodyPadding: '20px',
  },
)
</script>

<style scoped>
.app-card {
  border: none;
  border-radius: var(--radius-md);
  background: var(--card-bg);
  transition: all 0.3s ease;
  margin-bottom: 16px;
}
.app-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: var(--app-text);
}
:deep(.el-card__header) {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 16px 20px;
}
</style>
```

## EmptyState

- Path: `frontend/src/components/ui/EmptyState.vue`
- Description: Centered empty placeholder with optional action slot
- Props: `title` (default `暂无数据`)

```vue
<template>
  <div class="empty-state">
    <el-icon class="icon"><i class="el-icon-remove-outline" /></el-icon>
    <div class="title">{{ title }}</div>
    <div v-if="$slots.action" class="action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ title?: string }>(), { title: '暂无数据' })
</script>

<style scoped>
.empty-state {
  padding: 24px;
  text-align: center;
  color: var(--app-subtext);
}
.icon { font-size: 28px; opacity: 0.6; }
.title { margin-top: 8px; }
.action { margin-top: 12px; }
</style>
```

## SkeletonPage

- Path: `frontend/src/components/SkeletonPage.vue`
- Description: Route-level loading skeleton for Suspense fallback

```vue
<template>
  <div class="skeleton-page">
    <div class="sk-card"></div>
    <div class="sk-grid">
      <div v-for="i in 3" :key="i" class="sk-card"></div>
    </div>
  </div>
</template>
```

## ErrorBoundary

- Path: `frontend/src/components/ErrorBoundary.vue`
- Description: Catches render errors; retry or go home

Hardcoded Element blue `#409eff` on primary button (inconsistency).

## BaseChart

- Path: `frontend/src/components/charts/BaseChart.vue`
- Description: ECharts wrapper; props `option`, `height`, `autoresize`; emit `chart-click`
