<template>
  <router-view />
</template>

<script setup lang="ts"></script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family:
    'Inter',
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    Roboto,
    Oxygen,
    Ubuntu,
    Cantarell,
    sans-serif;
  -webkit-font-smoothing: antialiased;
  background: var(--app-bg);
}

#app {
  min-height: 100vh;
  background: var(--app-bg);
  color: var(--app-text);
}

/* Global Element Plus Overrides */
.el-button {
  font-weight: 500;
  border-radius: var(--radius-sm);
}

.el-button--primary {
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.2);
}

.el-button--primary:hover,
.el-button--primary:focus {
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

.el-card {
  border-radius: var(--radius-md);
  border: none;
}

/* Scrollbar styling */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>
