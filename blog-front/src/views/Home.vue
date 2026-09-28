<template>
  <!-- 首页：文章流卡片列表 -->
  <div class="home">
    <!-- 搜索区 -->
    <div class="search-bar">
      <n-input
        v-model:value="keyword"
        placeholder="搜索文章标题或摘要"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <n-button type="primary" @click="handleSearch">搜索</n-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <n-spin size="large" />
      <p>正在加载文章...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!list.length" class="empty-state">
      <div class="empty-icon">📭</div>
      <p>暂无文章</p>
    </div>

    <!-- 文章列表 -->
    <div v-else class="article-list">
      <n-card
        v-for="item in list"
        :key="item.id"
        class="article-card"
        :bordered="false"
        hoverable
        @click="goDetail(item.id)"
      >
        <div class="article-row">
          <!-- 封面 -->
          <div class="article-cover">
            <img
              v-if="item.coverUrl"
              :src="item.coverUrl"
              :alt="item.title"
              @error="onImgError"
            />
            <div v-else class="cover-placeholder">
              <span>{{ item.title?.slice(0, 1) || 'B' }}</span>
            </div>
          </div>
          <!-- 内容 -->
          <div class="article-body">
            <h2 class="article-title">{{ item.title }}</h2>
            <p class="article-summary">{{ item.summary || '暂无摘要' }}</p>
            <div class="article-meta">
              <span>👁 {{ item.viewCount ?? 0 }} 浏览</span>
              <span v-if="item.createTime">🕒 {{ formatTime(item.createTime) }}</span>
            </div>
          </div>
        </div>
      </n-card>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-wrap">
      <n-pagination
        v-model:page="page"
        :page-count="pageCount"
        :page-size="size"
        show-quick-jumper
        @update:page="fetchList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard,
  NInput,
  NButton,
  NPagination,
  NSpin,
  useMessage
} from 'naive-ui'
import { getArticleListApi } from '@/api/article'

const router = useRouter()
const message = useMessage()

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

// 获取文章列表
const fetchList = async () => {
  loading.value = true
  try {
    const data = await getArticleListApi({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    // 兼容分页返回结构
    list.value = data?.list || []
    total.value = data?.total || 0
  } catch (e) {
    // 错误已由响应拦截器提示
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  page.value = 1
  fetchList()
}

// 跳转详情
const goDetail = (id) => {
  if (!id) return
  router.push(`/article/${id}`)
}

// 图片加载失败隐藏
const onImgError = (e) => {
  e.target.style.display = 'none'
}

// 时间格式化
const formatTime = (t) => {
  if (!t) return ''
  try {
    const d = new Date(t)
    if (isNaN(d.getTime())) return t
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  } catch (e) {
    return t
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.home {
  padding: 4px 0;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.loading-wrap {
  text-align: center;
  padding: 80px 0;
  color: #888;
}

.loading-wrap p {
  margin-top: 12px;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  cursor: pointer;
  border-radius: 8px;
}

.article-row {
  display: flex;
  gap: 16px;
}

.article-cover {
  flex-shrink: 0;
  width: 200px;
  height: 130px;
  border-radius: 6px;
  overflow: hidden;
  background: #f0f2f5;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4098fc, #6ad6ff);
  color: #fff;
  font-size: 48px;
  font-weight: 700;
}

.article-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #222;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-summary {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  margin-top: 8px;
  display: flex;
  gap: 16px;
  color: #999;
  font-size: 12px;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* 移动端：封面在上，内容在下 */
@media (max-width: 640px) {
  .article-row {
    flex-direction: column;
    gap: 12px;
  }
  .article-cover {
    width: 100%;
    height: 180px;
  }
  .article-title {
    font-size: 16px;
  -webkit-line-clamp: 1;
  }
}
</style>
