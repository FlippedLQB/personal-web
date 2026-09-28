<template>
  <div class="page-container">
    <div class="card-box">
      <div class="search-bar">
        <h3 class="page-title">素材管理</h3>
        <div style="flex: 1"></div>
        <n-upload
          v-if="hasPerm('image:upload')"
          :show-file-list="false"
          :custom-request="handleUpload"
          accept="image/*"
          multiple
        >
          <n-button type="primary" :loading="uploading">+ 上传图片</n-button>
        </n-upload>
        <n-button @click="loadList">刷新</n-button>
      </div>

      <n-spin :show="loading">
        <div v-if="imageList.length === 0 && !loading" class="empty">
          <n-empty description="暂无素材" />
        </div>
        <div class="image-grid">
          <div v-for="item in imageList" :key="item.id" class="image-item">
            <div class="image-wrapper">
              <n-image :src="item.url" object-fit="cover" style="width: 100%; height: 140px" />
            </div>
            <div class="image-info">
              <n-tooltip trigger="hover">
                <template #trigger>
                  <div class="image-name">{{ item.fileName }}</div>
                </template>
                {{ item.fileName }}
              </n-tooltip>
              <div class="image-actions">
                <n-button size="tiny" @click="copyLink(item.url)">复制链接</n-button>
                <n-button
                  v-if="hasPerm('image:delete')"
                  size="tiny"
                  type="error"
                  ghost
                  @click="handleDelete(item)"
                >
                  删除
                </n-button>
              </div>
            </div>
          </div>
        </div>
      </n-spin>

      <div class="pagination">
        <n-pagination
          v-model:page="query.page"
          :page-size="query.size"
          :item-count="total"
          :page-sizes="[20, 40, 60]"
          show-size-picker
          @update:page="loadList"
          @update:page-size="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import Compressor from 'compressorjs'
import { getUploadList, deleteUpload, uploadImage } from '@/api/upload'
import { hasPerm } from '@/utils/perm'

const message = useMessage()
const dialog = useDialog()

const query = reactive({ page: 1, size: 20 })
const total = ref(0)
const loading = ref(false)
const uploading = ref(false)
const imageList = ref([])

async function loadList() {
  loading.value = true
  try {
    const res = await getUploadList(query)
    const data = res.data || {}
    imageList.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    // 错误已提示
  } finally {
    loading.value = false
  }
}

function handleSizeChange(size) {
  query.size = size
  query.page = 1
  loadList()
}

// 压缩
function compress(file) {
  return new Promise((resolve, reject) => {
    // eslint-disable-next-line no-new
    new Compressor(file, {
      quality: 0.8,
      maxWidth: 1920,
      convertSize: 0,
      success: resolve,
      error: reject
    })
  })
}

async function handleUpload({ file }) {
  const raw = file.file
  if (!raw) return
  uploading.value = true
  try {
    const compressed = await compress(raw)
    const formData = new FormData()
    const ext = raw.name.split('.').pop()
    formData.append('file', compressed, `upload_${Date.now()}.${ext}`)
    await uploadImage(formData)
    message.success('上传成功')
    loadList()
  } catch (e) {
    message.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function copyLink(url) {
  try {
    await navigator.clipboard.writeText(url)
    message.success('链接已复制')
  } catch (e) {
    // 降级
    const input = document.createElement('input')
    input.value = url
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    input.remove()
    message.success('链接已复制')
  }
}

function handleDelete(item) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除「${item.fileName}」吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteUpload(item.id)
        message.success('删除成功')
        loadList()
      } catch (e) {
        // 错误已提示
      }
    }
  })
}

onMounted(loadList)
</script>

<style scoped>
.page-title {
  margin: 0;
  font-size: 16px;
  color: #1f2a44;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.image-item {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 6px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.image-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.image-wrapper {
  width: 100%;
  height: 140px;
  background: #fafafa;
  overflow: hidden;
}

.image-info {
  padding: 8px 10px;
}

.image-name {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}

.image-actions {
  display: flex;
  gap: 6px;
}

.empty {
  padding: 40px 0;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
