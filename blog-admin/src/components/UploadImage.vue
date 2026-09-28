<template>
  <div class="upload-image">
    <n-upload
      :show-file-list="false"
      :custom-request="handleCustomRequest"
      accept="image/*"
    >
      <n-button :loading="uploading" type="primary" v-bind="$attrs">
        <template v-if="uploading">上传中...</template>
        <template v-else>
          <slot>上传图片</slot>
        </template>
      </n-button>
    </n-upload>
    <div v-if="previewUrl" class="preview">
      <n-image :src="previewUrl" width="120" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useMessage } from 'naive-ui'
import Compressor from 'compressorjs'
import { uploadImage } from '@/api/upload'

const props = defineProps({
  // 上传成功后的图片地址（v-model）
  modelValue: { type: String, default: '' },
  // 压缩质量
  quality: { type: Number, default: 0.8 },
  // 最大宽度
  maxWidth: { type: Number, default: 1920 }
})

const emit = defineEmits(['update:modelValue', 'success'])

const message = useMessage()
const uploading = ref(false)
const previewUrl = ref(props.modelValue)

async function compressImage(file) {
  return new Promise((resolve, reject) => {
    // eslint-disable-next-line no-new
    new Compressor(file, {
      quality: props.quality,
      maxWidth: props.maxWidth,
      convertSize: 0,
      success(result) {
        resolve(result)
      },
      error(err) {
        reject(err)
      }
    })
  })
}

async function handleCustomRequest({ file }) {
  const raw = file.file
  if (!raw) return
  uploading.value = true
  try {
    // 压缩
    const compressed = await compressImage(raw)
    // 构造 form-data
    const formData = new FormData()
    // 保留原始扩展名
    const ext = raw.name.split('.').pop()
    const fileName = `upload_${Date.now()}.${ext}`
    formData.append('file', compressed, fileName)
    const res = await uploadImage(formData)
    const url = res.data?.url
    if (!url) throw new Error('上传响应缺少 url')
    previewUrl.value = url
    emit('update:modelValue', url)
    emit('success', res.data)
    message.success('上传成功')
  } catch (e) {
    message.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-image {
  display: inline-block;
}

.preview {
  margin-top: 8px;
}
</style>
