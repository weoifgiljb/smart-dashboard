<template>
  <router-view />
</template>

<script setup lang="ts"></script>

<style lang="less">
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body,
#app {
  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text);
}

.el-button {
  font-weight: 500;
  border-radius: var(--radius-sm);
}

.el-button--primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.el-button--primary:hover,
.el-button--primary:focus {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.el-input__wrapper {
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px var(--color-border) inset;
}

.el-card {
  border-radius: var(--radius-md);
}
</style>
