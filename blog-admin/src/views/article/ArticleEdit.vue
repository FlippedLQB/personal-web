<template>
  <div class="page-container">
    <div class="card-box">
      <div class="edit-header">
        <h2 class="edit-title">{{ isEdit ? '编辑文章' : '新建文章' }}</h2>
        <n-space>
          <n-button @click="goBack">返回</n-button>
          <n-button :loading="saving" @click="handleSave(0)">存为草稿</n-button>
          <n-button type="primary" :loading="saving" @click="handleSave(1)">发布</n-button>
        </n-space>
      </div>

      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top">
        <n-form-item label="标题" path="title">
          <n-input v-model:value="form.title" placeholder="请输入文章标题" maxlength="120" show-count />
        </n-form-item>
        <n-form-item label="封面图" path="cover">
          <upload-image v-model="form.cover" />
        </n-form-item>
        <n-form-item label="正文" path="content">
          <div class="wang-editor-container" style="width: 100%">
            <Toolbar
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              mode="default"
              style="border-bottom: 1px solid #e8e8e8"
            />
            <Editor
              v-model="form.content"
              :defaultConfig="editorConfig"
              mode="default"
              style="height: 480px; overflow-y: hidden"
              @onCreated="handleEditorCreated"
            />
          </div>
        </n-form-item>
      </n-form>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import UploadImage from '@/components/UploadImage.vue'
import { getArticleDetail, createArticle, updateArticle } from '@/api/article'
import { uploadImage } from '@/api/upload'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const articleId = route.params.id
const isEdit = !!articleId

const formRef = ref(null)
const saving = ref(false)

const form = reactive({
  id: articleId || undefined,
  title: '',
  cover: '',
  content: '',
  status: 0
})

const rules = {
  title: { required: true, message: '请输入文章标题', trigger: ['blur', 'input'] },
  content: { required: true, message: '请输入文章内容', trigger: ['blur', 'change'] }
}

// WangEditor 实例
const editorRef = shallowRef(null)
const toolbarConfig = {
  excludeKeys: ['group-video'] // 暂时屏蔽视频
}

const editorConfig = {
  placeholder: '请输入正文内容...',
  MENU_CONF: {
    uploadImage: {
      // 自定义上传：走 /api/upload/image
      async customUpload(file, insertFn) {
        try {
          const formData = new FormData()
          formData.append('file', file, file.name)
          const res = await uploadImage(formData)
          const url = res.data?.url
          if (!url) throw new Error('上传响应缺少 url')
          // insertFn(url, alt, href)
          insertFn(url, file.name, url)
        } catch (e) {
          message.error(e.message || '图片上传失败')
        }
      }
    }
  }
}

function handleEditorCreated(editor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) editor.destroy()
})

async function loadDetail() {
  if (!isEdit) return
  try {
    const res = await getArticleDetail(articleId)
    const data = res.data || {}
    form.title = data.title || ''
    form.cover = data.cover || ''
    form.content = data.content || ''
    form.status = data.status ?? 0
  } catch (e) {
    // 错误已提示
  }
}

async function handleSave(status) {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }
  if (!form.content || form.content.trim() === '<p><br></p>') {
    message.warning('请输入文章内容')
    return
  }
  saving.value = true
  try {
    const payload = {
      id: form.id,
      title: form.title,
      cover: form.cover,
      content: form.content,
      status
    }
    if (isEdit) {
      await updateArticle(payload)
    } else {
      await createArticle(payload)
    }
    message.success(status === 1 ? '发布成功' : '已保存草稿')
    router.push('/admin/article/list')
  } catch (e) {
    // 错误已提示
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/admin/article/list')
}

onMounted(loadDetail)
</script>

<style scoped>
.edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.edit-title {
  margin: 0;
  font-size: 18px;
  color: #1f2a44;
}
</style>
