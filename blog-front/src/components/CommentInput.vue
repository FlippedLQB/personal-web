<template>
  <!-- 评论输入框组件 -->
  <div class="comment-input">
    <!-- 未登录：置灰提示并跳转登录 -->
    <div v-if="!user.isLoggedIn" class="comment-login-tip" @click="goLogin">
      <n-input
        placeholder="请先登录后评论"
        :disabled="true"
        size="medium"
      />
      <n-button type="primary" size="medium" @click.stop="goLogin">
        去登录
      </n-button>
    </div>

    <!-- 已登录：评论输入框 -->
    <div v-else class="comment-form">
      <div v-if="replyTarget" class="reply-tip">
        <span>
          回复 <strong>{{ replyTarget.nickname }}</strong>
        </span>
        <n-button text size="small" @click="cancelReply">取消回复</n-button>
      </div>
      <n-input
        v-model:value="content"
        type="textarea"
        :rows="3"
        :placeholder="replyTarget ? `回复 ${replyTarget.nickname}...` : '写下你的评论...'"
        maxlength="500"
        show-count
        :disabled="submitting"
      />
      <div class="comment-form-actions">
        <span class="hint">文明发言，理性讨论</span>
        <n-button
          type="primary"
          :loading="submitting"
          :disabled="!content.trim()"
          @click="handleSubmit"
        >
          发布评论
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { NInput, NButton, useMessage } from 'naive-ui'
import { useUserStore } from '@/store/user'
import { addCommentApi } from '@/api/comment'

const props = defineProps({
  // 所属文章ID
  articleId: {
    type: [Number, String],
    required: true
  },
  // 当前回复目标（评论对象），为空则发表顶层评论
  replyTarget: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['success'])

const router = useRouter()
const user = useUserStore()
const message = useMessage()

const content = ref('')
const submitting = ref(false)

// 跳转登录页，携带 redirect
const goLogin = () => {
  router.push({
    path: '/login',
    query: { redirect: router.currentRoute.value.fullPath }
  })
}

// 取消回复
const cancelReply = () => {
  emit('success', { type: 'cancel' })
}

// 提交评论
const handleSubmit = async () => {
  const text = content.value.trim()
  if (!text) {
    message.warning('评论内容不能为空')
    return
  }
  if (!props.articleId) {
    message.error('文章ID缺失')
    return
  }

  const body = {
    articleId: props.articleId,
    content: text
  }

  // 如有回复目标，设置 parentId 和 replyToUserId
  if (props.replyTarget) {
    // 如果是回复"顶层评论"，parentId = 该评论id
    // 如果是回复"回复"，parentId 仍为顶层评论id（这里以 replyTarget.parentId 或 replyTarget.id 处理）
    body.parentId = props.replyTarget.parentId || props.replyTarget.id
    body.replyToUserId = props.replyTarget.userId
  }

  submitting.value = true
  try {
    const data = await addCommentApi(body)
    message.success('评论发布成功')
    content.value = ''
    emit('success', { type: 'submit', data })
  } catch (e) {
    // 错误已由响应拦截器统一提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.comment-input {
  margin-top: 16px;
}

.comment-login-tip {
  display: flex;
  gap: 8px;
  align-items: center;
  cursor: pointer;
  opacity: 0.85;
}

.comment-login-tip:hover {
  opacity: 1;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reply-tip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #888;
  background: #f6f8fa;
  padding: 6px 10px;
  border-radius: 4px;
}

.comment-form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint {
  font-size: 12px;
  color: #aaa;
}

@media (max-width: 640px) {
  .comment-login-tip {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
