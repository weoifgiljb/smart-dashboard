<template>
  <div class="books-page">
    <!-- 头部搜索区 -->
    <div class="header-banner">
      <div class="banner-content">
        <h2>发现下一本好书</h2>
        <p>探索、阅读、收藏，构建你的知识殿堂</p>
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索书名、作者..."
            class="main-search"
            size="large"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #suffix>
              <el-button type="primary" round @click="handleSearch" :loading="searching">搜索</el-button>
            </template>
          </el-input>
        </div>
        <div class="quick-filters">
          <span 
            v-for="opt in sortOptions" 
            :key="opt.value" 
            class="filter-tag" 
            :class="{ active: sortBy === opt.value }"
            @click="changeSort(opt.value)"
          >
            {{ opt.label }}
          </span>
          <el-divider direction="vertical" />
          <span 
            class="filter-tag" 
            :class="{ active: showOnlyFavorited }" 
            @click="toggleShowFavorite"
          >
            <el-icon><StarFilled /></el-icon> 仅看收藏
          </span>
        </div>
      </div>
    </div>

    <!-- 书籍列表 -->
    <div class="books-container" v-loading="loading">
      <div v-if="books.length === 0" class="empty-state">
        <div class="empty-icon">📚</div>
        <p>没有找到相关书籍，换个词试试？</p>
        <el-button @click="resetFilters">重置筛选</el-button>
      </div>

      <div v-else class="masonry-grid">
        <div v-for="book in books" :key="book.id" class="book-card-wrapper">
          <div class="book-card" @click="goDetail(book)">
            <div class="cover-image">
              <img :src="book.cover || fallbackCover" loading="lazy" @error="onImgError" />
              <div class="cover-overlay">
                <el-button type="primary" round size="small">查看详情</el-button>
              </div>
              <div class="fav-btn" @click.stop="toggleFavorite(book)">
                <el-icon :class="{ active: isFavorited(book.id) }">
                  <StarFilled v-if="isFavorited(book.id)" />
                  <Star v-else />
                </el-icon>
              </div>
            </div>
            <div class="book-info">
              <h3 class="book-title" :title="book.title">{{ book.title }}</h3>
              <div class="book-meta">
                <span class="author">{{ book.author || '佚名' }}</span>
                <div class="rating" v-if="book.rating > 0">
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
                   :loading="generatingId === book.id"
                   @click.stop="handleGenBookImage(book)"
                 >
                   <el-icon><MagicStick /></el-icon> AI配图
                 </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-section" v-if="totalPages > 1">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Star, StarFilled, MagicStick } from '@element-plus/icons-vue'
import { getBooks, searchBooks } from '@/api/books'
import { generateBookImage } from '@/api/ai'

const router = useRouter()
const books = ref<any[]>([])
const loading = ref(false)
const searching = ref(false)
const fallbackCover = '/no-cover.svg'

const searchKeyword = ref('')
const sortBy = ref('random')
const showOnlyFavorited = ref(false)
const generatingId = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = ref(12) // 4 columns * 3 rows
const total = ref(0)
const favoriteIds = ref<Set<string>>(new Set())

const sortOptions = [
  { label: '随机推荐', value: 'random' },
  { label: '评分最高', value: 'rating' },
  { label: '最新上架', value: 'new' },
  { label: '热门书籍', value: 'hot' },
]

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

onMounted(async () => {
  const saved = localStorage.getItem('favoriteBooks')
  if (saved) favoriteIds.value = new Set(JSON.parse(saved))
  await loadBooks()
})

const processBooksData = (rawBooks: any[]) => {
  return rawBooks.map((b: any) => {
    let title = b.title
    let author = b.author
    let cover = b.cover
    if (title && (title.endsWith('.jpg') || title.endsWith('.png'))) {
      if (b.category && b.category.length > 20) title = b.category
    }
    if (author && (author.startsWith('http') || author.includes('.jpg'))) {
      if (!cover || !cover.startsWith('http')) cover = author
      author = 'Unknown'
    }
    if (!cover || cover.includes('placeholder')) cover = ''
    return { ...b, title, author, cover }
  })
}

const loadBooks = async () => {
  try {
    loading.value = true
    // 如果是仅看收藏，就在前端过滤（简化逻辑，实际应由后端支持）
    if (showOnlyFavorited.value) {
      // 模拟加载收藏...这里简单处理为加载所有后过滤，或者需要后端支持findByIds
      // 暂时先加载普通列表
    }
    
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      sortBy: sortBy.value,
    }
    const data: any = await getBooks(params.page, params.size, params.sortBy)

    if (data.content) {
      books.value = processBooksData(data.content)
      total.value = data.totalElements || 0
    } else if (Array.isArray(data)) {
      books.value = processBooksData(data)
      total.value = data.length
    } else {
      books.value = []
    }
    
    if (showOnlyFavorited.value) {
      books.value = books.value.filter(b => favoriteIds.value.has(b.id))
    }
  } catch (error) {
    ElMessage.error('获取书籍失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    currentPage.value = 1
    await loadBooks()
    return
  }
  searching.value = true
  try {
    const data: any = await searchBooks(searchKeyword.value)
    const rawList = Array.isArray(data) ? data : []
    books.value = processBooksData(rawList)
    total.value = books.value.length
    currentPage.value = 1
  } catch {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

const changeSort = (val: string) => {
  sortBy.value = val
  currentPage.value = 1
  loadBooks()
}

const toggleShowFavorite = () => {
  showOnlyFavorited.value = !showOnlyFavorited.value
  loadBooks()
}

const resetFilters = () => {
  searchKeyword.value = ''
  sortBy.value = 'random'
  showOnlyFavorited.value = false
  loadBooks()
}

const handlePageChange = () => {
  loadBooks()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const isFavorited = (id: string) => favoriteIds.value.has(id)

const toggleFavorite = (book: any) => {
  if (isFavorited(book.id)) {
    favoriteIds.value.delete(book.id)
    ElMessage.info('取消收藏')
  } else {
    favoriteIds.value.add(book.id)
    ElMessage.success('已收藏')
  }
  localStorage.setItem('favoriteBooks', JSON.stringify(Array.from(favoriteIds.value)))
}

const handleGenBookImage = async (book: any) => {
  generatingId.value = book.id
  try {
    const updated: any = await generateBookImage(book.id)
    const idx = books.value.findIndex(b => b.id === book.id)
    if (idx >= 0) books.value[idx] = { ...books.value[idx], ...updated }
    ElMessage.success('配图已生成')
  } catch {
    ElMessage.error('生成失败')
  } finally {
    generatingId.value = null
  }
}

const goDetail = (book: any) => {
  router.push({
    name: 'BookDetail',
    params: { id: book.id || 'unknown' },
    state: { book },
  })
}

const onImgError = (e: Event) => {
  (e.target as HTMLImageElement).src = fallbackCover
}
</script>

<style scoped>
.books-page {
  min-height: 100vh;
  background: var(--app-bg);
}

/* 顶部横幅 */
.header-banner {
  background: white;
  padding: 40px 20px;
  text-align: center;
  border-bottom: 1px solid var(--border);
}

.banner-content {
  max-width: 800px;
  margin: 0 auto;
}

.banner-content h2 { font-size: 28px; margin: 0 0 8px; color: var(--app-text); }
.banner-content p { color: var(--text-secondary); margin-bottom: 24px; }

.search-box {
  max-width: 600px;
  margin: 0 auto 20px;
}

.main-search :deep(.el-input__wrapper) {
  border-radius: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  padding-left: 16px;
}

.quick-filters {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  color: var(--text-secondary);
}

.filter-tag {
  cursor: pointer;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}
.filter-tag:hover { color: var(--primary); }
.filter-tag.active { color: var(--primary); font-weight: 600; }

/* 瀑布流 */
.books-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
}

.masonry-grid {
  column-count: 4;
  column-gap: 24px;
}

.book-card-wrapper {
  break-inside: avoid;
  margin-bottom: 24px;
}

.book-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
  border: 1px solid transparent;
}

.book-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: var(--el-border-color-lighter);
}

.cover-image {
  position: relative;
  width: 100%;
  padding-top: 140%; /* 2:3 aspect ratio approximately */
  background: #f1f5f9;
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
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.book-card:hover .cover-overlay { opacity: 1; }

.fav-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(255,255,255,0.9);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  font-size: 18px;
  color: #cbd5e1;
  transition: all 0.2s;
}

.fav-btn:hover { transform: scale(1.1); }
.fav-btn .active { color: #f59e0b; }

.book-info {
  padding: 12px;
}

.book-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--app-text);
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
  color: var(--text-light);
}

.rating {
  display: flex;
  align-items: center;
  gap: 2px;
  color: #f59e0b;
  font-weight: 600;
}

.book-actions {
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}

.ai-btn {
  width: 100%;
  color: var(--primary);
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--text-secondary);
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

@media (max-width: 992px) {
  .masonry-grid { column-count: 3; }
}
@media (max-width: 768px) {
  .masonry-grid { column-count: 2; }
}
@media (max-width: 480px) {
  .masonry-grid { column-count: 1; }
}
</style>