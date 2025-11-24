<template>
  <div class="skeleton-page">
    <div class="sk-card"></div>
    <div class="sk-grid">
      <div v-for="i in 3" :key="i" class="sk-card"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SkeletonPage' })
</script>

<style scoped>
.skeleton-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
}
.sk-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
@media (max-width: 960px) {
  .sk-grid {
    grid-template-columns: 1fr;
  }
}
.sk-card {
  height: 120px;
  background: linear-gradient(90deg, #f2f3f5 25%, #e9ecef 37%, #f2f3f5 63%);
  background-size: 400% 100%;
  border-radius: 8px;
  animation: shine 1.4s ease infinite;
}
@keyframes shine {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
