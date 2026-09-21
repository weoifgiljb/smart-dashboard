<template>
  <div class="books-page">
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
              <el-button type="primary" round :loading="searching" @click="handleSearch">
                搜索
              </el-button>
            </template>
          </el-input>
        </div>
        <div class="quick-filters">
          <span
            class="filter-tag"
            :class="{ active: sortBy === BookSort.Random }"
            @click="changeSort(BookSort.Random)"
          >
            随机推荐
          </span>
          <span
            class="filter-tag"
            :class="{ active: sortBy === BookSort.Rating }"
            @click="changeSort(BookSort.Rating)"
          >
            评分最高
          </span>
          <span
            class="filter-tag"
            :class="{ active: sortBy === BookSort.New }"
            @click="changeSort(BookSort.New)"
          >
            最新上架
          </span>
          <span
            class="filter-tag"
            :class="{ active: sortBy === BookSort.Hot }"
            @click="changeSort(BookSort.Hot)"
          >
            热门书籍
          </span>
          <el-button
            v-if="showShuffle"
            size="small"
            round
            :loading="loading"
            @click="shuffleRandom"
          >
            换一批
          </el-button>
          <el-divider direction="vertical" />
          <span
            class="filter-tag"
            :class="{ active: showOnlyFavorited }"
            @click="toggleShowFavorite"
          >
            <el-icon><StarFilled /></el-icon>
            仅看收藏
          </span>
          <el-button size="small" round @click="openImportDialog">导入书目</el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="books-container">
      <div v-if="emptyKind" class="empty-state">
        <div class="empty-icon">📚</div>
        <p>{{ emptyCopy.description }}</p>
        <el-button v-if="emptyCopy.action" @click="onEmptyAction">
          {{ emptyCopy.action }}
        </el-button>
      </div>
      <BookGrid
        v-else
        :books="books"
        :favorite-ids="favoriteIds"
        :generating-id="generatingId"
        @open="goDetail"
        @toggle-favorite="toggleFavorite"
        @generate="handleGenBookImage"
      />
      <div v-if="showPager" class="pagination-section">
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

    <el-dialog v-model="importOpen" title="导入书目" width="520px" align-center destroy-on-close>
      <p class="import-hint">
        CSV 需为 Book32 格式：ASIN, 书名, 作者, 分类, 封面URL。地址必须是公网 http/https。
      </p>
      <el-form label-position="top">
        <el-form-item label="CSV 地址">
          <el-input v-model="importUrl" placeholder="https://example.com/books.csv" clearable />
        </el-form-item>
        <el-form-item label="导入数量">
          <el-input-number v-model="importLimit" :min="1" :max="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importOpen = false">取消</el-button>
        <el-button :loading="importingSample" @click="handleSampleImport">导入示例书架</el-button>
        <el-button type="primary" :loading="importingCsv" @click="handleCsvImport"
          >开始导入</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter, type HistoryState } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, StarFilled } from '@element-plus/icons-vue'
import { generateBookImage } from '@/api/ai'
import {
  getBooks,
  getBooksByIds,
  getRandomBooks,
  importBooks,
  importSampleBooks,
  searchBooks,
} from '@/api/books'
import BookGrid from '@/components/BookGrid.vue'
import { useUserStore } from '@/store/user'
import {
  BookEmptyKind,
  BookSort,
  bookEmptyCopy,
  normalizeBook,
  parseBooksPayload,
  readPayloadMessage,
  resolveBookEmptyKind,
  type BookItem,
} from '@/utils/bookDisplay'
import { loadFavoriteIds, saveFavoriteIds, toggleFavoriteId } from '@/utils/bookFavorites'

const router = useRouter()
const userStore = useUserStore()
const books = ref<BookItem[]>([])
const loading = ref(false)
const searching = ref(false)
const searchKeyword = ref('')
const searchingActive = ref(false)
const sortBy = ref(BookSort.Random)
const showOnlyFavorited = ref(false)
const generatingId = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = 12
const total = ref(0)
const favoriteIds = ref(new Set<string>())
const importOpen = ref(false)
const importUrl = ref('')
const importLimit = ref(24)
const importingCsv = ref(false)
const importingSample = ref(false)

const ownerKey = computed(() => userStore.user?.id || userStore.user?.username || 'anon')

const emptyKind = computed(() =>
  resolveBookEmptyKind({
    hasKeyword: searchingActive.value,
    favoritesOnly: showOnlyFavorited.value,
    count: books.value.length,
  }),
)

const emptyCopy = computed(() =>
  emptyKind.value ? bookEmptyCopy(emptyKind.value) : bookEmptyCopy(BookEmptyKind.Catalog),
)

const showShuffle = computed(
  () => sortBy.value === BookSort.Random && !searchingActive.value && !showOnlyFavorited.value,
)

const showPager = computed(
  () =>
    !searchingActive.value &&
    !showOnlyFavorited.value &&
    sortBy.value !== BookSort.Random &&
    total.value > pageSize,
)

function persistFavorites() {
  saveFavoriteIds(ownerKey.value, Array.from(favoriteIds.value))
}

watch(
  ownerKey,
  (key) => {
    favoriteIds.value = new Set(loadFavoriteIds(key))
  },
  { immediate: true },
)

onMounted(async () => {
  await loadBooks()
})

const loadBooks = async () => {
  try {
    loading.value = true
    if (showOnlyFavorited.value) {
      const ids = Array.from(favoriteIds.value)
      books.value = parseBooksPayload(await getBooksByIds(ids)).books
      total.value = books.value.length
      return
    }
    if (sortBy.value === BookSort.Random) {
      books.value = parseBooksPayload(await getRandomBooks(pageSize)).books
      total.value = books.value.length
      return
    }
    const data = await getBooks(currentPage.value - 1, pageSize, sortBy.value)
    const parsed = parseBooksPayload(data)
    books.value = parsed.books
    total.value = parsed.total
  } catch (error) {
    if (!isAxiosError(error)) ElMessage.error('获取书籍失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    searchingActive.value = false
    currentPage.value = 1
    await loadBooks()
    return
  }
  searching.value = true
  searchingActive.value = true
  showOnlyFavorited.value = false
  try {
    books.value = parseBooksPayload(await searchBooks(keyword)).books
    total.value = books.value.length
    currentPage.value = 1
  } catch (error) {
    if (!isAxiosError(error)) ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}

function changeSort(val: BookSort) {
  sortBy.value = val
  searchingActive.value = false
  searchKeyword.value = ''
  currentPage.value = 1
  loadBooks()
}

function shuffleRandom() {
  sortBy.value = BookSort.Random
  searchingActive.value = false
  loadBooks()
}

function toggleShowFavorite() {
  showOnlyFavorited.value = !showOnlyFavorited.value
  searchingActive.value = false
  loadBooks()
}

function onEmptyAction() {
  const kind = emptyKind.value
  if (!kind) return
  switch (kind) {
    case BookEmptyKind.Search:
      searchKeyword.value = ''
      searchingActive.value = false
      loadBooks()
      return
    case BookEmptyKind.Favorites:
      showOnlyFavorited.value = false
      loadBooks()
      return
    case BookEmptyKind.Catalog:
      openImportDialog()
      return
    default: {
      const exhaustive: never = kind
      return exhaustive
    }
  }
}

function openImportDialog() {
  importOpen.value = true
}

function finishImport(message: string) {
  importOpen.value = false
  ElMessage.success(message)
  searchingActive.value = false
  showOnlyFavorited.value = false
  sortBy.value = BookSort.Random
  currentPage.value = 1
  loadBooks()
}

async function handleSampleImport() {
  importingSample.value = true
  try {
    const data = await importSampleBooks()
    finishImport(readPayloadMessage(data, '示例书架已导入'))
  } catch (error) {
    if (!isAxiosError(error)) ElMessage.error('导入失败')
  } finally {
    importingSample.value = false
  }
}

async function handleCsvImport() {
  const csvUrl = importUrl.value.trim()
  if (!csvUrl.startsWith('http://') && !csvUrl.startsWith('https://')) {
    ElMessage.warning('请填写公网 http/https 地址')
    return
  }
  importingCsv.value = true
  try {
    const data = await importBooks(csvUrl, importLimit.value)
    finishImport(readPayloadMessage(data, '已开始导入，稍后点换一批查看'))
  } catch (error) {
    if (!isAxiosError(error)) ElMessage.error('导入失败')
  } finally {
    importingCsv.value = false
  }
}

function handlePageChange() {
  loadBooks()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function toggleFavorite(book: BookItem) {
  const added = toggleFavoriteId(favoriteIds.value, book.id)
  favoriteIds.value = new Set(favoriteIds.value)
  persistFavorites()
  ElMessage[added ? 'success' : 'info'](added ? '已收藏' : '取消收藏')
  if (showOnlyFavorited.value && !added) {
    books.value = books.value.filter((item) => item.id !== book.id)
  }
}

async function handleGenBookImage(book: BookItem) {
  generatingId.value = book.id
  try {
    const updated = normalizeBook(await generateBookImage(book.id))
    if (!updated) return
    books.value = books.value.map((item) => (item.id === book.id ? { ...item, ...updated } : item))
    ElMessage.success('配图已生成')
  } catch (error) {
    if (!isAxiosError(error)) ElMessage.error('生成失败')
  } finally {
    generatingId.value = null
  }
}

function goDetail(book: BookItem) {
  router.push({
    name: 'BookDetail',
    params: { id: book.id },
    state: { book: { ...book } } satisfies HistoryState,
  })
}
</script>

<style scoped lang="less">
.books-page {
  min-height: 100vh;
  background: var(--color-bg);
}

.header-banner {
  background: var(--color-bg-elevated);
  padding: 40px 20px;
  text-align: center;
  border-bottom: 1px solid var(--color-border);
}

.banner-content {
  max-width: 800px;
  margin: 0 auto;
}

.banner-content h2 {
  font-size: 28px;
  margin: 0 0 8px;
  color: var(--color-text);
}

.banner-content p {
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

.search-box {
  max-width: 600px;
  margin: 0 auto 20px;
}

.main-search :deep(.el-input__wrapper) {
  border-radius: 24px;
  padding-left: 16px;
}

.quick-filters {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.filter-tag {
  cursor: pointer;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.filter-tag:hover,
.filter-tag.active {
  color: var(--color-primary);
}

.filter-tag.active {
  font-weight: 600;
}

.books-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--color-text-secondary);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.import-hint {
  margin: 0 0 16px;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}
</style>
