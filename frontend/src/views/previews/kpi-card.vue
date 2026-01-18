<template>
  <div class="preview-demo">
    <el-row :gutter="20">
      <el-col :span="6">
        <KpiCard :icon="Trophy" :value="365" unit="天" label="连续打卡" variant="streak" />
      </el-col>
      <el-col :span="6">
        <KpiCard :icon="Reading" :value="1280" unit="词" label="已学单词" variant="words" />
      </el-col>
      <el-col :span="6">
        <KpiCard :icon="Timer" :value="89" unit="个" label="完成番茄" variant="pomodoro" />
      </el-col>
      <el-col :span="6">
        <KpiCard :icon="DataLine" :value="180" unit="天" label="累计坚持" variant="total" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import KpiCard from '@/components/dashboard/KpiCard.vue'
import { Trophy, Reading, Timer, DataLine } from '@element-plus/icons-vue'
</script>

<style scoped>
.preview-demo {
  padding: 20px;
}
</style>
