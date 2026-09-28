<template>
  <!-- 相册页：瀑布流/网格图片展示 -->
  <div class="album">
    <h1 class="page-title">公开相册</h1>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-wrap">
      <n-spin size="large" />
      <p>正在加载图片...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!list.length" class="empty-state">
      <div class="empty-icon">🖼</div>
      <p>暂无图片</p>
    </div>

    <!-- 瀑布流图片 -->
    <div v-else class="masonry">
      <div
        v-for="item in list"
        :key="item.id"
        class="masonry-item"
      >
        <n-image
          :src="item.url"
          :alt="item.description || '图片'"
          object-fit="cover"
          :preview-src="[item.url]"
          @error="onImgError(item)"
        />
        <p v-if="item.description" class="img-desc">{{ item.description }}</p>
      </div>
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
import {
  NImage,
  NPagination,
  NSpin,
  useMessage
} from 'naive-ui'
import { getImageListApi } from '@/api/image'

const message = useMessage()

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

// 获取图片列表
const fetchList = async () => {
  loading.value = true
  try {
    const data = await getImageListApi({ page: page.value, size: size.value })
    list.value = data?.list || []
    total.value = data?.total || 0
  } catch (e) {
    // 错误已由响应拦截器提示
  } finally {
    loading.value = false
  }
}

// 图片加载失败处理
const onImgError = (item) => {
  item.url = ''
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.album {
  padding: 4px 0;
}

.page-title {
  font-size: 22px;
  margin-bottom: 16px;
  color: #333;
}

.loading-wrap {
  text-align: center;
  padding: 80px 0;
  color: #888;
}

.loading-wrap p {
  margin-top: 12px;
}

/* CSS 瀑布流（多列方案） */
.masonry {
  column-count: 4;
  column-gap: 12px;
}

.masonry-item {
  break-inside: avoid;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.masonry-item :deep(img) {
  width: 100%;
  display: block;
}

.img-desc {
  padding: 8px 10px;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* 响应式列数 */
@media (max-width: 1024px) {
  .masonry {
    column-count: 3;
  }
}

@media (max-width: 720px) {
  .masonry {
    column-count: 2;
    column-gap: 8px;
  }
}
</style>
