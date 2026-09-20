<template>
  <div class="book-card-wrapper">
    <div class="book-card" @click="emit('open', book)">
      <div class="cover-image">
        <img v-if="showCover" :src="book.cover" alt="" @error="onImgError" />
        <div v-else class="cover-fallback">暂无封面</div>
        <div class="cover-overlay">
          <el-button type="primary" round size="small">查看详情</el-button>
        </div>
        <div class="fav-btn" @click.stop="emit('toggleFavorite', book)">
          <el-icon :class="{ active: favorited }">
            <StarFilled v-if="favorited" />
            <Star v-else />
          </el-icon>
        </div>
      </div>
      <div class="book-info">
        <h3 class="book-title" :title="book.title">{{ book.title }}</h3>
        <div class="book-meta">
          <span class="author">{{ book.author || '佚名' }}</span>
          <div v-if="(book.rating || 0) > 0" class="rating">
            <el-icon class="star-icon"><StarFilled /></el-icon>
            {{ book.rating }}
          </div>
        </div>
        <div class="book-actions">
          <el-button
            text
            bg
            size="small"
            class="ai-btn"
            :loading="generating"
            @click.stop="emit('generate', book)"
          >
            <el-icon><MagicStick /></el-icon>
            AI配图
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { MagicStick, Star, StarFilled } from '@element-plus/icons-vue'
import { isDisplayableCover, type BookItem } from '@/utils/bookDisplay'

const props = defineProps<{
  book: BookItem
  favorited: boolean
  generating: boolean
}>()

const emit = defineEmits<{
  open: [book: BookItem]
  toggleFavorite: [book: BookItem]
  generate: [book: BookItem]
}>()

const coverFailed = ref(false)
const showCover = computed(() => isDisplayableCover(props.book.cover) && !coverFailed.value)

watch(
  () => props.book.cover,
  () => {
    coverFailed.value = false
  },
)

function onImgError() {
  coverFailed.value = true
}
</script>

<style scoped lang="less">
.book-card-wrapper {
  break-inside: avoid;
  margin-bottom: 24px;
}

.book-card {
  background: var(--color-bg-elevated);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  cursor: pointer;
  border: 1px solid transparent;
}

.book-card-wrapper:hover .book-card {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-border);
}

.cover-image {
  position: relative;
  width: 100%;
  padding-top: 140%;
  background: var(--color-bg-muted);
}

.cover-image img,
.cover-fallback {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.cover-image img {
  object-fit: cover;
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  font-size: 13px;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.book-card-wrapper:hover .cover-overlay {
  opacity: 1;
}

.fav-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: var(--color-bg-elevated);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  font-size: 18px;
  color: var(--color-text-muted);
}

.fav-btn .active {
  color: var(--color-warning);
}

.book-info {
  padding: 12px;
}

.book-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--color-text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.rating {
  display: flex;
  align-items: center;
  gap: 2px;
  color: var(--color-warning);
  font-weight: 600;
}

.book-actions {
  padding-top: 8px;
  border-top: 1px solid var(--color-bg-muted);
}

.ai-btn {
  width: 100%;
  color: var(--color-primary);
}
</style>
