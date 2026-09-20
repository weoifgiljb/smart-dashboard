<template>
  <div class="book-detail-page">
    <el-page-header content="书籍详情" @back="goShelf" />
    <el-card v-if="book" class="detail-card">
      <div class="detail-header">
        <img
          v-if="showCover"
          :src="book.cover"
          class="detail-cover"
          alt=""
          @error="coverFailed = true"
        />
        <div v-else class="detail-cover detail-fallback">暂无封面</div>
        <div class="detail-meta">
          <h2 class="title">{{ book.title }}</h2>
          <p class="author">作者：{{ book.author || '佚名' }}</p>
          <div v-if="book.rating != null" class="rating">
            <el-rate :model-value="rateValue" disabled show-score />
          </div>
          <div class="actions">
            <el-button type="primary" @click="toggleFavorite">
              {{ favorited ? '取消收藏' : '收藏这本书' }}
            </el-button>
            <el-button @click="goShelf">返回书架</el-button>
          </div>
          <el-button
            class="github-link"
            link
            type="primary"
            :disabled="!book.title"
            @click="openGithubSearch"
          >
            在 GitHub 查找相关仓库
          </el-button>
        </div>
      </div>
      <div class="desc">
        <h3>简介</h3>
        <p v-if="book.description">{{ book.description }}</p>
        <p v-else class="muted">暂无简介</p>
      </div>
    </el-card>
    <el-card v-else-if="loaded" class="detail-card">
      <p class="muted">没有找到这本书</p>
      <el-button type="primary" @click="goShelf">返回书架</el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBookById } from '@/api/books'
import { useUserStore } from '@/store/user'
import { isDisplayableCover, normalizeBook, type BookItem } from '@/utils/bookDisplay'
import { loadFavoriteIds, saveFavoriteIds, toggleFavoriteId } from '@/utils/bookFavorites'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const book = ref<BookItem | null>(null)
const favoriteIds = ref(new Set<string>())
const loaded = ref(false)
const coverFailed = ref(false)

const ownerKey = computed(() => userStore.user?.id || userStore.user?.username || 'anon')
const favorited = computed(() => Boolean(book.value && favoriteIds.value.has(book.value.id)))
const showCover = computed(() => isDisplayableCover(book.value?.cover) && !coverFailed.value)
const rateValue = computed(() => {
  const rating = book.value?.rating
  if (rating == null) return 0
  return rating > 5 ? rating / 2 : rating
})

function goShelf() {
  router.push('/books')
}

function stateBook() {
  const state = history.state
  if (!state || typeof state !== 'object' || !('book' in state)) return null
  return normalizeBook((state as { book: unknown }).book)
}

const resolveBook = async () => {
  const fromState = stateBook()
  if (fromState) {
    book.value = fromState
    return
  }
  try {
    book.value = normalizeBook(await getBookById(String(route.params.id)))
  } catch {
    book.value = null
  }
}

function toggleFavorite() {
  if (!book.value) return
  const added = toggleFavoriteId(favoriteIds.value, book.value.id)
  favoriteIds.value = new Set(favoriteIds.value)
  saveFavoriteIds(ownerKey.value, Array.from(favoriteIds.value))
  ElMessage[added ? 'success' : 'info'](added ? '已收藏' : '取消收藏')
}

function openGithubSearch() {
  if (!book.value?.title) return
  const q = encodeURIComponent(`${book.value.title} in:name,description,readme`)
  window.open(`https://github.com/search?q=${q}&type=repositories&s=stars&o=desc`, '_blank')
}

watch(
  () => book.value?.cover,
  () => {
    coverFailed.value = false
  },
)

watch(
  ownerKey,
  (key) => {
    favoriteIds.value = new Set(loadFavoriteIds(key))
  },
  { immediate: true },
)

onMounted(async () => {
  await resolveBook()
  loaded.value = true
})
</script>

<style scoped lang="less">
.book-detail-page {
  padding: 20px;
}

.detail-card {
  margin-top: 10px;
}

.detail-header {
  display: flex;
  gap: 20px;
}

.detail-cover {
  width: 240px;
  height: 320px;
  object-fit: cover;
  border-radius: var(--radius-md);
  background: var(--color-bg-muted);
  flex-shrink: 0;
}

.detail-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

.detail-meta {
  flex: 1;
}

.title {
  margin: 0 0 6px;
  color: var(--color-text);
}

.author {
  margin: 0 0 10px;
  color: var(--color-text-secondary);
}

.rating {
  margin: 8px 0 12px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.github-link {
  margin-top: 12px;
  padding: 0;
}

.desc {
  margin-top: 18px;
  color: var(--color-text);
}

.muted {
  color: var(--color-text-muted);
}
</style>
