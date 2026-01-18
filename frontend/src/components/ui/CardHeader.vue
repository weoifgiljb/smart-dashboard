<template>
  <div class="card-header-ui">
    <div class="left"><slot name="left" /></div>
    <div class="right"><slot name="right" /></div>
  </div>
</template>

<script setup lang="ts">
// simple presentational header for cards
</script>

<style scoped>
.card-header-ui {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header-ui .left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-header-ui .right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
