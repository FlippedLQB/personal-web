<template>
  <!-- 文章详情页 -->
  <div class="article-detail">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <n-spin size="large" />
      <p>正在加载文章...</p>
    </div>

    <!-- 加载失败 / 不存在 -->
    <div v-else-if="!article" class="empty-state">
      <div class="empty-icon">📄</div>
      <p>文章不存在或加载失败</p>
      <n-button @click="router.push('/')">返回首页</n-button>
    </div>

    <!-- 文章主体 -->
    <article v-else class="page-card">
      <h1 class="article-title">{{ article.title }}</h1>
      <div class="article-meta">
        <span>作者：{{ article.author || '博主' }}</span>
        <span>👁 {{ article.viewCount ?? 0 }} 浏览</span>
        <span v-if="article.createTime">🕒 {{ formatTime(article.createTime) }}</span>
      </div>

      <!-- 富文本渲染（用 DOMPurify 清洗 HTML 防XSS） -->
      <div
        class="article-content"
        v-html="sanitizedContent"
      ></div>
    </article>

    <!-- 评论区 -->
    <section v-if="article" class="comment-section page-card">
      <h2 class="section-title">评论 <span class="count">({{ commentTotal }})</span></h2>

      <!-- 评论输入框 -->
      <CommentInput
        :article-id="articleId"
        :reply-target="replyTarget"
        @success="handleCommentEvent"
      />

      <!-- 评论列表 -->
      <div class="comment-list">
        <div v-if="commentLoading" class="loading-wrap small">
          <n-spin />
        </div>
        <div v-else-if="!comments.length" class="empty-state small">
          <p>还没有评论，来抢沙发吧～</p>
        </div>
        <template v-else>
          <CommentItem
            v-for="c in comments"
            :key="c.id"
            :comment="c"
            :current-user-id="user.userId"
            @reply="handleReply"
            @delete="handleDelete"
          />
          <div v-if="commentTotal > commentSize" class="pagination-wrap">
            <n-pagination
              v-model:page="commentPage"
              :page-count="commentPageCount"
              :page-size="commentSize"
              @update:page="fetchComments"
            />
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DOMPurify from 'dompurify'
import {
  NSpin,
  NButton,
  NPagination,
  useMessage,
  useDialog
} from 'naive-ui'
import { getArticleDetailApi } from '@/api/article'
import { getCommentListApi, deleteCommentApi } from '@/api/comment'
import { useUserStore } from '@/store/user'
import CommentItem from '@/components/CommentItem.vue'
import CommentInput from '@/components/CommentInput.vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const message = useMessage()
const dialog = useDialog()

const articleId = computed(() => route.params.id)

const article = ref(null)
const loading = ref(false)

// 富文本清洗
const sanitizedContent = computed(() => {
  if (!article.value?.content) return ''
  return DOMPurify.sanitize(article.value.content)
})

// ===== 评论相关 =====
const comments = ref([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentSize = ref(10)
const commentLoading = ref(false)
const replyTarget = ref(null)

const commentPageCount = computed(() =>
  Math.max(1, Math.ceil(commentTotal.value / commentSize.value))
)

// 获取文章详情
const fetchArticle = async () => {
  loading.value = true
  try {
    const data = await getArticleDetailApi(articleId.value)
    article.value = data
  } catch (e) {
    article.value = null
  } finally {
    loading.value = false
  }
}

// 获取评论列表
const fetchComments = async () => {
  commentLoading.value = true
  try {
    const data = await getCommentListApi(articleId.value, {
      page: commentPage.value,
      size: commentSize.value
    })
    comments.value = data?.list || []
    commentTotal.value = data?.total || 0
  } catch (e) {
    // 错误已由响应拦截器提示
  } finally {
    commentLoading.value = false
  }
}

// 点击回复
const handleReply = ({ target }) => {
  if (!user.isLoggedIn) {
    message.warning('请先登录后回复')
    router.push({
      path: '/login',
      query: { redirect: route.fullPath }
    })
    return
  }
  replyTarget.value = target
  // 滚动到评论输入框
  setTimeout(() => {
    document.querySelector('.comment-section .comment-input')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, 50)
}

// 删除评论
const handleDelete = (target) => {
  if (!target?.id) return
  dialog.warning({
    title: '删除评论',
    content: '确定要删除该评论吗？删除后不可恢复。',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteCommentApi(target.id)
        message.success('删除成功')
        // 删除后刷新
        fetchComments()
      } catch (e) {
        // 错误已由响应拦截器提示
      }
    }
  })
}

// 评论输入框事件回调
const handleCommentEvent = (payload) => {
  if (payload?.type === 'cancel') {
    replyTarget.value = null
    return
  }
  // 提交成功
  replyTarget.value = null
  commentPage.value = 1
  fetchComments()
}

// 时间格式化
const formatTime = (t) => {
  if (!t) return ''
  try {
    const d = new Date(t)
    if (isNaN(d.getTime())) return t
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch (e) {
    return t
  }
}

// 监听路由参数变化（切换文章时重新加载）
watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      article.value = null
      comments.value = []
      commentPage.value = 1
      fetchArticle()
      fetchComments()
    }
  }
)

onMounted(() => {
  fetchArticle()
  fetchComments()
})
</script>

<style scoped>
.article-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px 0;
}

.loading-wrap {
  text-align: center;
  padding: 80px 0;
  color: #888;
}

.loading-wrap.small {
  padding: 40px 0;
}

.loading-wrap p {
  margin-top: 12px;
}

.article-title {
  font-size: 26px;
  font-weight: 700;
  color: #222;
  margin-bottom: 12px;
  line-height: 1.4;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #999;
  font-size: 13px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}

.article-content {
  margin-top: 12px;
  font-size: 15px;
  line-height: 1.8;
}

.section-title {
  font-size: 18px;
  color: #333;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.count {
  color: #999;
  font-size: 14px;
  font-weight: normal;
}

.comment-list {
  margin-top: 12px;
  border-top: 1px solid #f0f2f5;
  padding-top: 8px;
}

.empty-state.small {
  padding: 30px 0;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  .article-title {
    font-size: 20px;
  }
  .article-meta {
    gap: 8px;
    font-size: 12px;
  }
  .article-content {
    font-size: 14px;
  }
}
</style>
