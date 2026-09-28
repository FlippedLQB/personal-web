import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import { setupPermDirective } from './utils/perm'
import './assets/main.css'

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Pinia 状态管理
app.use(createPinia())
// 注册路由
app.use(router)

// 注册全局自定义指令 v-perm
setupPermDirective(app)

app.mount('#app')
