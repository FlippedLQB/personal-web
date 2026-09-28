<template>
  <!-- 单条评论 / 回复 组件（支持两层嵌套） -->
  <div class="comment-item" :class="{ reply: isReply }">
    <div class="comment-avatar">
      <n-avatar round :size="40" :src="comment.avatar || defaultAvatar" />
    </div>
    <div class="comment-main">
      <div class="comment-head">
        <span class="comment-nickname">{{ comment.nickname }}</span>
        <template v-if="comment.replyToNickname && isReply">
          <span class="reply-arrow">回复</span>
          <span class="comment-nickname">{{ comment.replyToNickname }}</span>
        </template>
        <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
      </div>
      <div class="comment-content">{{ comment.content }}</div>
      <div class="comment-actions">
        <!-- 回复按钮：登录后显示 -->
        <n-button
          v-if="user.isLoggedIn"
          text
          size="small"
          @click="onReply(comment)"
        >
          回复
        </n-button>
        <!-- 删除按钮：仅本人评论才显示 -->
        <n-button
          v-if="canDelete"
          text
          size="small"
          type="error"
          @click="onDelete(comment)"
        >
          删除
        </n-button>
      </div>

      <!-- 二层回复列表 -->
      <div v-if="comment.replies && comment.replies.length" class="reply-list">
        <CommentItem
          v-for="reply in comment.replies"
          :key="reply.id"
          :comment="reply"
          :is-reply="true"
          :current-user-id="currentUserId"
          @reply="onReply"
          @delete="onDelete"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { NAvatar, NButton } from 'naive-ui'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'CommentItem' })

const props = defineProps({
  // 评论/回复对象
  comment: {
    type: Object,
    required: true
  },
  // 是否为回复（第二层）
  isReply: {
    type: Boolean,
    default: false
  },
  // 当前登录用户ID
  currentUserId: {
    type: [Number, String, null],
    default: null
  }
})

const emit = defineEmits(['reply', 'delete'])

const user = useUserStore()

// 默认头像（占位 SVG）
const defaultAvatar =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40"><rect width="40" height="40" fill="%23e0e6ed"/><text x="50%" y="50%" font-size="18" text-anchor="middle" dy=".35em" fill="%2399a">U</text></svg>'

// 是否可删除：登录 & 是本人
const canDelete = computed(() => {
  return (
    user.isLoggedIn &&
    props.currentUserId != null &&
    props.comment.userId === props.currentUserId
  )
})

// 触发回复
const onReply = (target) => {
  emit('reply', { target, isReply: props.isReply })
}

// 触发删除
const onDelete = (target) => {
  emit('delete', target)
}

// 时间格式化
const formatTime = (t) => {
  if (!t) return ''
  try {
    const d = new Date(t)
    if (isNaN(d.getTime())) return t
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const h = String(d.getHours()).padStart(2, '0')
    const mi = String(d.getMinutes()).padStart(2, '0')
    return `${y}-${m}-${day} ${h}:${mi}`
  } catch (e) {
    return t
  }
}
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
}

.comment-item.reply {
  padding: 8px 0;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #888;
  flex-wrap: wrap;
}

.comment-nickname {
  color: #4098fc;
  font-weight: 500;
}

.reply-arrow {
  color: #aaa;
  font-size: 12px;
}

.comment-time {
  color: #aaa;
  font-size: 12px;
  margin-left: auto;
}

.comment-content {
  margin: 6px 0;
  color: #333;
  word-break: break-word;
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  gap: 12px;
}

.reply-list {
  margin-top: 4px;
  padding-left: 8px;
  border-left: 2px solid #f0f2f5;
}

@media (max-width: 640px) {
  .comment-item {
    gap: 8px;
  }
  .reply-list {
    padding-left: 4px;
  }
}
</style>
