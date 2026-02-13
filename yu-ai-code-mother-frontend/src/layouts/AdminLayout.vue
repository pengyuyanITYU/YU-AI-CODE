<template>
  <a-layout class="admin-layout">
    <!-- 左侧可折叠侧边栏 -->
    <AdminSidebar v-model:collapsed="sidebarCollapsed" />

    <!-- 右侧内容区域 -->
    <a-layout class="admin-main-layout" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <!-- 顶部简化导航栏 -->
      <a-layout-header class="admin-header">

      </a-layout-header>

      <!-- 页面内容区域 -->
      <a-layout-content class="admin-content">
        <div class="content-wrapper">
          <router-view />
        </div>
      </a-layout-content>

    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'
import AdminSidebar from '@/components/AdminSidebar.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
import {
  LogoutOutlined,
  HomeOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 侧边栏折叠状态（默认展开）
const sidebarCollapsed = ref(false)

// 返回首页
const goHome = () => {
  router.push('/')
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
}

/* 主内容区域布局 */
.admin-main-layout {
  flex: 1;
  margin-left: 220px;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  min-height: 100vh;
}

.admin-main-layout.sidebar-collapsed {
  margin-left: 80px;
}

/* 顶部导航栏 */
.admin-header {
  height: 72px;
  padding: 0 32px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.85) 0%, rgba(30, 41, 59, 0.75) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 用户区域 */
.user-login-status {
  display: flex;
  align-items: center;
}

.user-info {
  cursor: pointer;
  padding: 6px 12px 6px 16px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.2);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.2);
}

.user-name {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
}

.user-avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

.dropdown-icon {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.8);
  transition: transform 0.3s ease;
}

.user-info:hover .dropdown-icon {
  transform: rotate(180deg);
}

/* 内容区域 */
.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 72px - 70px - 48px);
}

/* 底部 */
.admin-footer {
  padding: 0;
  background: transparent;
}

/* 下拉菜单样式 */
:global(.user-dropdown-menu) {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.95) 100%) !important;
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.5);
  padding: 8px;
  border-radius: 12px;
  min-width: 160px;
}

:global(.user-dropdown-menu .ant-dropdown-menu-item) {
  color: rgba(255, 255, 255, 0.9);
  padding: 10px 16px;
  border-radius: 8px;
  margin-bottom: 2px;
  transition: all 0.2s ease;
}

:global(.user-dropdown-menu .ant-dropdown-menu-item:hover) {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

:global(.user-dropdown-menu .ant-dropdown-menu-divider) {
  background: rgba(148, 163, 184, 0.2);
  margin: 8px 0;
}

/* 彻底隐藏所有折叠菜单的悬浮子菜单 Tooltip - 最高优先级 */
:global(.ant-menu-submenu-popup),
:global([class*="ant-menu-submenu-popup"]),
:global(div.ant-menu-submenu-popup) {
  display: none !important;
  visibility: hidden !important;
  opacity: 0 !important;
  pointer-events: none !important;
  z-index: -9999 !important;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .admin-main-layout {
    margin-left: 0;
  }

  .admin-main-layout.sidebar-collapsed {
    margin-left: 0;
  }

  .admin-sidebar {
    transform: translateX(-100%);
  }

  .admin-sidebar.collapsed {
    transform: translateX(-100%);
  }
}
</style>
