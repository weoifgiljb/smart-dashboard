<template>
  <div class="not-found">
    <el-empty description="页面不存在">
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.not-found {
  padding: 40px 0;
}
</style>


