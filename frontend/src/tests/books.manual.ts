/**
 * 书籍推送功能测试文件
 * 用于验证优化后的功能是否正常工作
 */

// ========== 测试 API 函数 ==========

/**
 * 测试 1: 分页获取推荐书籍
 */
export async function testGetBooksWithPagination() {
  console.log('🧪 Test 1: 分页获取推荐书籍')
  try {
    const response = await fetch('/api/books?page=0&size=10&sortBy=rating')
    const data = await response.json()

    console.assert(data.content !== undefined, '应返回 content 字段')
    console.assert(Array.isArray(data.content), 'content 应为数组')
    console.assert(data.totalElements !== undefined, '应返回 totalElements')

    console.log('✅ 分页获取成功，返回', data.content.length, '条数据')
    return data
  } catch (error) {
    console.error('❌ 分页获取失败:', error)
    return null
  }
}

/**
 * 测试 2: 按排序方式获取
 */
export async function testDifferentSortMethods() {
  console.log('🧪 Test 2: 按不同排序方式获取')
  const sortMethods = ['rating', 'hot', 'new']

  for (const sort of sortMethods) {
    try {
      const response = await fetch(`/api/books?page=0&size=5&sortBy=${sort}`)
      const data = await response.json()
      console.log(`✅ 排序方式 '${sort}' 返回`, data.content?.length, '条数据')
    } catch (error) {
      console.error(`❌ 排序方式 '${sort}' 失败:`, error)
    }
  }
}

/**
 * 测试 3: 搜索功能
 */
export async function testSearch() {
  console.log('🧪 Test 3: 搜索功能')
  const keywords = ['Python', '机器学习', '算法']

  for (const keyword of keywords) {
    try {
      const response = await fetch(`/api/books/search?keyword=${keyword}`)
      const data = await response.json()
      console.log(`✅ 搜索关键词 '${keyword}' 返回`, data.length, '条结果')
    } catch (error) {
      console.error(`❌ 搜索 '${keyword}' 失败:`, error)
    }
  }
}

/**
 * 测试 4: 按分类获取
 */
export async function testGetByCategory() {
  console.log('🧪 Test 4: 按分类获取书籍')
  const categories = ['技术', '文学', '历史']

  for (const category of categories) {
    try {
      const response = await fetch(`/api/books/category/${category}`)
      const data = await response.json()
      console.log(`✅ 分类 '${category}' 返回`, data.length, '条数据')
    } catch (error) {
      console.error(`❌ 分类 '${category}' 失败:`, error)
    }
  }
}

/**
 * 测试 5: 获取书籍详情
 */
export async function testGetBookDetail() {
  console.log('🧪 Test 5: 获取书籍详情')

  try {
    // 先获取一本书
    const listResponse = await fetch('/api/books?page=0&size=1')
    const listData = await listResponse.json()

    if (listData.content && listData.content.length > 0) {
      const bookId = listData.content[0].id
      const detailResponse = await fetch(`/api/books/${bookId}`)
      const detail = await detailResponse.json()

      console.assert(detail.id === bookId, 'ID 应匹配')
      console.assert(detail.title !== undefined, '应包含标题')
      console.assert(detail.author !== undefined, '应包含作者')

      console.log('✅ 书籍详情获取成功:', detail.title)
      return detail
    } else {
      console.warn('⚠️ 暂无书籍数据')
    }
  } catch (error) {
    console.error('❌ 获取详情失败:', error)
  }
}

// ========== 测试前端功能 ==========

/**
 * 测试 6: localStorage 收藏功能
 */
export function testLocalStorageFavorites() {
  console.log('🧪 Test 6: localStorage 收藏功能')

  // 清空
  localStorage.removeItem('favoriteBooks')

  // 添加收藏
  const favorites = ['book1', 'book2', 'book3']
  localStorage.setItem('favoriteBooks', JSON.stringify(favorites))

  // 读取验证
  const saved = localStorage.getItem('favoriteBooks')
  const parsed = saved ? JSON.parse(saved) : []

  console.assert(parsed.length === 3, '应保存 3 项')
  console.assert(parsed.includes('book1'), '应包含 book1')

  console.log('✅ localStorage 收藏功能正常')
}

/**
 * 测试 7: Set 数据结构管理
 */
export function testSetFavoriteManagement() {
  console.log('🧪 Test 7: Set 数据结构管理')

  const favoriteIds = new Set<string>()

  // 添加
  favoriteIds.add('book1')
  favoriteIds.add('book2')
  console.assert(favoriteIds.size === 2, '应有 2 项')

  // 检查
  console.assert(favoriteIds.has('book1'), 'book1 应存在')
  console.assert(!favoriteIds.has('book3'), 'book3 应不存在')

  // 删除
  favoriteIds.delete('book1')
  console.assert(favoriteIds.size === 1, '删除后应有 1 项')

  console.log('✅ Set 数据结构管理正常')
}

// ========== 集成测试 ==========

/**
 * 运行所有测试
 */
export async function runAllTests() {
  console.log('🎬 开始运行所有测试...\n')

  // 本地测试
  testLocalStorageFavorites()
  testSetFavoriteManagement()
  console.log('')

  // API 测试
  await testGetBooksWithPagination()
  await testDifferentSortMethods()
  await testSearch()
  await testGetByCategory()
  await testGetBookDetail()

  console.log('\n🎉 所有测试完成！')
}

// ========== 使用示例 ==========

/**
 * 在浏览器控制台中运行测试：
 *
 * 1. 导入测试模块
 *    import { runAllTests } from '@/tests/books.spec.ts'
 *
 * 2. 运行所有测试
 *    await runAllTests()
 *
 * 3. 或运行单个测试
 *    await testGetBooksWithPagination()
 *    await testSearch()
 *    testLocalStorageFavorites()
 */
