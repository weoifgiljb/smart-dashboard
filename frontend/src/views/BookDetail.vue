<template>
  <div class="book-detail-page">
    <el-page-header content="书籍详情" @back="goBack" />
    <el-card class="detail-card">
      <div class="detail-header">
        <img :src="book.cover || fallbackCover" class="detail-cover" @error="onImgError" />
        <div class="detail-meta">
          <h2 class="title">{{ book.title }}</h2>
          <p class="author">作者：{{ book.author || '未知' }}</p>
          <div v-if="book.rating != null" class="rating">
            <el-rate v-model="book.rating" disabled show-score />
          </div>
          <div class="actions">
            <el-button type="primary" :disabled="!book.title" @click="openGithubSearch">
              在 GitHub 搜索书目文档
            </el-button>
          </div>
        </div>
      </div>
      <div class="desc">
        <h3>简介</h3>
        <p v-if="book.description">{{ book.description }}</p>
        <p v-else class="muted">暂无简介</p>
      </div>
    </el-card>

    <el-card class="github-card">
      <template #header>
        <div class="card-header">
          <span>GitHub 书目文档（Top 5）</span>
          <el-button text type="primary" :disabled="!book.title" @click="openGithubSearch"
            >在 GitHub 打开</el-button
          >
        </div>
      </template>

      <el-skeleton v-if="githubLoading" :rows="4" animated />
      <div v-else>
        <div v-if="githubResults.length === 0" class="muted">未找到相关仓库结果</div>
        <ul v-else class="repo-list">
          <li v-for="repo in githubResults" :key="repo.id" class="repo-item">
            <a class="repo-link" :href="repo.html_url" target="_blank" rel="noopener">
              {{ repo.full_name }}
            </a>
            <span class="stars">★ {{ repo.stargazers_count }}</span>
            <p class="repo-desc">{{ repo.description }}</p>
          </li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBookById, getBooks } from '@/api/books'
import { ElMessage } from 'element-plus'

type Repo = {
  id: number
  full_name: string
  html_url: string
  description: string
  stargazers_count: number
}

const route = useRoute()
const router = useRouter()

const book = reactive<any>({
  id: '',
  title: '',
  author: '',
  cover: '',
  description: '',
  rating: null,
})

const githubResults = ref<Repo[]>([])
const githubLoading = ref<boolean>(false)
const fallbackCover = '/no-cover.svg'

const goBack = () => {
  router.back()
}

const onImgError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.onerror = null
  target.src = fallbackCover
}

const resolveBook = async () => {
  const stateBook: any = (history.state && (history.state as any).book) || null
  let targetBook = stateBook

  // Fallback：若刷新后无 state，优先从详情接口获取
  if (!targetBook) {
    try {
      const byId: any = await getBookById(String(route.params.id))
      if (byId) targetBook = byId
    } catch {
      // 失败再尝试从列表兜底（兼容旧逻辑）
      try {
        const all: any = await getBooks(0, 100, 'random') // 尝试获取一些书
        // 注意：这里可能找不到，仅作为最后的尝试
        if (Array.isArray(all)) {
          const found = all.find((b: any) => String(b.id) === String(route.params.id))
          if (found) targetBook = found
        }
      } catch {
        // 保持静默
      }
    }
  }

  if (targetBook) {
    // 修复数据错位逻辑 (同 Books.vue)
    let title = targetBook.title
    let author = targetBook.author
    let category = targetBook.category
    let cover = targetBook.cover
    const desc = targetBook.description

    // 1. 修复标题
    if (title && (title.endsWith('.jpg') || title.endsWith('.png'))) {
      if (category && category.length > 20 && !category.includes('Calendar')) {
        title = category
      } else if (desc && desc.startsWith('From Book32 dataset - ')) {
        title = desc.replace('From Book32 dataset - ', '')
      }
    }

    // 2. 修复作者/封面
    if (author && (author.startsWith('http') || author.includes('.jpg'))) {
      if (!cover || !cover.startsWith('http')) {
        cover = author
      }
      author = 'Unknown'
    }

    // 3. 封面图兜底
    if (
      !cover ||
      cover.startsWith('https://via.placeholder.com') ||
      cover.startsWith('http://via.placeholder.com')
    ) {
      cover = ''
    }

    // 更新响应式对象
    Object.assign(book, { ...targetBook, title, author, cover })
  }
}

const searchGithub = async (query: string) => {
  if (!query) return
  githubLoading.value = true
  try {
    const url =
      'https://api.github.com/search/repositories?q=' +
      encodeURIComponent(`${query} in:name,description,readme`) +
      '&sort=stars&order=desc&per_page=5'
    const res = await fetch(url, { headers: { Accept: 'application/vnd.github+json' } })
    const json = await res.json()
    githubResults.value = Array.isArray(json.items) ? json.items : []
  } catch (e: any) {
    ElMessage.error('GitHub 搜索失败，请稍后重试')
  } finally {
    githubLoading.value = false
  }
}

const openGithubSearch = () => {
  if (!book.title) return
  const q = encodeURIComponent(`${book.title} in:name,description,readme`)
  const link = `https://github.com/search?q=${q}&type=repositories&s=stars&o=desc`
  window.open(link, '_blank')
}

onMounted(async () => {
  await resolveBook()
  if (book.title) {
    searchGithub(book.title)
  }
})
</script>

<style scoped>
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
  border-radius: 4px;
  background: #f5f7fa;
}
.detail-meta {
  flex: 1;
}
.title {
  margin: 0 0 6px 0;
}
.author {
  margin: 0 0 10px 0;
  color: #666;
}
.rating {
  margin: 8px 0 12px 0;
}
.actions {
  display: flex;
  gap: 12px;
}
.desc {
  margin-top: 18px;
}
.muted {
  color: #999;
}
.github-card {
  margin-top: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.repo-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.repo-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.repo-item:last-child {
  border-bottom: none;
}
.repo-link {
  font-weight: 600;
  text-decoration: none;
}
.repo-desc {
  color: #666;
  margin-top: 4px;
}
.stars {
  margin-left: 8px;
  color: #fa8c16;
}
</style>
