<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { 
  addApp, 
  listMyAppVoByPage, 
  listGoodAppVoByPage, 
  updateAppVisualRange, 
  deleteApp,
  applyForFeatured,
  updateMyPriority
} from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
  // 滚动到输入框
  const inputEl = document.querySelector('.input-section')
  if (inputEl) {
    inputEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入您的创意想法')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录以保存您的作品')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('AI 正在为您构建应用...')
      // 跳转到对话页面，确保ID是字符串类型
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 切换可见范围
const toggleVisualRange = async (app: API.AppVO) => {
  if (!app.id) {
    return
  }

  const newVisualRange = !app.visualRange

  try {
    const res = await updateAppVisualRange({
      appId: app.id,
      visualRange: newVisualRange,
    })

    if (res.data.code === 0) {
      message.success(newVisualRange ? '已设置为公开' : '已设置为私有')
      app.visualRange = newVisualRange
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (error) {
    console.error('切换可见范围失败：', error)
    message.error('操作失败，请重试')
  }
}

const handleDeleteApp = async (app: API.AppVO) => {
  if (!app.id) {
    return
  }

  try {
    const res = await deleteApp({ id: app.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      loadMyApps()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 申请精选
const handleApplyFeatured = async (app: API.AppVO) => {
  if (!app.id) return
  try {
    const res = await applyForFeatured({ appId: app.id })
    if (res.data.code === 0) {
      message.success('申请提交成功，请等待管理员审核')
      loadMyApps()
    } else {
      message.error('申请失败：' + res.data.message)
    }
  } catch (error) {
    console.error('申请精选失败：', error)
    message.error('申请失败，请重试')
  }
}

// 切换置顶
const handleTogglePin = async (app: API.AppVO) => {
  if (!app.id) return
  const newPriority = app.userPriority && app.userPriority > 0 ? 0 : 999
  try {
    const res = await updateMyPriority({ appId: app.id, userPriority: newPriority })
    if (res.data.code === 0) {
      message.success(newPriority > 0 ? '已置顶' : '已取消置顶')
      loadMyApps()
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch (error) {
    console.error('切换置顶失败：', error)
    message.error('操作失败，请重试')
  }
}

onMounted(() => {
  loadMyApps()
  loadFeaturedApps()

  // 鼠标跟随光效逻辑优化
  const handleMouseMove = (e: MouseEvent) => {
    const { clientX, clientY } = e
    // 使用 requestAnimationFrame 优化性能
    requestAnimationFrame(() => {
      document.documentElement.style.setProperty('--mouse-x', `${clientX}px`)
      document.documentElement.style.setProperty('--mouse-y', `${clientY}px`)
    })
  }

  document.addEventListener('mousemove', handleMouseMove)

  return () => {
    document.removeEventListener('mousemove', handleMouseMove)
  }
})
</script>

<template>
  <div id="homePage">
    <!-- 背景装饰层 -->
    <div class="bg-decoration">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="grid-overlay"></div>
    </div>

    <div class="container">
      <!-- 头部区域 -->
      <header class="hero-section fade-in-up">
        <div class="badge">AI Powered Platform</div>
        <h1 class="hero-title">
          让创意 <span class="gradient-text">瞬间落地</span>
        </h1>
        <p class="hero-description">
          无需编写代码，只需一句话，AI 帮您构建全功能 Web 应用。
        </p>
      </header>

      <!-- 核心输入区域 -->
      <section class="input-wrapper fade-in-up delay-1">
        <div class="glass-panel input-box">
          <a-textarea
            v-model:value="userPrompt"
            placeholder="描述您想创建的应用... 例如：帮我做一个支持暗黑模式的个人摄影作品集网站"
            :auto-size="{ minRows: 3, maxRows: 6 }"
            :bordered="false"
            class="custom-textarea"
            @keyup.enter="createApp"
          />
          <div class="input-footer">
            <span class="hint-text">
              <span class="icon">✨</span> 支持 Markdown 格式描述
            </span>
            <a-button
              type="primary"
              class="generate-btn"
              @click="createApp"
              :loading="creating"
            >
              {{ creating ? '生成中...' : '立即生成' }}
              <template #icon v-if="!creating">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M5 12H19M19 12L12 5M19 12L12 19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </template>
            </a-button>
          </div>
        </div>
      </section>

      <!-- 灵感快捷键 -->
      <section class="quick-actions fade-in-up delay-2">
        <div class="quick-title">试试这些灵感：</div>
        <div class="chips-container">
          <button
            class="chip"
            @click="setPrompt('创建一个极简主义的个人博客，包含文章归档、关于我页面，支持Markdown渲染，主色调为深灰色。')"
          >
            📝 个人博客
          </button>
          <button
            class="chip"
            @click="setPrompt('设计一个SaaS产品的落地页，包含Hero区域、功能特性网格、定价表格和FAQ部分，风格现代科技感。')"
          >
            🚀 产品落地页
          </button>
          <button
            class="chip"
            @click="setPrompt('构建一个在线待办事项管理工具，支持任务分组、优先级标记、拖拽排序，数据保存在本地存储。')"
          >
            ✅ 任务管理
          </button>
          <button
            class="chip"
            @click="setPrompt('制作一个摄影师作品集网站，使用瀑布流布局展示照片，点击可查看大图，包含联系方式表单。')"
          >
            📷 摄影作品集
          </button>
        </div>
      </section>

      <!-- 我的作品 -->
      <section class="content-section fade-in-up delay-3" v-if="myApps.length > 0">
        <div class="section-header">
          <h2 class="section-title">我的工作台</h2>
          <span class="section-subtitle">管理您创建的应用</span>
        </div>

        <div class="app-grid">
          <AppCard
            v-for="app in myApps"
            :key="app.id"
            :app="app"
            @view-chat="viewChat"
            @view-work="viewWork"
            @toggle-visual-range="toggleVisualRange"
            @delete-app="handleDeleteApp"
            @apply-featured="handleApplyFeatured"
            @toggle-pin="handleTogglePin"
            class="app-card-item"
          />
        </div>

        <div class="pagination-wrapper" v-if="myAppsPage.total > myAppsPage.pageSize">
          <a-pagination
            v-model:current="myAppsPage.current"
            v-model:page-size="myAppsPage.pageSize"
            :total="myAppsPage.total"
            show-less-items
            @change="loadMyApps"
          />
        </div>
      </section>

      <!-- 精选案例 -->
      <section class="content-section fade-in-up delay-3">
        <div class="section-header">
          <h2 class="section-title">社区精选</h2>
          <span class="section-subtitle">探索其他创作者的精彩应用</span>
        </div>

        <div class="featured-grid">
          <AppCard
            v-for="app in featuredApps"
            :key="app.id"
            :app="app"
            :featured="true"
            @view-chat="viewChat"
            @view-work="viewWork"
            class="app-card-item"
          />
        </div>

        <div class="pagination-wrapper">
          <a-pagination
            v-model:current="featuredAppsPage.current"
            v-model:page-size="featuredAppsPage.pageSize"
            :total="featuredAppsPage.total"
            show-less-items
            @change="loadFeaturedApps"
          />
        </div>
      </section>
    </div>

  <!--    &lt;!&ndash; 底部版权简单展示 &ndash;&gt;-->
  <!--    <footer class="simple-footer">-->
  <!--      <p>© 2026 AI App Generator. Powered by 鱼</p>-->
  <!--    </footer>-->
  </div>
</template>

<style scoped>
/* 全局变量与基础设置 */
:root {
  --primary-color: #3b82f6;
  --primary-hover: #2563eb;
  --bg-color: #f8fafc;
  --text-main: #1e293b;
  --text-sub: #64748b;
  --glass-bg: rgba(255, 255, 255, 0.7);
  --glass-border: rgba(255, 255, 255, 0.8);
  --card-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  --glow-color: rgba(59, 130, 246, 0.15);
}

#homePage {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  color: rgba(255, 255, 255, 0.95);
  overflow-x: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 动态背景 */
.bg-decoration {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.10) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.10) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(600px 420px at 50% 40%, rgba(0,0,0,0.85), transparent 70%);
  opacity: 0.35;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.65;
  animation: float 20s infinite ease-in-out;
}

.orb-1 {
  width: 420px;
  height: 420px;
  background: rgba(56, 189, 248, 0.9);
  top: -140px;
  left: -120px;
  animation-delay: 0s;
}

.orb-2 {
  width: 360px;
  height: 360px;
  background: rgba(168, 85, 247, 0.9);
  bottom: -140px;
  right: -140px;
  animation-delay: -5s;
}

/* 光标跟随高亮 */
#homePage::after {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.12) 0%, transparent 70%);
  transform: translate(calc(var(--mouse-x, -100%) - 50%), calc(var(--mouse-y, -100%) - 50%));
  pointer-events: none;
  z-index: 1;
  transition: transform 0.1s linear;
}

/* 布局容器 */
.container {
  position: relative;
  z-index: 2;
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 24px 40px;
}

/* Hero 区域 */
.hero-section {
  text-align: center;
  margin-bottom: 60px;
}

.badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  background: rgba(59, 130, 246, 0.15);
  color: rgba(59, 130, 246, 0.9);
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 24px;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.hero-title {
  font-size: 4rem;
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 24px;
  letter-spacing: -0.02em;
  color: rgba(255, 255, 255, 0.98);
}

.gradient-text {
  background: linear-gradient(135deg, #ffffff 0%, rgba(59, 130, 246, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-description {
  font-size: 1.25rem;
  color: rgba(148, 163, 184, 0.9);
  max-width: 600px;
  margin: 0 auto;
  line-height: 1.6;
}

/* 输入框区域 */
.input-wrapper {
  max-width: 860px;
  margin: 0 auto 40px;
}

.input-box {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 24px;
  padding: 8px;
  box-shadow: 
    0 32px 80px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.input-box:focus-within {
  transform: translateY(-2px);
  box-shadow:
    0 40px 100px rgba(59, 130, 246, 0.25),
    0 0 0 2px rgba(59, 130, 246, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.custom-textarea {
  font-size: 1.1rem;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.95);
  padding: 16px 20px;
  border-radius: 16px !important;
  background: transparent;
}

/* 覆盖 Ant Design textarea 默认样式 */
:deep(.ant-input) {
  resize: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.95);
}
:deep(.ant-input:focus) {
  box-shadow: none;
}
:deep(.ant-input::placeholder) {
  color: rgba(148, 163, 184, 0.6);
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px 12px;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
  margin-top: 4px;
}

.hint-text {
  font-size: 0.9rem;
  color: rgba(148, 163, 184, 0.8);
  display: flex;
  align-items: center;
  gap: 6px;
}

.generate-btn {
  height: 44px;
  padding: 0 28px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 1rem;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  transition: all 0.3s ease;
}

.generate-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
  filter: brightness(1.05);
}

/* 快捷操作 Chips */
.quick-actions {
  text-align: center;
  margin-bottom: 80px;
}

.quick-title {
  font-size: 0.9rem;
  color: #64748b;
  margin-bottom: 16px;
  font-weight: 500;
}

.chips-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  max-width: 900px;
  margin: 0 auto;
}

.chip {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.2);
  padding: 10px 20px;
  border-radius: 100px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.chip:hover {
  border-color: rgba(59, 130, 246, 0.5);
  color: rgba(59, 130, 246, 1);
  background: rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

/* 内容区域通用样式 */
.content-section {
  margin-bottom: 80px;
}

.section-header {
  margin-bottom: 32px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  border-left: 4px solid rgba(59, 130, 246, 0.8);
  padding-left: 16px;
}

.section-title {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0;
  color: rgba(255, 255, 255, 0.98);
  background: linear-gradient(135deg, #ffffff 0%, rgba(59, 130, 246, 0.9) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.section-subtitle {
  font-size: 1rem;
  color: rgba(148, 163, 184, 0.9);
  margin-top: 4px;
}

/* 卡片网格 */
.app-grid, .featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 32px;
  margin-bottom: 40px;
}

/* 确保卡片组件在容器中表现良好 */
:deep(.ant-card) {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  overflow: hidden;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.75) 0%, rgba(15, 23, 42, 0.85) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
}

:deep(.ant-card:hover) {
  transform: translateY(-5px);
  box-shadow: 0 32px 80px rgba(59, 130, 246, 0.25);
  border-color: rgba(59, 130, 246, 0.3);
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
}

/* 底部 */
.simple-footer {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
  font-size: 0.9rem;
  position: relative;
  z-index: 2;
}

/* 动画类 */
.fade-in-up {
  opacity: 0;
  animation: fadeInUp 0.8s ease-out forwards;
}

.delay-1 { animation-delay: 0.2s; }
.delay-2 { animation-delay: 0.4s; }
.delay-3 { animation-delay: 0.6s; }

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20px, -20px); }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }

  .container {
    padding-top: 40px;
  }

  .app-grid, .featured-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .input-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .generate-btn {
    width: 100%;
    justify-content: center;
  }

  .chips-container {
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 10px;
    /* 隐藏滚动条 */
    -ms-overflow-style: none;
    scrollbar-width: none;
  }
  .chips-container::-webkit-scrollbar {
    display: none;
  }

  .chip {
    white-space: nowrap;
    flex-shrink: 0;
  }
}
</style>
