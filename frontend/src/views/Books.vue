<template>
  <div class="books-page">
    <!-- 工具栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索书籍..."
            clearable
            @keyup.enter="handleSearch"
          >
            <template #suffix>
              <el-icon v-if="searching" class="is-loading">
                <Loading />
              </el-icon>
              <el-icon v-else class="cursor-pointer" @click="handleSearch">
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>

        <div class="filter-controls">
          <el-select v-model="sortBy" placeholder="排序方式" @change="handleSortChange">
            <el-option label="评分最高" value="rating" />
            <el-option label="最热" value="hot" />
            <el-option label="最新" value="new" />
            <el-option label="随机发现" value="random" />
          </el-select>

          <el-button-group>
            <el-button type="primary" :plain="!showOnlyFavorited" @click="showOnlyFavorited = false"
              >全部</el-button
            >
            <el-button type="primary" :plain="showOnlyFavorited" @click="showOnlyFavorited = true"
              >收藏</el-button
            >
          </el-button-group>

          <el-button type="primary" :loading="loading" @click="loadBooks">
            <el-icon><Refresh /></el-icon>
            刷新推荐
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 书籍列表 -->
    <el-card v-loading="loading" class="books-list-card">
      <div v-if="books.length === 0" class="empty-state">
        <div class="empty-icon">📚</div>
        <p>暂无书籍推荐</p>
      </div>
      <div v-else class="books-masonry">
        <div v-for="item in books" :key="item.id" class="book-masonry-item">
          <el-card
            :body-style="{ padding: '0px' }"
            class="book-card"
            @mouseenter="hoveredId = item.id"
            @mouseleave="hoveredId = null"
          >
            <div class="book-cover-wrapper">
              <img
                :src="item.cover || fallbackCover"
                class="book-cover"
                loading="lazy"
                @error="onImgError($event)"
              />
              <div v-if="hoveredId === item.id" class="overlay">
                <el-button type="primary" text @click="goDetail(item)">查看详情</el-button>
              </div>
            </div>
            <div class="book-info">
              <h4 class="book-title" :title="item.title">{{ item.title }}</h4>
              <p class="book-author">{{ item.author || '未知作者' }}</p>
              <p class="book-description">{{ item.description }}</p>
              <div class="book-stats">
                <el-rate v-model="item.rating" disabled show-score size="small" />
              </div>
              <div class="book-actions">
                <el-button
                  size="small"
                  :type="isFavorited(item.id) ? 'danger' : 'default'"
                  @click.stop="toggleFavorite(item)"
                >
                  <el-icon><Star /></el-icon>
                  {{ isFavorited(item.id) ? '已收藏' : '收藏' }}
                </el-button>
                <el-button
                  size="small"
                  type="primary"
                  plain
                  :loading="generatingId === item.id"
                  @click.stop="handleGenBookImage(item)"
                >
                  <el-icon><Picture /></el-icon>
                  AI配图
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>

    <!-- 分页 -->
    <el-card v-if="totalPages > 1" class="pagination-card">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getBooks, searchBooks } from '@/api/books'
import { generateBookImage } from '@/api/ai'
import { useRouter } from 'vue-router'
import { Loading, Search, Refresh, Star, Picture } from '@element-plus/icons-vue'

// 基础数据
const books = ref<any[]>([])
const router = useRouter()
const loading = ref<boolean>(false)
const searching = ref<boolean>(false)
const fallbackCover = '/no-cover.svg'

// 新增数据
const searchKeyword = ref<string>('')
const sortBy = ref<string>('random') // 默认使用随机发现模式，让用户看到新内容
const showOnlyFavorited = ref<boolean>(false)
const hoveredId = ref<string | null>(null)
const generatingId = ref<string | null>(null)
const currentPage = ref<number>(1)
const pageSize = ref<number>(10)
const total = ref<number>(0)

// 本地收藏管理
const favoriteIds = ref<Set<string>>(new Set())

// 计算属性
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

onMounted(async () => {
  // 从 localStorage 恢复收藏
  const saved = localStorage.getItem('favoriteBooks')
  if (saved) {
    favoriteIds.value = new Set(JSON.parse(saved))
  }
  await loadBooks()
})

// 数据清洗与修复逻辑
const processBooksData = (rawBooks: any[]) => {
  return rawBooks.map((b: any) => {
    let title = b.title
    let author = b.author
    let category = b.category
    let cover = b.cover

    // 修复导入数据可能出现的字段错位
    // 如果标题看起来像图片文件名
    if (title && (title.endsWith('.jpg') || title.endsWith('.png'))) {
      // 尝试从分类中恢复标题，如果分类看起来像标题
      if (category && category.length > 20 && !category.includes('Calendar')) {
        title = category
      } else if (b.description && b.description.startsWith('From Book32 dataset - ')) {
        // 从描述中提取
        title = b.description.replace('From Book32 dataset - ', '')
      }
    }

    // 如果作者看起来像URL
    if (author && (author.startsWith('http') || author.includes('.jpg'))) {
      // 这其实是封面图
      if (!cover || !cover.startsWith('http')) {
        cover = author
      }
      author = 'Unknown'
    }

    // 封面图降级处理
    if (
      !cover ||
      cover.startsWith('https://via.placeholder.com') ||
      cover.startsWith('http://via.placeholder.com')
    ) {
      cover = ''
    }

    return { ...b, title, author, cover }
  })
}

// 加载书籍
const loadBooks = async () => {
  try {
    loading.value = true
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      sortBy: sortBy.value,
    }
    const data: any = await getBooks(params.page, params.size, params.sortBy)

    // 处理分页响应和非分页响应
    if (data.content) {
      books.value = data.content
      total.value = data.totalElements || 0
    } else if (Array.isArray(data)) {
      books.value = data
      total.value = data.length
    } else {
      books.value = []
    }

    // 清理占位图和修复数据错位
    books.value = processBooksData(books.value)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '获取书籍失败')
  } finally {
    loading.value = false
  }
}

// 搜索书籍
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    currentPage.value = 1
    await loadBooks()
    return
  }

  try {
    searching.value = true
    const data: any = await searchBooks(searchKeyword.value)
    const rawList = Array.isArray(data) ? data : []
    books.value = processBooksData(rawList)
    total.value = books.value.length
    currentPage.value = 1
    ElMessage.success(`找到 ${books.value.length} 本书籍`)
  } catch (error: any) {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

// 排序变化
const handleSortChange = async () => {
  currentPage.value = 1
  await loadBooks()
}

// 分页变化
const handlePageChange = async () => {
  await loadBooks()
}

// 收藏管理
const isFavorited = (bookId: string): boolean => {
  return favoriteIds.value.has(bookId)
}

const toggleFavorite = (book: any) => {
  if (isFavorited(book.id)) {
    favoriteIds.value.delete(book.id)
    ElMessage.info('已取消收藏')
  } else {
    favoriteIds.value.add(book.id)
    ElMessage.success('已收藏')
  }
  // 保存到 localStorage
  localStorage.setItem('favoriteBooks', JSON.stringify(Array.from(favoriteIds.value)))
}

// 生成配图
const handleGenBookImage = async (book: any) => {
  try {
    generatingId.value = book.id
    const updated: any = await generateBookImage(book.id)
    // 更新本地数据
    const idx = books.value.findIndex((b) => b.id === book.id)
    if (idx >= 0) {
      books.value[idx] = { ...books.value[idx], ...updated }
    }
    ElMessage.success('配图已生成')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '生成配图失败')
  } finally {
    generatingId.value = null
  }
}

// 跳转详情页
const goDetail = (book: any) => {
  router.push({
    name: 'BookDetail',
    params: { id: book.id || 'unknown' },
    state: { book },
  })
}

// 处理图片加载失败
const onImgError = (e: Event) => {
  const target = e.target as HTMLImageElement
  target.onerror = null
  target.src = fallbackCover
}
</script>

<style scoped>
/* 布局 */
.books-page {
  padding: 20px;
}

.toolbar-card,
.books-list-card,
.pagination-card {
  margin-bottom: 20px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.search-bar {
  flex: 1;
  min-width: 250px;
}

.search-bar :deep(.el-input) {
  border-radius: 4px;
}

.filter-controls {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-controls :deep(.el-select) {
  width: 120px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

/* 书籍卡片 */
.book-card {
  cursor: pointer;
  transition:
    transform 0.2s ease-in-out,
    box-shadow 0.2s ease-in-out;
  overflow: hidden;
  height: 100%; /* 确保卡片填满容器 */
}

.book-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* 书籍封面 */
.book-cover-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.book-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease-in-out;
}

.book-card:hover .book-cover {
  transform: scale(1.05);
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.overlay :deep(.el-button) {
  background: white;
}

/* 书籍信息 */
.book-info {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.book-title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
}

.book-author {
  margin: 0;
  color: #666;
  font-size: 13px;
}

.book-description {
  margin: 0;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-clamp: 2;
}

.book-stats {
  margin: 4px 0 0 0;
}

.book-stats :deep(.el-rate) {
  align-items: center;
}

/* 操作按钮 */
.book-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.book-actions :deep(.el-button) {
  flex: 1;
  min-width: 80px;
}

/* 分页 */
.pagination-card :deep(.el-pagination) {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar {
    min-width: unset;
    width: 100%;
  }

  .filter-controls {
    width: 100%;
  }

  .filter-controls :deep(.el-select) {
    width: 100%;
  }

  .book-actions {
    flex-direction: column;
  }

  .book-actions :deep(.el-button) {
    width: 100%;
    min-width: unset;
  }
}

/* 加载动画 */
.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  0% {
    transform: rotateZ(0deg);
  }
  100% {
    transform: rotateZ(360deg);
  }
}

/* 光标指针 */
.cursor-pointer {
  cursor: pointer;
}

/* 瀑布流布局 */
.books-masonry {
  column-count: 4;
  column-gap: 20px;
  padding: 10px 0;
}

.book-masonry-item {
  break-inside: avoid;
  margin-bottom: 20px;
  /* 修复 Chrome 渲染 bug */
  -webkit-column-break-inside: avoid;
  page-break-inside: avoid;
}

@media (max-width: 1400px) {
  .books-masonry {
    column-count: 3;
  }
}

@media (max-width: 992px) {
  .books-masonry {
    column-count: 2;
  }
}

@media (max-width: 600px) {
  .books-masonry {
    column-count: 1;
  }
}
</style>
