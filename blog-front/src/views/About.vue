<template>
  <!-- 关于我：简单静态页面 -->
  <div class="about page-card">
    <h1 class="title">关于我</h1>
    <div class="content">
      <p>你好，欢迎来到我的个人博客。</p>
      <p>
        这里是我记录生活、分享技术的角落。本站点使用
        Vue3 + Vite + VueRouter + Pinia + Axios + NaiveUI + DOMPurify
        构建，仅供学习与展示使用。
      </p>
      <p>站点定位：</p>
      <ul>
        <li>未登录用户：可浏览文章、相册、关于页，可查看评论。</li>
        <li>登录用户：可发表评论、回复评论、删除自己的评论。</li>
        <li>本前台不提供发文、图片上传功能（仅展示公开内容）。</li>
      </ul>
      <p>如果你喜欢这里的内容，欢迎留言交流。</p>
    </div>
  </div>
</template>

<script setup></script>

<style scoped>
.about {
  max-width: 800px;
  margin: 0 auto;
}

.title {
  font-size: 28px;
  margin-bottom: 16px;
  color: #333;
}

.content {
  color: #555;
  line-height: 1.9;
}

.content p {
  margin: 10px 0;
}

.content ul {
  margin: 10px 0;
  padding-left: 24px;
}

.content li {
  margin: 6px 0;
}
</style>
