<template>
  <div class="admin-sidebar" :class="{ collapsed: collapsed }">
    <!-- 侧边栏头部 - Logo区域 -->
    <div class="sidebar-header">
      <div class="logo-section">
        <div class="logo-wrapper">
          <svg class="logo" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="sidebarFishGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#2563eb" />
                <stop offset="50%" stop-color="#3b82f6" />
                <stop offset="100%" stop-color="#06b6d4" />
              </linearGradient>
              <filter id="sidebarGlowFilter" x="-50%" y="-50%" width="200%" height="200%">
                <feGaussianBlur stdDeviation="2.5" result="coloredBlur" />
                <feMerge>
                  <feMergeNode in="coloredBlur" />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>
            </defs>
            <g filter="url(#sidebarGlowFilter)">
              <path
                d="M75 35C75 35 65 20 45 20C25 20 15 40 15 60C15 80 30 90 50 85C70 80 80 65 80 50"
                stroke="url(#sidebarFishGradient)"
                stroke-width="8"
                stroke-linecap="round"
                stroke-linejoin="round"
                fill="none"
              />
              <circle cx="65" cy="35" r="4" fill="#1e293b" />
              <circle cx="85" cy="30" r="3" fill="url(#sidebarFishGradient)" opacity="0.8" />
              <circle cx="92" cy="18" r="2" fill="url(#sidebarFishGradient)" opacity="0.6" />
            </g>
          </svg>
        </div>
        <span v-if="!collapsed" class="logo-text">管理后台</span>
      </div>
      <!-- 折叠按钮 -->
      <div class="collapse-btn" @click="toggleCollapse">
        <MenuFoldOutlined v-if="!collapsed" />
        <MenuUnfoldOutlined v-else />
      </div>
    </div>

    <!-- 菜单区域 -->
    <div class="sidebar-menu-container">
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        :inline-collapsed="collapsed"
        :items="menuItems"
        @click="handleMenuClick"
        class="admin-menu"
      />
    </div>

    <!-- 底部区域 - 用户信息 -->
    <div class="sidebar-footer">
      <a-divider v-if="!collapsed" class="footer-divider" />
      <div class="user-section" @click="handleUserClick">
        <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="collapsed ? 36 : 32" class="user-avatar" />
        <div v-if="!collapsed" class="user-info">
          <span class="user-name">{{ loginUserStore.loginUser.userName ?? '无名' }}</span>
          <span class="user-role">管理员</span>
        </div>
        <LogoutOutlined v-if="!collapsed" class="logout-icon" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
import {
  UserOutlined,
  AppstoreOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UnorderedListOutlined,
  FileTextOutlined,
  HomeOutlined,
  LogoutOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'
import { message } from 'ant-design-vue'

interface Props {
  collapsed?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  collapsed: false,
})

const emit = defineEmits<{
  'update:collapsed': [value: boolean]
}>()

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

// 当前选中的菜单项
const selectedKeys = ref<string[]>([route.path])
// 展开的子菜单
const openKeys = ref<string[]>(['/admin/appManage_parent'])

// 子菜单路由映射到父菜单key
const subMenuToParent: Record<string, string> = {
  '/admin/appManage': '/admin/appManage_parent',
  '/admin/appReview': '/admin/appManage_parent',
}

// 监听路由变化更新选中状态
watch(
  () => route.path,
  (newPath) => {
    // 折叠状态下，子菜单页面选中父菜单
    if (props.collapsed && subMenuToParent[newPath]) {
      selectedKeys.value = [subMenuToParent[newPath]]
    } else {
      selectedKeys.value = [newPath]
    }

    // 如果是应用管理下的页面，保持展开（仅在非折叠状态）
    if (!props.collapsed && (newPath === '/admin/appManage' || newPath === '/admin/appReview')) {
      if (!openKeys.value.includes('/admin/appManage_parent')) {
        openKeys.value.push('/admin/appManage_parent')
      }
    }
  },
  { immediate: true }
)

// 监听折叠状态变化
watch(
  () => props.collapsed,
  (isCollapsed) => {
    if (isCollapsed) {
      // 折叠时清空展开的子菜单，防止显示悬浮菜单
      openKeys.value = []
      // 如果当前在子菜单页面，选中父菜单
      const currentPath = route.path
      if (subMenuToParent[currentPath]) {
        selectedKeys.value = [subMenuToParent[currentPath]]
      }
    } else {
      // 展开时，如果当前在子菜单页面，展开父菜单
      const currentPath = route.path
      if (subMenuToParent[currentPath]) {
        openKeys.value = [subMenuToParent[currentPath]]
        selectedKeys.value = [currentPath]
      }
    }
  }
)

// 菜单配置
const menuItems = computed<MenuProps['items']>(() => [
  {
    key: '/admin',
    icon: () => h(HomeOutlined),
    label: '首页',
    title: '首页',
  },
  {
    key: '/admin/userManage',
    icon: () => h(UserOutlined),
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage_parent',
    icon: () => h(AppstoreOutlined),
    label: '应用管理',
    title: '应用管理',
    children: [
      {
        key: '/admin/appManage',
        icon: () => h(UnorderedListOutlined),
        label: '应用列表',
        title: '应用列表',
      },
      {
        key: '/admin/appReview',
        icon: () => h(FileTextOutlined),
        label: '用户申请',
        title: '用户申请',
      },
    ],
  },
])

// 切换折叠状态
const toggleCollapse = () => {
  emit('update:collapsed', !props.collapsed)
}

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  if (key.endsWith('_parent')) {
    return
  }
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 处理用户区域点击
const handleUserClick = async () => {
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
.admin-sidebar {
  width: 220px;
  height: 100vh;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.95) 0%, rgba(30, 41, 59, 0.9) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(148, 163, 184, 0.15);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: fixed;
  left: 0;
  top: 0;
  z-index: 200;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.3);
}

.admin-sidebar.collapsed {
  width: 80px;
}

.admin-sidebar.collapsed .sidebar-header {
  justify-content: space-between;
  padding: 0 12px;
}

.admin-sidebar.collapsed .logo-text {
  display: none;
}

.admin-sidebar.collapsed .logo-section {
  flex: 0 0 auto;
}

/* 头部区域 */
.sidebar-header {
  height: 72px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  position: relative;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}

.logo-wrapper {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.logo {
  width: 28px;
  height: 28px;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  white-space: nowrap;
  background: linear-gradient(135deg, #ffffff 0%, rgba(59, 130, 246, 0.9) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  color: rgba(148, 163, 184, 0.8);
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.95);
}

/* 菜单区域 */
.sidebar-menu-container {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-menu-container::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu-container::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-menu-container::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.2);
  border-radius: 2px;
}

:deep(.admin-menu) {
  background: transparent !important;
  border-right: none !important;
}

:deep(.admin-menu .ant-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 4px 0;
  padding: 0 16px !important;
  border-radius: 10px;
  color: rgba(148, 163, 184, 0.9);
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.admin-menu .ant-menu-item:hover) {
  color: rgba(255, 255, 255, 0.98) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.admin-menu .ant-menu-item-selected) {
  background: rgba(59, 130, 246, 0.2) !important;
  color: rgba(59, 130, 246, 1) !important;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.3);
}

:deep(.admin-menu .ant-menu-submenu) {
  margin: 4px 0;
}

:deep(.admin-menu .ant-menu-submenu-title) {
  height: 44px;
  line-height: 44px;
  padding: 0 16px !important;
  border-radius: 10px;
  color: rgba(148, 163, 184, 0.9);
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.admin-menu .ant-menu-submenu-title:hover) {
  color: rgba(255, 255, 255, 0.98) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.admin-menu .ant-menu-submenu-selected > .ant-menu-submenu-title) {
  color: rgba(59, 130, 246, 1) !important;
}

:deep(.admin-menu .ant-menu-sub) {
  background: transparent !important;
}

:deep(.admin-menu .ant-menu-sub .ant-menu-item) {
  padding-left: 48px !important;
}

:deep(.admin-menu .anticon) {
  font-size: 16px;
  margin-right: 10px;
}

/* 折叠状态下的菜单 */
:deep(.admin-menu.ant-menu-inline-collapsed) {
  width: 56px;
}

:deep(.admin-menu.ant-menu-inline-collapsed .ant-menu-item) {
  padding: 0 16px !important;
}

:deep(.admin-menu.ant-menu-inline-collapsed .anticon) {
  font-size: 18px;
  margin-right: 0;
}

/* 底部区域 */
.sidebar-footer {
  padding: 12px 16px 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
}

.footer-divider {
  margin: 0 0 12px 0 !important;
  border-color: rgba(148, 163, 184, 0.1) !important;
  min-width: auto !important;
}

/* 用户区域 */
.user-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid transparent;
}

.user-section:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(148, 163, 184, 0.2);
}

.user-avatar {
  border: 2px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
  flex-shrink: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.user-name {
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 11px;
  color: rgba(148, 163, 184, 0.7);
  margin-top: 2px;
}

.logout-icon {
  color: rgba(148, 163, 184, 0.6);
  font-size: 14px;
  transition: all 0.3s ease;
}

.user-section:hover .logout-icon {
  color: rgba(255, 100, 100, 0.9);
  transform: translateX(2px);
}

/* 折叠状态下的用户区域 */
.admin-sidebar.collapsed .user-section {
  justify-content: center;
  padding: 8px;
}

.admin-sidebar.collapsed .user-avatar {
  margin: 0;
}

/* 确保折叠状态下的父菜单项有正确的选中样式 */
:deep(.admin-menu.ant-menu-inline-collapsed .ant-menu-submenu-selected > .ant-menu-submenu-title) {
  background: rgba(59, 130, 246, 0.2) !important;
  color: rgba(59, 130, 246, 1) !important;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.3);
}
</style>
