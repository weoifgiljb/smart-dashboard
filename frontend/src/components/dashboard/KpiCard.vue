<template>
  <el-card shadow="hover" class="kpi-card">
    <div class="kpi-body">
      <div class="kpi-icon" :class="`icon-${variant}`">
        <el-icon>
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="kpi-info">
        <div class="stat-value">
          {{ value }} <span class="unit">{{ unit }}</span>
        </div>
        <div class="stat-label">{{ label }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  icon: Component
  value: number | string
  unit: string
  label: string
  variant: 'streak' | 'words' | 'pomodoro' | 'total' | 'primary' | 'success' | 'warning' | 'info'
}>()
</script>

<style scoped>
.kpi-card {
  border: none;
  background: var(--card-bg);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all 0.3s ease;
}

.kpi-card:hover {
  transform: translateY(-4px);
}

.kpi-body {
  display: flex;
  align-items: center;
  padding: 16px 8px;
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.kpi-card:hover .kpi-icon {
  transform: scale(1.1) rotate(5deg);
}

.icon-streak {
  background: #fff1f2;
  color: #e11d48;
}

.icon-words {
  background: #eff6ff;
  color: #2563eb;
}

.icon-pomodoro {
  background: #fff7ed;
  color: #ea580c;
}

.icon-total {
  background: #f0fdf4;
  color: #16a34a;
}

.icon-primary {
  background: #eff6ff;
  color: #3b82f6;
}

.icon-success {
  background: #f0fdf4;
  color: #10b981;
}

.icon-warning {
  background: #fff7ed;
  color: #f59e0b;
}

.icon-info {
  background: #f0f9ff;
  color: #0ea5e9;
}

.kpi-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--app-text);
  line-height: 1.2;
}

.unit {
  font-size: 12px;
  font-weight: normal;
  color: var(--text-light);
  margin-left: 2px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .kpi-body {
    padding: 14px 6px;
  }

  .stat-value {
    font-size: 24px;
  }

  .kpi-icon {
    width: 42px;
    height: 42px;
    font-size: 20px;
    margin-right: 12px;
  }
}

@media (max-width: 768px) {
  .kpi-card:hover {
    transform: translateY(-2px);
  }

  .kpi-body {
    padding: 12px 8px;
  }

  .kpi-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
    margin-right: 12px;
  }

  .stat-value {
    font-size: 22px;
  }

  .stat-label {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .kpi-body {
    padding: 10px 6px;
  }

  .kpi-icon {
    width: 36px;
    height: 36px;
    font-size: 16px;
    margin-right: 10px;
    border-radius: 10px;
  }

  .stat-value {
    font-size: 20px;
  }

  .unit {
    font-size: 11px;
  }

  .stat-label {
    font-size: 11px;
  }
}
</style>
