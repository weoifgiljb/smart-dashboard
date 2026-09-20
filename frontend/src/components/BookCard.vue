<template>
  <div class="book-card-wrapper">
    <div class="book-card" @click="emit('open', book)">
      <div class="cover-image">
        <img :src="book.cover || fallbackCover" loading="lazy" @error="onImgError" />
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
import { MagicStick, Star, StarFilled } from '@element-plus/icons-vue'
import type { BookItem } from '@/utils/bookDisplay'

const fallbackCover = '/no-cover.svg'

defineProps<{
  book: BookItem
  favorited: boolean
  generating: boolean
}>()

const emit = defineEmits<{
  open: [book: BookItem]
  toggleFavorite: [book: BookItem]
  generate: [book: BookItem]
}>()

function onImgError(e: Event) {
  const target = e.target as HTMLImageElement
  target.onerror = null
  target.src = fallbackCover
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

.book-card:hover {
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

.cover-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
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
}

.book-card:hover .cover-overlay {
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
