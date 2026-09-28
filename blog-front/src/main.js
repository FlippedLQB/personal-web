import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './assets/main.css'

// 创建Vue应用实例
const app = createApp(App)

// 注册 Pinia 与 路由
app.use(createPinia())
app.use(router)

app.mount('#app')
